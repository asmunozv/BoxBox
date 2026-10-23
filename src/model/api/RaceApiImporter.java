package model.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import model.dao.CircuitDAO;
import model.dao.RaceDAO;
import model.entity.Circuit;
import model.entity.Race;

public class RaceApiImporter {

    private static final String BASE_URL_PREFIX = "https://api.jolpi.ca/ergast/f1/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final RaceDAO raceDAO;
    private final CircuitDAO circuitDAO;

    public RaceApiImporter(RaceDAO raceDAO, CircuitDAO circuitDAO) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.raceDAO = raceDAO;
        this.circuitDAO = circuitDAO;
    }

    public ImportSummary importSeasonRaces(int season) {
        RaceApiResponse response = fetchSeason(season);
        if (response == null || response.mrData == null || response.mrData.raceTable == null) {
            return new ImportSummary(0, 0, 0);
        }

        List<RaceDto> items = response.mrData.raceTable.races;
        if (items == null || items.isEmpty()) {
            return new ImportSummary(0, 0, 0);
        }

        int inserted = 0;
        int skippedExisting = 0;
        int skippedMissingCircuit = 0;

        for (RaceDto item : items) {
            String ergastCircuitId = item.circuit != null ? item.circuit.circuitId : null;
            if (ergastCircuitId == null || ergastCircuitId.isBlank()) {
                skippedMissingCircuit++;
                continue;
            }

            Optional<Circuit> circuit = circuitDAO.findByErgastCircuitId(ergastCircuitId);
            if (circuit.isEmpty()) {
                skippedMissingCircuit++;
                continue;
            }

            Race race = mapToEntity(item, circuit.get().getIdCircuit());
            int id = raceDAO.createIfNotExists(race);
            if (race.getIdRace() != null && race.getIdRace() == id) {
                inserted++;
            } else {
                skippedExisting++;
            }
        }

        return new ImportSummary(inserted, skippedExisting, skippedMissingCircuit);
    }

    private RaceApiResponse fetchSeason(int season) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL_PREFIX + season + ".json"))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Race API request failed with status " + response.statusCode());
            }
            return gson.fromJson(response.body(), RaceApiResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch race data from Jolpica API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch race data from Jolpica API.", exception);
        }
    }

    private Race mapToEntity(RaceDto dto, int idCircuit) {
        Race race = new Race();
        race.setSeason(parseInteger(dto.season, 0));
        race.setRoundNumber(parseInteger(dto.round, 0));
        race.setName(dto.raceName);
        race.setRaceDate(parseDate(dto.date));
        race.setIdCircuit(idCircuit);
        return race;
    }

    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    public static class ImportSummary {
        private final int insertedCount;
        private final int skippedExistingCount;
        private final int skippedMissingCircuitCount;

        public ImportSummary(int insertedCount, int skippedExistingCount, int skippedMissingCircuitCount) {
            this.insertedCount = insertedCount;
            this.skippedExistingCount = skippedExistingCount;
            this.skippedMissingCircuitCount = skippedMissingCircuitCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getSkippedExistingCount() {
            return skippedExistingCount;
        }

        public int getSkippedMissingCircuitCount() {
            return skippedMissingCircuitCount;
        }
    }

    private static class RaceApiResponse {
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

        @SerializedName("raceName")
        private String raceName;

        @SerializedName("date")
        private String date;

        @SerializedName("Circuit")
        private CircuitRefDto circuit;
    }

    private static class CircuitRefDto {
        @SerializedName("circuitId")
        private String circuitId;
    }
}
