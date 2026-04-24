package com.bledna.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;

/**
 * AI Service implementation using Roboflow API for specialized waste detection.
 */
public class RoboflowAIService implements AIService {

    // You can get your API key from Roboflow dashboard
    private static final String API_KEY = "u6qj8ChNPWi8sTQh6qbs"; 
    private static final String MODEL_ID = "garbage-classification-3";
    private static final String VERSION = "2";
    private static final String API_URL = "https://detect.roboflow.com/" + MODEL_ID + "/" + VERSION + "?api_key=" + API_KEY;

    @Override
    public String analyzeWaste(File imageFile) {
        if (API_KEY.equals("YOUR_ROBOFLOW_API_KEY")) {
            return "Error: Please provide a valid Roboflow API Key in RoboflowAIService.java";
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(base64Image))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseRoboflowResponse(response.body());
            } else {
                return "Error: Roboflow API returned " + response.statusCode() + " - " + response.body();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String parseRoboflowResponse(String jsonResponse) {
        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray predictions = root.getAsJsonArray("predictions");

            if (predictions.size() == 0) {
                return "No waste detected in the image.";
            }

            // Get the prediction with highest confidence
            JsonObject best = predictions.get(0).getAsJsonObject();
            for (int i = 1; i < predictions.size(); i++) {
                if (predictions.get(i).getAsJsonObject().get("confidence").getAsDouble() > 
                    best.get("confidence").getAsDouble()) {
                    best = predictions.get(i).getAsJsonObject();
                }
            }

            String className = best.get("class").getAsString();
            double confidence = best.get("confidence").getAsDouble() * 100;

            return className; // Return the class name (e.g., "plastic", "glass")
        } catch (Exception e) {
            return "Error parsing Roboflow response: " + e.getMessage();
        }
    }
}
