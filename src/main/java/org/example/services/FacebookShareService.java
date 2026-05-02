package org.example.services;

import org.example.integrations.facebook.FacebookGraphApiClient;
import org.example.utils.EnvConfig;

import java.io.IOException;

public class FacebookShareService {
    private static final int MAX_MESSAGE_LENGTH = 700;
    private final FacebookGraphApiClient client = new FacebookGraphApiClient();

    public String share(String message) throws IOException {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message is too long (max " + MAX_MESSAGE_LENGTH + " chars)");
        }

        String graphBaseUrl = EnvConfig.getOptional("FB_GRAPH_BASE_URL", "https://graph.facebook.com/v20.0");
        String pageId = EnvConfig.getRequired("FB_PAGE_ID");
        String pageAccessToken = EnvConfig.getRequired("FB_PAGE_ACCESS_TOKEN");

        return client.publishToPage(graphBaseUrl, pageId, pageAccessToken, normalized);
    }
}
