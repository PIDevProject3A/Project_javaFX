package com.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Envoi email gratuit via API Brevo (ex Sendinblue).
 * Plan gratuit suffisant pour un projet étudiant.
 */
public class ReceiptMailService {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";
    private final HttpClient http = HttpClient.newHttpClient();
    private final Properties config = loadConfig();

    public void sendReceiptEmail(String toEmail, String eventName, String receiptCode, byte[] pdfBytes) throws Exception {
        String apiKey = read("brevo.api.key");
        String fromEmail = read("brevo.sender.email");
        String fromName = read("brevo.sender.name");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Brevo API key manquante (brevo.api.key).");
        }
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Email expéditeur manquant (brevo.sender.email).");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Email destinataire vide.");
        }

        String attachmentB64 = Base64.getEncoder().encodeToString(pdfBytes);
        String safeEvent = escapeJson(eventName != null ? eventName : "Événement");
        String safeCode = escapeJson(receiptCode != null ? receiptCode : "—");
        String safeTo = escapeJson(toEmail);
        String safeFrom = escapeJson(fromEmail);
        String safeFromName = escapeJson(fromName != null && !fromName.isBlank() ? fromName : "Bledna");

        String payload = "{"
                + "\"sender\":{\"name\":\"" + safeFromName + "\",\"email\":\"" + safeFrom + "\"},"
                + "\"to\":[{\"email\":\"" + safeTo + "\"}],"
                + "\"subject\":\"Votre reçu de paiement - Bledna\","
                + "\"htmlContent\":\"<p>Bonjour,</p><p>Veuillez trouver en pièce jointe votre reçu de paiement pour <b>"
                + safeEvent + "</b>.</p><p>Code reçu : <b>" + safeCode + "</b></p><p>Merci.</p>\","
                + "\"attachment\":[{\"name\":\"recu_" + safeCode + ".pdf\",\"content\":\"" + attachmentB64 + "\"}]"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("Brevo API error " + resp.statusCode() + " : " + resp.body());
        }
    }

    private String read(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();
        v = config.getProperty(key);
        if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (var in = ReceiptMailService.class.getClassLoader().getResourceAsStream("stripe.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {
        }
        return p;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}


