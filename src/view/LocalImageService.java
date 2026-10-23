package view;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalImageService {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private static final Pattern TITLE_PATTERN = Pattern.compile("\\\"title\\\":\\\"([^\\\"]+)\\\"");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("\\\"source\\\":\\\"([^\\\"]+)\\\"");

    private LocalImageService() {
    }

    public static String resolveDriverImage(String ergastId, String fullName) {
        return resolveWithCache("drivers", List.of(safeName(ergastId), safeName(fullName)), fullName + " Formula One driver", fullName);
    }

    public static String resolveConstructorImage(String ergastId, String name) {
        return resolveWithCache("constructors", List.of(safeName(ergastId), safeName(name)), name + " Formula One constructor", name);
    }

    public static String resolveCircuitImage(String ergastId, String name) {
        return resolveWithCache("circuits", List.of(safeName(ergastId), safeName(name)), name + " Formula One circuit", name);
    }

    public static String resolveRaceImage(String raceName, int season, int round) {
        String seasonRound = safeName(season + "_" + round);
        String raceSeasonRound = safeName(raceName + "_" + season + "_" + round);
        return resolveWithCache("races", List.of(seasonRound, raceSeasonRound, safeName(raceName)), raceName + " Formula One Grand Prix", raceName);
    }

    private static String resolveWithCache(String category, List<String> keys, String searchQuery, String fallbackName) {
        Optional<Path> existing = findExisting(category, keys);
        if (existing.isPresent()) {
            return existing.get().toUri().toString();
        }

        Optional<String> imageUrl = findWikipediaImageUrl(searchQuery);
        if (imageUrl.isPresent()) {
            String saveKey = keys.isEmpty() ? "image" : keys.get(0);
            Optional<Path> saved = downloadAndSave(category, saveKey, imageUrl.get());
            if (saved.isPresent()) {
                return saved.get().toUri().toString();
            }
        }

        return "https://ui-avatars.com/api/?background=cc0000&color=ffffff&size=256&name="
            + URLEncoder.encode(fallbackName, StandardCharsets.UTF_8);
    }

    private static Optional<Path> findExisting(String category, List<String> keys) {
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            for (Path baseDir : baseDirectories()) {
                Path dir = baseDir.resolve(category);
                for (String ext : List.of(".jpg", ".jpeg", ".png", ".webp")) {
                    Path file = dir.resolve(key + ext);
                    if (Files.exists(file)) {
                        return Optional.of(file);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findWikipediaImageUrl(String query) {
        try {
            String search = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=1&srsearch=" + search;
            String searchBody = httpGet(searchUrl);
            if (searchBody == null) {
                return Optional.empty();
            }

            Matcher titleMatcher = TITLE_PATTERN.matcher(searchBody);
            if (!titleMatcher.find()) {
                return Optional.empty();
            }

            String title = titleMatcher.group(1);
            String summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/"
                + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
            String summaryBody = httpGet(summaryUrl);
            if (summaryBody == null) {
                return Optional.empty();
            }

            Matcher sourceMatcher = SOURCE_PATTERN.matcher(summaryBody);
            if (!sourceMatcher.find()) {
                return Optional.empty();
            }

            String source = sourceMatcher.group(1).replace("\\/", "/");
            if (source.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(source);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private static Optional<Path> downloadAndSave(String category, String key, String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(imageUrl)).GET().build();
            HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            String ext = extensionFromUrl(imageUrl);
            Path dir = baseDirectories().get(0).resolve(category);
            Files.createDirectories(dir);
            Path file = dir.resolve(key + ext);
            Files.write(file, response.body());
            return Optional.of(file);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return null;
        }
        return response.body();
    }

    private static List<Path> baseDirectories() {
        return List.of(
            Paths.get("assets", "images"),
            Paths.get("src", "assets", "images")
        );
    }

    private static String extensionFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".jpeg")) {
            return ".jpeg";
        }
        return ".jpg";
    }

    private static String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]+", "_");
    }
}
