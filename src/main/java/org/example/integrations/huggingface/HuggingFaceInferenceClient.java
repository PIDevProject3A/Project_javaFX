package org.example.integrations.huggingface;

import org.example.utils.EnvConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal HuggingFace Inference API client (no extra dependencies).
 * Docs: https://huggingface.co/docs/api-inference/
 */
public class HuggingFaceInferenceClient {
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;

    public String summarize(String model, String text) throws IOException {
        String endpoint = modelEndpoint(model);
        String payload = "{"
                + "\"inputs\":\"" + escapeJson(text) + "\","
                + "\"parameters\":{"
                + "\"max_length\":130,"
                + "\"min_length\":30"
                + "}"
                + "}";
        String response = postJson(endpoint, payload);
        // typical response: [{"summary_text":"..."}]
        String summary = extractJsonString(response, "summary_text");
        if (summary == null || summary.isBlank()) {
            // some models return generated_text
            summary = extractJsonString(response, "generated_text");
        }
        if (summary == null || summary.isBlank()) {
            throw new IOException("HuggingFace response missing summary_text.");
        }
        return summary;
    }

    public TagResult classifyTags(String model, String text, String[] candidateLabels) throws IOException {
        String endpoint = modelEndpoint(model);
        String labelsJson = toJsonArray(candidateLabels);
        String payload = "{"
                + "\"inputs\":\"" + escapeJson(text) + "\","
                + "\"parameters\":{"
                + "\"candidate_labels\":" + labelsJson
                + "}"
                + "}";
        String response = postJson(endpoint, payload);
        // typical response: {"sequence":"...","labels":["a","b"],"scores":[0.9,0.1]}
        String labelsRaw = extractJsonArray(response, "labels");
        String scoresRaw = extractJsonArray(response, "scores");
        return new TagResult(labelsRaw, scoresRaw, response);
    }

    private static String modelEndpoint(String model) {
        String base = EnvConfig.getOptional("HF_API_BASE_URL", "https://api-inference.huggingface.co/models");
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String m = (model == null || model.isBlank()) ? "facebook/bart-large-cnn" : model.trim();
        return trimmed + "/" + m;
    }

    private static String postJson(String endpoint, String payload) throws IOException {
        String token = EnvConfig.getRequired("HF_API_TOKEN");
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + token);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();

        if (status < 200 || status >= 300) {
            String msg = extractJsonString(response, "error");
            if (msg == null || msg.isBlank()) {
                msg = response;
            }
            throw new IOException("HuggingFace API error (" + status + "): " + msg);
        }
        // If model is loading, HF sometimes returns 200 with {"error":"Model ... is currently loading","estimated_time":...}
        String err = extractJsonString(response, "error");
        if (err != null && !err.isBlank()) {
            throw new IOException("HuggingFace API: " + err);
        }
        return response;
    }

    private static String readAll(InputStream stream) throws IOException {
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

    private static String extractJsonArray(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\[[^\\]]*\\])");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : "[]";
    }

    private static String toJsonArray(String[] values) {
        if (values == null || values.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(values[i] == null ? "" : values[i])).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    public record TagResult(String labelsJsonArray, String scoresJsonArray, String rawJson) {}
}

