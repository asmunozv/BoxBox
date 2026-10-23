package model.api;

import java.io.IOException;
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

import model.dao.ConstructorDAO;
import model.entity.Constructor;

public class ConstructorApiImporter {

    private static final String BASE_URL_PREFIX = "https://api.jolpi.ca/ergast/f1/";

    private final HttpClient httpClient;
    private final Gson gson;
    private final ConstructorDAO constructorDAO;

    public ConstructorApiImporter(ConstructorDAO constructorDAO) {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.constructorDAO = constructorDAO;
    }

    public ImportSummary importAllConstructors(int season) {
        int offset = 0;
        int total = Integer.MAX_VALUE;
        int inserted = 0;
        int skipped = 0;

        while (offset < total) {
            ConstructorApiResponse response = fetchConstructorsPage(season, offset, 100);
            if (response == null || response.mrData == null || response.mrData.constructorTable == null) {
                break;
            }

            total = parseInteger(response.mrData.total, 0);
            int limit = parseInteger(response.mrData.limit, 100);
            List<ConstructorDto> items = response.mrData.constructorTable.constructors;

            if (items == null || items.isEmpty()) {
                break;
            }

            for (ConstructorDto item : items) {
                Constructor constructorEntity = mapToEntity(item);
                java.util.Optional<Constructor> existing = constructorDAO.findByErgastConstructorId(constructorEntity.getErgastConstructorId());
                if (existing.isPresent()) {
                    Constructor current = existing.get();
                    current.setName(constructorEntity.getName());
                    current.setCountry(constructorEntity.getCountry());
                    constructorDAO.update(current);
                    skipped++;
                } else {
                    constructorDAO.create(constructorEntity);
                    inserted++;
                }
            }

            offset += limit;
        }

        return new ImportSummary(inserted, skipped);
    }

    private ConstructorApiResponse fetchConstructorsPage(int season, int offset, int limit) {
        String url = BASE_URL_PREFIX + season + "/constructors.json"
            + "?limit=" + encode(String.valueOf(limit))
            + "&offset=" + encode(String.valueOf(offset));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Constructor API request failed with status " + response.statusCode());
            }
            return gson.fromJson(response.body(), ConstructorApiResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch constructor data from Jolpica API.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to fetch constructor data from Jolpica API.", exception);
        }
    }

    private Constructor mapToEntity(ConstructorDto dto) {
        Constructor constructorEntity = new Constructor();
        constructorEntity.setErgastConstructorId(dto.constructorId);
        constructorEntity.setName(normalizeConstructorName(dto.constructorId, dto.name));
        constructorEntity.setCountry(dto.nationality);
        return constructorEntity;
    }

    private String normalizeConstructorName(String constructorId, String apiName) {
        if (constructorId == null) {
            return apiName;
        }
        switch (constructorId) {
            case "rb":
                return "Racing Bulls";
            case "red_bull":
            case "redbull":
                return "Red Bull Racing";
            case "haas":
                return "Haas";
            case "alpine":
                return "Alpine";
            case "cadillac":
                return "Cadillac";
            default:
                return apiName;
        }
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

    private static class ConstructorApiResponse {
        @SerializedName("MRData")
        private MrData mrData;
    }

    private static class MrData {
        @SerializedName("total")
        private String total;

        @SerializedName("limit")
        private String limit;

        @SerializedName("ConstructorTable")
        private ConstructorTable constructorTable;
    }

    private static class ConstructorTable {
        @SerializedName("Constructors")
        private List<ConstructorDto> constructors = new ArrayList<>();
    }

    private static class ConstructorDto {
        @SerializedName("constructorId")
        private String constructorId;

        @SerializedName("name")
        private String name;

        @SerializedName("nationality")
        private String nationality;
    }
}
