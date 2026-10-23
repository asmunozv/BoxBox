package model.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import model.dao.ConstructorDAO;
import model.dao.DriverDAO;
import model.dao.RaceDAO;
import model.dao.ResultDAO;
import model.entity.Constructor;
import model.entity.Driver;
import model.entity.Race;
import model.entity.Result;

public class ResultApiImporter {

    private static final String BASE_URL_PREFIX = "https://api.jolpi.ca/ergast/f1/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final ResultDAO resultDAO;
    private final RaceDAO raceDAO;
    private final DriverDAO driverDAO;
    private final ConstructorDAO constructorDAO;

    public ResultApiImporter(ResultDAO resultDAO, RaceDAO raceDAO, DriverDAO driverDAO, ConstructorDAO constructorDAO) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.resultDAO = resultDAO;
        this.raceDAO = raceDAO;
        this.driverDAO = driverDAO;
        this.constructorDAO = constructorDAO;
    }

    public ImportSummary importSeasonResults(int season) {
        ResultApiResponse response = fetchSeasonResults(season);
        if (response == null || response.mrData == null || response.mrData.raceTable == null) {
            return new ImportSummary(0, 0, 0);
        }

        List<RaceDto> races = response.mrData.raceTable.races;
        if (races == null || races.isEmpty()) {
            return new ImportSummary(0, 0, 0);
        }

        int inserted = 0;
        int skippedExisting = 0;
        int skippedMissingReference = 0;

        for (RaceDto raceDto : races) {
            int raceSeason = parseInteger(raceDto.season, 0);
            int round = parseInteger(raceDto.round, 0);

            Optional<Race> race = raceDAO.findBySeasonAndRound(raceSeason, round);
            if (race.isEmpty()) {
                skippedMissingReference += countResults(raceDto.results);
                continue;
            }

            for (ResultDto resultDto : raceDto.results) {
                String driverErgastId = resultDto.driver != null ? resultDto.driver.driverId : null;
                String constructorErgastId = resultDto.constructor != null ? resultDto.constructor.constructorId : null;

                if (isBlank(driverErgastId) || isBlank(constructorErgastId)) {
                    skippedMissingReference++;
                    continue;
                }

                Optional<Driver> driver = driverDAO.findByErgastDriverId(driverErgastId);
                Optional<Constructor> constructor = constructorDAO.findByErgastConstructorId(constructorErgastId);
                if (driver.isEmpty() || constructor.isEmpty()) {
                    skippedMissingReference++;
                    continue;
                }

                Result result = new Result();
                result.setIdRace(race.get().getIdRace());
                result.setIdDriver(driver.get().getIdDriver());
                result.setIdConstructor(constructor.get().getIdConstructor());
                result.setSessionType("RACE");
                Integer parsedPosition = parsePosition(resultDto.position);
                if (parsedPosition == null) {
                    skippedMissingReference++;
                    continue;
                }
                result.setPosition(parsedPosition);
                result.setPoints(parseBigDecimal(resultDto.points));

                int id = resultDAO.createIfNotExists(result);
                if (result.getIdResult() != null && result.getIdResult() == id) {
                    inserted++;
                } else {
                    skippedExisting++;
                }
            }
        }

        return new ImportSummary(inserted, skippedExisting, skippedMissingReference);
    }

    private ResultApiResponse fetchSeasonResults(int season) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL_PREFIX + season + "/results.json"))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Result API request failed with status " + response.statusCode());
            }
            return gson.fromJson(response.body(), ResultApiResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch result data from Jolpica API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch result data from Jolpica API.", exception);
        }
    }

    private int parseInteger(String value, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private Integer parsePosition(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1 || parsed > 20) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private int countResults(List<ResultDto> results) {
        return results == null ? 0 : results.size();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class ImportSummary {
        private final int insertedCount;
        private final int skippedExistingCount;
        private final int skippedMissingReferenceCount;

        public ImportSummary(int insertedCount, int skippedExistingCount, int skippedMissingReferenceCount) {
            this.insertedCount = insertedCount;
            this.skippedExistingCount = skippedExistingCount;
            this.skippedMissingReferenceCount = skippedMissingReferenceCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getSkippedExistingCount() {
            return skippedExistingCount;
        }

        public int getSkippedMissingReferenceCount() {
            return skippedMissingReferenceCount;
        }
    }

    private static class ResultApiResponse {
        @SerializedName("MRData")
        private MrData mrData;
    }

    private static class MrData {
        @SerializedName("RaceTable")
        private RaceTable raceTable;
    }

    private static class RaceTable {
        @SerializedName("Races")
        private List<RaceDto> races = new ArrayList<>();
    }

    private static class RaceDto {
        @SerializedName("season")
        private String season;

        @SerializedName("round")
        private String round;

        @SerializedName("Results")
        private List<ResultDto> results = new ArrayList<>();
    }

    private static class ResultDto {
        @SerializedName("position")
        private String position;

        @SerializedName("points")
        private String points;

        @SerializedName("Driver")
        private DriverRefDto driver;

        @SerializedName("Constructor")
        private ConstructorRefDto constructor;
    }

    private static class DriverRefDto {
        @SerializedName("driverId")
        private String driverId;
    }

    private static class ConstructorRefDto {
        @SerializedName("constructorId")
        private String constructorId;
    }
}
