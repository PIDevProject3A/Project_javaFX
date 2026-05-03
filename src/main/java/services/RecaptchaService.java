package services;

import utils.RecaptchaConfig;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

public class RecaptchaService {
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Pattern SUCCESS_PATTERN = Pattern.compile("\\\"success\\\"\\s*:\\s*true");
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("\\\"hostname\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public boolean verifyToken(String token) {
        if (token == null || token.isBlank() || !RecaptchaConfig.isConfigured()) {
            return false;
        }

        String payload = "secret=" + encode(RecaptchaConfig.secret())
                + "&response=" + encode(token);

        HttpRequest request = HttpRequest.newBuilder(URI.create(VERIFY_URL))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            String responseBody = response.body();
            if (!SUCCESS_PATTERN.matcher(responseBody).find()) {
                return false;
            }

            String hostname = extractHostname(responseBody);
            return "localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String extractHostname(String responseBody) {
        var matcher = HOSTNAME_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}


