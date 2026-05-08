package com.esprit.services;

import com.esprit.utils.CompreFaceConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompreFaceFaceIdService {
    private static final Pattern SIMILARITY_PATTERN = Pattern.compile("\"similarity\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"subject\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;

    public CompreFaceFaceIdService() {
        this(HttpClient.newHttpClient());
    }

    CompreFaceFaceIdService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return CompreFaceConfig.isConfigured();
    }

    public VerificationResult verifyByEmailAndImage(String email, Path imagePath) {
        if (!isConfigured()) {
            return VerificationResult.error("CompreFace is not configured.");
        }
        if (isBlank(email)) {
            return VerificationResult.error("Email required for Face ID verification.");
        }
        if (imagePath == null || !Files.exists(imagePath)) {
            return VerificationResult.error("Image not found.");
        }

        try {
            MultipartPayload payload = buildMultipartPayload(email.trim().toLowerCase(), imagePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CompreFaceConfig.baseUrl() + "/api/v1/recognition/verify"))
                    .header("x-api-key", CompreFaceConfig.apiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + payload.boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload.body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return VerificationResult.error("CompreFace responded with HTTP " + response.statusCode() + ".");
            }

            return parseVerifyResponse(email.trim().toLowerCase(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return VerificationResult.error("Unable to contact CompreFace.");
        } catch (IOException exception) {
            return VerificationResult.error("Unable to contact CompreFace.");
        }
    }

    public EnrollResult enrollFace(String subject, Path imagePath) {
        if (!isConfigured()) {
            return EnrollResult.error("CompreFace is not configured.");
        }
        if (isBlank(subject)) {
            return EnrollResult.error("Invalid Face ID subject.");
        }
        if (imagePath == null || !Files.exists(imagePath)) {
            return EnrollResult.error("Image not found.");
        }

        try {
            MultipartPayload payload = buildMultipartPayload(subject.trim(), imagePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CompreFaceConfig.baseUrl() + "/api/v1/recognition/faces"))
                    .header("x-api-key", CompreFaceConfig.apiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + payload.boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload.body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return EnrollResult.error("Enrollment failed, HTTP " + response.statusCode() + ".");
            }
            if (response.body() == null || response.body().contains("\"result\":[]")) {
                return EnrollResult.error("No face detected for enrollment.");
            }
            return EnrollResult.successResult();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return EnrollResult.error("Unable to contact CompreFace.");
        } catch (IOException exception) {
            return EnrollResult.error("Unable to contact CompreFace.");
        }
    }

    public RecognitionResult recognizeByImage(Path imagePath) {
        if (!isConfigured()) {
            return RecognitionResult.error("CompreFace is not configured.");
        }
        if (imagePath == null || !Files.exists(imagePath)) {
            return RecognitionResult.error("Image not found.");
        }

        try {
            MultipartPayload payload = buildMultipartPayload("recognize", imagePath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CompreFaceConfig.baseUrl() + "/api/v1/recognition/recognize"))
                    .header("x-api-key", CompreFaceConfig.apiKey())
                    .header("Content-Type", "multipart/form-data; boundary=" + payload.boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload.body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return RecognitionResult.error("Recognition failed, HTTP " + response.statusCode() + ".");
            }
            return parseRecognizeResponse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return RecognitionResult.error("Unable to contact CompreFace.");
        } catch (IOException exception) {
            return RecognitionResult.error("Unable to contact CompreFace.");
        }
    }

    static VerificationResult parseVerifyResponse(String email, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return VerificationResult.error("Empty response from CompreFace.");
        }

        if (responseBody.contains("\"result\":[]")) {
            return VerificationResult.noFace("No face detected in the image.");
        }

        boolean hasSubject = responseBody.contains("\"subject\":\"" + escapeJson(email) + "\"");
        double similarity = findBestSimilarity(responseBody);

        if (hasSubject && similarity >= CompreFaceConfig.similarityThreshold()) {
            return VerificationResult.match(similarity);
        }

        if (responseBody.contains("\"subjects\":[]")) {
            return VerificationResult.noMatch("Face detected but not recognized for this email.");
        }

        if (similarity < 0) {
            return VerificationResult.noMatch("Unable to identify a valid match.");
        }

        return VerificationResult.noMatch("Insufficient facial matching.");
    }

    static double findBestSimilarity(String responseBody) {
        Matcher matcher = SIMILARITY_PATTERN.matcher(responseBody);
        double best = -1.0;
        while (matcher.find()) {
            double similarity = Double.parseDouble(matcher.group(1));
            if (similarity > best) {
                best = similarity;
            }
        }
        return best;
    }

    static RecognitionResult parseRecognizeResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return RecognitionResult.error("Empty response from CompreFace.");
        }
        if (responseBody.contains("\"result\":[]")) {
            return RecognitionResult.noFace("No face detected in the image.");
        }

        Matcher subjectMatcher = SUBJECT_PATTERN.matcher(responseBody);
        if (!subjectMatcher.find()) {
            return RecognitionResult.noMatch("No known face recognized.");
        }

        String subject = subjectMatcher.group(1);
        double similarity = findBestSimilarity(responseBody);
        if (similarity < CompreFaceConfig.similarityThreshold()) {
            return RecognitionResult.noMatch("Face detected but confidence is too low.");
        }
        return RecognitionResult.match(subject, similarity);
    }

    private MultipartPayload buildMultipartPayload(String email, Path imagePath) throws IOException {
        String boundary = "----JavaFaceBoundary" + UUID.randomUUID();
        String fileName = imagePath.getFileName().toString();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeTextPart(output, boundary, "subject", email);
        writeFilePart(output, boundary, "file", fileName, Files.readAllBytes(imagePath));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return new MultipartPayload(boundary, output.toByteArray());
    }

    private void writeTextPart(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(ByteArrayOutputStream output, String boundary, String partName, String fileName, byte[] fileBytes) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + partName + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class MultipartPayload {
        private final String boundary;
        private final byte[] body;

        private MultipartPayload(String boundary, byte[] body) {
            this.boundary = boundary;
            this.body = body;
        }
    }

    public enum VerificationStatus {
        MATCH,
        NO_MATCH,
        NO_FACE,
        ERROR
    }

    public static final class VerificationResult {
        private final VerificationStatus status;
        private final String message;
        private final double similarity;

        private VerificationResult(VerificationStatus status, String message, double similarity) {
            this.status = status;
            this.message = message;
            this.similarity = similarity;
        }

        public static VerificationResult match(double similarity) {
            return new VerificationResult(VerificationStatus.MATCH, "Face recognized.", similarity);
        }

        public static VerificationResult noMatch(String message) {
            return new VerificationResult(VerificationStatus.NO_MATCH, message, -1.0);
        }

        public static VerificationResult noFace(String message) {
            return new VerificationResult(VerificationStatus.NO_FACE, message, -1.0);
        }

        public static VerificationResult error(String message) {
            return new VerificationResult(VerificationStatus.ERROR, message, -1.0);
        }

        public VerificationStatus status() {
            return status;
        }

        public String message() {
            return message;
        }

        public double similarity() {
            return similarity;
        }
    }

    public enum RecognitionStatus {
        MATCH,
        NO_MATCH,
        NO_FACE,
        ERROR
    }

    public static final class RecognitionResult {
        private final RecognitionStatus status;
        private final String message;
        private final String subject;
        private final double similarity;

        private RecognitionResult(RecognitionStatus status, String message, String subject, double similarity) {
            this.status = status;
            this.message = message;
            this.subject = subject;
            this.similarity = similarity;
        }

        public static RecognitionResult match(String subject, double similarity) {
            return new RecognitionResult(RecognitionStatus.MATCH, "Face recognized.", subject, similarity);
        }

        public static RecognitionResult noMatch(String message) {
            return new RecognitionResult(RecognitionStatus.NO_MATCH, message, null, -1.0);
        }

        public static RecognitionResult noFace(String message) {
            return new RecognitionResult(RecognitionStatus.NO_FACE, message, null, -1.0);
        }

        public static RecognitionResult error(String message) {
            return new RecognitionResult(RecognitionStatus.ERROR, message, null, -1.0);
        }

        public RecognitionStatus status() {
            return status;
        }

        public String message() {
            return message;
        }

        public String subject() {
            return subject;
        }

        public double similarity() {
            return similarity;
        }
    }

    public static final class EnrollResult {
        private final boolean success;
        private final String message;

        private EnrollResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static EnrollResult successResult() {
            return new EnrollResult(true, "Face ID successfully enrolled.");
        }

        public static EnrollResult error(String message) {
            return new EnrollResult(false, message);
        }

        public boolean success() {
            return success;
        }

        public String message() {
            return message;
        }
    }
}





