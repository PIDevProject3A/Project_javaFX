package com.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

import com.esprit.entities.WeatherData;

public class WeatherService {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private static final Pattern CURRENT_OBJECT_PATTERN = Pattern.compile("\\\"current\\\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern TEMPERATURE_PATTERN = Pattern.compile("\\\"temperature_2m\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern WIND_PATTERN = Pattern.compile("\\\"wind_speed_10m\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
    private static final Pattern RAIN_PATTERN = Pattern.compile("\\\"rain\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

    private final HttpClient httpClient;

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public WeatherData fetchCurrentWeather(double latitude, double longitude) throws IOException {
        String lat = URLEncoder.encode(String.format(Locale.US, "%.6f", latitude), StandardCharsets.UTF_8);
        String lon = URLEncoder.encode(String.format(Locale.US, "%.6f", longitude), StandardCharsets.UTF_8);

        String uri = BASE_URL
            + "?latitude=" + lat
            + "&longitude=" + lon
            + "&current=temperature_2m,wind_speed_10m,rain";

        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(12))
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Weather request interrupted.", ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Weather API returned status " + response.statusCode() + ".");
        }

        return parseWeather(response.body());
    }

    private WeatherData parseWeather(String json) throws IOException {
        Matcher currentMatcher = CURRENT_OBJECT_PATTERN.matcher(json);
        if (!currentMatcher.find()) {
            throw new IOException("Weather API response missing current weather block.");
        }

        String currentObject = currentMatcher.group(1);

        double temperature = extractRequiredDouble(currentObject, TEMPERATURE_PATTERN, "temperature_2m");
        double wind = extractRequiredDouble(currentObject, WIND_PATTERN, "wind_speed_10m");
        double rain = extractRequiredDouble(currentObject, RAIN_PATTERN, "rain");

        return new WeatherData(temperature, wind, rain);
    }

    private double extractRequiredDouble(String source, Pattern pattern, String fieldName) throws IOException {
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new IOException("Weather API response missing field: " + fieldName + ".");
        }

        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ex) {
            throw new IOException("Weather API field " + fieldName + " is not a valid number.", ex);
        }
    }
}
