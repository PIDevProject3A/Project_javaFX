package org.example.utils;

import org.example.services.NotificationService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModerationApiClient {
    private ModerationApiClient() {
    }

    public static List<String> checkBadWords(String text) throws IOException {
        int port = NotificationService.getInstance().getApiPort();
        URL url = new URL("http://localhost:" + port + "/api/moderation/check");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        String payload = "{\"text\":\"" + escapeJson(text == null ? "" : text) + "\"}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        String response = "";
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (stream != null) {
            try (InputStream is = stream) {
                response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IOException("Moderation API error: " + response);
        }
        String blockedCsv = extractString(response, "blockedCsv");
        List<String> blocked = new ArrayList<>();
        if (blockedCsv != null && !blockedCsv.isBlank()) {
            for (String s : blockedCsv.split(",")) {
                String w = s.trim();
                if (!w.isEmpty()) {
                    blocked.add(w);
                }
            }
        }
        return blocked;
    }

    private static String extractString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
