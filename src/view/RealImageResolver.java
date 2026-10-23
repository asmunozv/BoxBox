package view;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public final class RealImageResolver {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Gson GSON = new Gson();

    private RealImageResolver() {
    }

    public static String resolveImageUrl(String query, String fallbackName) {
        return resolveImageUrl(query, fallbackName, "");
    }

    public static String resolveImageUrl(String query, String fallbackName, String context) {
        try {
            String wikipediaTitle = findBestWikipediaTitle((query + " " + context).trim());
            if (wikipediaTitle == null || wikipediaTitle.isBlank()) {
                wikipediaTitle = query;
            }

            String title = URLEncoder.encode(wikipediaTitle.replace(' ', '_'), StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + title;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                WikiSummary summary = GSON.fromJson(response.body(), WikiSummary.class);
                if (summary != null && summary.thumbnail != null && summary.thumbnail.source != null && !summary.thumbnail.source.isBlank()) {
                    return summary.thumbnail.source;
                }
            }
        } catch (Exception exception) {
            // Fallback below.
        }
        return "https://ui-avatars.com/api/?background=cc0000&color=ffffff&size=256&name="
            + URLEncoder.encode(fallbackName, StandardCharsets.UTF_8);
    }

    private static String findBestWikipediaTitle(String searchText) {
        try {
            String encoded = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
            String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=1&srsearch=" + encoded;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchUrl)).GET().build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                WikiSearchResponse searchResponse = GSON.fromJson(response.body(), WikiSearchResponse.class);
                if (searchResponse != null
                    && searchResponse.query != null
                    && searchResponse.query.search != null
                    && !searchResponse.query.search.isEmpty()) {
                    return searchResponse.query.search.get(0).title;
                }
            }
        } catch (Exception exception) {
            return null;
        }
        return null;
    }

    private static class WikiSummary {
        @SerializedName("thumbnail")
        private Thumbnail thumbnail;
    }

    private static class Thumbnail {
        @SerializedName("source")
        private String source;
    }

    private static class WikiSearchResponse {
        @SerializedName("query")
        private SearchQuery query;
    }

    private static class SearchQuery {
        @SerializedName("search")
        private java.util.List<SearchItem> search;
    }

    private static class SearchItem {
        @SerializedName("title")
        private String title;
    }
}
