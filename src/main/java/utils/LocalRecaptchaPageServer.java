package utils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class LocalRecaptchaPageServer {
    private static final String PATH = "/recaptcha";

    private HttpServer server;
    private int port;

    public synchronized String start(String siteKey) throws IOException {
        if (server != null) {
            return buildUrl();
        }

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext(PATH, exchange -> {
            byte[] html = buildHtml(siteKey).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(html);
            }
        });
        server.start();
        return buildUrl();
    }

    private String buildUrl() {
        return "http://localhost:" + port + PATH;
    }

    private String buildHtml(String siteKey) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'>"
                + "<script src='https://www.google.com/recaptcha/api.js' async defer></script>"
                + "<style>body{margin:0;background:#fcfffb;font-family:Arial,sans-serif;}"
                + ".wrap{padding:8px 0 0 6px;}</style></head>"
                + "<body><div class='wrap'>"
                + "<div class='g-recaptcha' data-sitekey='" + siteKey + "' data-callback='onSolved' data-expired-callback='onExpired'></div>"
                + "</div><script>"
                + "function onSolved(token){if(window.captchaBridge){window.captchaBridge.onToken(token);}}"
                + "function onExpired(){if(window.captchaBridge){window.captchaBridge.onToken('');}}"
                + "</script></body></html>";
    }
}

