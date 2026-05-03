package org.example.integrations.facebook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FacebookGraphApiClient {
    private static final int CONNECT_TIMEOUT_MS = 7000;
    private static final int READ_TIMEOUT_MS = 12000;

    public String publishToPage(String graphBaseUrl, String pageId, String pageAccessToken, String message) throws IOException {
        String endpoint = sanitizeBaseUrl(graphBaseUrl) + "/" + urlEncode(pageId) + "/feed";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");

        String payload = "message=" + urlEncode(message) + "&access_token=" + urlEncode(pageAccessToken);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        String response = readResponse(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();

        if (status < 200 || status >= 300) {
            String fbError = extractErrorMessage(response);
            throw new IOException("Facebook Graph API error (" + status + "): " + fbError);
        }

        String postId = extractJsonString(response, "id");
        if (postId == null || postId.isBlank()) {
            throw new IOException("Facebook Graph API returned no post id.");
        }
        return postId;
    }

    private static String sanitizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://graph.facebook.com/v20.0";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String readResponse(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream is = stream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractErrorMessage(String json) {
        String message = extractJsonString(json, "message");
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

    private static String urlEncode(String value) {
        String safe = value == null ? "" : value;
        return URLEncoder.encode(safe, StandardCharsets.UTF_8);
    }
}
