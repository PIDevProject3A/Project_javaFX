package org.example.integrations.translation;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationService {

    /**
     * Translate text using MyMemory Translation API (Free, no key required).
     * @param text The text to translate.
     * @param targetLang The target language code (e.g. "fr", "en").
     * @return The translated text.
     */
    public static String translate(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            // Using "Autodetect" for source language
            String langPair = URLEncoder.encode("Autodetect|" + targetLang, StandardCharsets.UTF_8.toString());
            String endpoint = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=" + langPair;

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                try (InputStream is = connection.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return extractTranslatedText(json);
                }
            } else {
                System.err.println("Translation API failed with status: " + status);
            }
        } catch (Exception e) {
            System.err.println("Error calling Translation API: " + e.getMessage());
        }
        return text; // Return original text on failure
    }

    private static String extractTranslatedText(String json) {
        // Look for: "translatedText":"<text>"
        Pattern pattern = Pattern.compile("\"translatedText\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            // Unescape json unicode and quotes if needed (simplified)
            String match = matcher.group(1);
            return match.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
        }
        return null;
    }
}
