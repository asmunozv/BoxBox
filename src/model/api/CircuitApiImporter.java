package model.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import model.dao.CircuitDAO;
import model.entity.Circuit;

public class CircuitApiImporter {

    private static final String BASE_URL_PREFIX = "https://api.jolpi.ca/ergast/f1/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final CircuitDAO circuitDAO;

    public CircuitApiImporter(CircuitDAO circuitDAO) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.circuitDAO = circuitDAO;
    }

    public ImportSummary importAllCircuits(int season) {
        int offset = 0;
        int total = Integer.MAX_VALUE;
        int inserted = 0;
        int skipped = 0;

        while (offset < total) {
            CircuitApiResponse response = fetchCircuitsPage(season, offset, 100);
            if (response == null || response.mrData == null || response.mrData.circuitTable == null) {
                break;
            }

            total = parseInteger(response.mrData.total, 0);
            int limit = parseInteger(response.mrData.limit, 100);
            List<CircuitDto> items = response.mrData.circuitTable.circuits;

            if (items == null || items.isEmpty()) {
                break;
            }

            for (CircuitDto item : items) {
                Circuit circuit = mapToEntity(item);
                if (circuitDAO.findByErgastCircuitId(circuit.getErgastCircuitId()).isPresent()) {
                    skipped++;
                } else {
                    circuitDAO.create(circuit);
                    inserted++;
                }
            }

            offset += limit;
        }

        return new ImportSummary(inserted, skipped);
    }

    private CircuitApiResponse fetchCircuitsPage(int season, int offset, int limit) {
        String url = BASE_URL_PREFIX + season + "/circuits.json"
            + "?limit=" + encode(String.valueOf(limit))
            + "&offset=" + encode(String.valueOf(offset));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Circuit API request failed with status " + response.statusCode());
            }
            return gson.fromJson(response.body(), CircuitApiResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch circuit data from Jolpica API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch circuit data from Jolpica API.", exception);
        }
    }

    private Circuit mapToEntity(CircuitDto dto) {
        Circuit circuit = new Circuit();
        circuit.setErgastCircuitId(dto.circuitId);
        circuit.setName(dto.circuitName);

        if (dto.location != null) {
            circuit.setCountry(dto.location.country);
            circuit.setLocality(dto.location.locality);
            circuit.setLatitude(parseBigDecimal(dto.location.lat));
            circuit.setLongitude(parseBigDecimal(dto.location.longValue));
        }

        return circuit;
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

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
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

    private static class CircuitApiResponse {
        @SerializedName("MRData")
        private MrData mrData;
    }

    private static class MrData {
        @SerializedName("total")
        private String total;

        @SerializedName("limit")
        private String limit;

        @SerializedName("CircuitTable")
        private CircuitTable circuitTable;
    }

    private static class CircuitTable {
        @SerializedName("Circuits")
        private List<CircuitDto> circuits = new ArrayList<>();
    }

    private static class CircuitDto {
        @SerializedName("circuitId")
        private String circuitId;

        @SerializedName("circuitName")
        private String circuitName;

        @SerializedName("Location")
        private LocationDto location;
    }

    private static class LocationDto {
        @SerializedName("lat")
        private String lat;

        @SerializedName("long")
        private String longValue;

        @SerializedName("locality")
        private String locality;

        @SerializedName("country")
        private String country;
    }
}
