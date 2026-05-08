package org.example.services;

import org.example.integrations.tinyurl.TinyUrlClient;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ExternalShareLinkService {
    private final TinyUrlClient tinyUrlClient = new TinyUrlClient();

    public ShareLinkResult buildFacebookShareLink(String urlToShare) throws IOException {
        String shortUrl = tinyUrlClient.shorten(urlToShare);
        String facebookShareUrl = "https://www.facebook.com/sharer/sharer.php?u=" + URLEncoder.encode(shortUrl, StandardCharsets.UTF_8);
        return new ShareLinkResult(shortUrl, facebookShareUrl);
    }

    public record ShareLinkResult(String shortUrl, String facebookShareUrl) {}
}
