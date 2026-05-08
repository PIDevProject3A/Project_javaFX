package org.example.integrations.tinyurl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * TinyURL public API (no auth) to shorten links.
 * Endpoint: https://tinyurl.com/api-create.php?url=...
 */
public class TinyUrlClient {
    private static final int CONNECT_TIMEOUT_MS = 7000;
    private static final int READ_TIMEOUT_MS = 12000;

    public String shorten(String longUrl) throws IOException {
        String url = longUrl == null ? "" : longUrl.trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }
        String endpoint = "https://tinyurl.com/api-create.php?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException("TinyURL API error (" + status + "): " + response);
        }
        String tiny = response == null ? "" : response.trim();
        if (!tiny.startsWith("http")) {
            throw new IOException("TinyURL API returned unexpected response: " + tiny);
        }
        return tiny;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream is = stream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
