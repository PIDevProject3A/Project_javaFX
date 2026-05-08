package org.example.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ModerationApiClient {
    private ModerationApiClient() {
    }

    public static List<String> checkBadWords(String text) throws IOException {
        List<String> blocked = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocked;
        }

        // Utilisation de PurgoMalum API (100% Gratuit, Sans Clé API)
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        String endpoint = "https://www.purgomalum.com/service/containsprofanity?text=" + encodedText;
        
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);

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
            throw new IOException("Moderation API error (" + status + "): " + response);
        }

        // L'API renvoie "true" si le texte contient des insultes, "false" sinon.
        if ("true".equalsIgnoreCase(response.trim())) {
            blocked.add("Contenu toxique ou mots interdits détectés par l'IA de modération externe.");
        }
        
        return blocked;
    }
}
