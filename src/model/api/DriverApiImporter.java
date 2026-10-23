package model.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import model.dao.DriverDAO;
import model.entity.Driver;

public class DriverApiImporter {

    private static final String BASE_URL_PREFIX = "https://api.jolpi.ca/ergast/f1/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final DriverDAO driverDAO;

    public DriverApiImporter(DriverDAO driverDAO) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.driverDAO = driverDAO;
    }

    public ImportSummary importAllDrivers(int season) {
        int offset = 0;
        int total = Integer.MAX_VALUE;
        int inserted = 0;
        int skipped = 0;

        while (offset < total) {
            DriverApiResponse response = fetchDriversPage(season, offset, 100);
            if (response == null || response.mrData == null || response.mrData.driverTable == null) {
                break;
            }

            total = parseInteger(response.mrData.total, 0);
            int limit = parseInteger(response.mrData.limit, 100);

            List<DriverDto> items = response.mrData.driverTable.drivers;
            if (items == null || items.isEmpty()) {
                break;
            }

            for (DriverDto item : items) {
                if ("jak_crawford".equalsIgnoreCase(item.driverId)) {
                    skipped++;
                    continue;
                }
                Driver driver = mapToEntity(item);

                if (driver.getCode() != null && !driver.getCode().isBlank()) {
                    driverDAO.findByCode(driver.getCode()).ifPresent(existingByCode -> {
                        if (!existingByCode.getErgastDriverId().equals(driver.getErgastDriverId())) {
                            driver.setCode(null);
                        }
                    });
                }

                if (driverDAO.findByErgastDriverId(driver.getErgastDriverId()).isPresent()) {
                    skipped++;
                } else {
                    driverDAO.create(driver);
                    inserted++;
                }
            }

            offset += limit;
        }

        return new ImportSummary(inserted, skipped);
    }

    private DriverApiResponse fetchDriversPage(int season, int offset, int limit) {
        String url = BASE_URL_PREFIX + season + "/drivers.json"
            + "?limit=" + encode(String.valueOf(limit))
            + "&offset=" + encode(String.valueOf(offset));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Driver API request failed with status " + response.statusCode());
            }
            return gson.fromJson(response.body(), DriverApiResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch driver data from Jolpica API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch driver data from Jolpica API.", exception);
        }
    }

    private Driver mapToEntity(DriverDto dto) {
        Driver driver = new Driver();
        driver.setErgastDriverId(dto.driverId);
        driver.setPermanentNumber(parseInteger(dto.permanentNumber, null));
        driver.setCode(dto.code);
        driver.setGivenName(dto.givenName);
        driver.setFamilyName(dto.familyName);
        driver.setDateOfBirth(parseDate(dto.dateOfBirth));
        driver.setNationality(dto.nationality);
        return driver;
    }

    private Integer parseInteger(String value, Integer defaultValue) {
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

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class ImportSummary {
        private final int insertedCount;
        private final int skippedCount;

        public ImportSummary(int insertedCount, int skippedCount) {
            this.insertedCount = insertedCount;
            this.skippedCount = skippedCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }
    }

    private static class DriverApiResponse {
        @SerializedName("MRData")
        private MrData mrData;
    }

    private static class MrData {
        @SerializedName("total")
        private String total;

        @SerializedName("limit")
        private String limit;

        @SerializedName("DriverTable")
        private DriverTable driverTable;
    }

    private static class DriverTable {
        @SerializedName("Drivers")
        private List<DriverDto> drivers = new ArrayList<>();
    }

    private static class DriverDto {
        @SerializedName("driverId")
        private String driverId;

        @SerializedName("permanentNumber")
        private String permanentNumber;

        @SerializedName("code")
        private String code;

        @SerializedName("givenName")
        private String givenName;

        @SerializedName("familyName")
        private String familyName;

        @SerializedName("dateOfBirth")
        private String dateOfBirth;

        @SerializedName("nationality")
        private String nationality;
    }
}
