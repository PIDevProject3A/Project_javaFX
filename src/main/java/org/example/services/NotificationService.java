package org.example.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entities.Notification;
import org.example.entities.Notification.NotificationType;
import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.utils.AppConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NotificationService {
    private static final NotificationService INSTANCE = new NotificationService();
    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private final AtomicInteger idSequence = new AtomicInteger(1);
    private final ForumServices forumServices = new ForumServices();
    private final ReponseServices reponseServices = new ReponseServices();
    private final FacebookShareService facebookShareService = new FacebookShareService();
    private final ExternalShareLinkService externalShareLinkService = new ExternalShareLinkService();
    private final AiTopicInsightsService aiTopicInsightsService = new AiTopicInsightsService();
    private final List<String> badWords = List.of(
            "con", "connard", "connasse", "putain", "merde", "salope", "encule",
            "fuck", "shit", "bitch", "asshole", "bastard"
    );
    private HttpServer apiServer;
    private int apiPort = AppConstants.NOTIFICATION_API_PORT;
    private volatile Integer pinnedTopicId;

    private NotificationService() {
    }

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    public ObservableList<Notification> getNotifications() {
        return notifications;
    }

    public Notification publishTopicCreated(String username, String topicTitle, int topicId) {
        Notification notif = new Notification(NotificationType.NEW_TOPIC, username, "a créé un nouveau topic", topicTitle, topicId);
        return addNotification(notif);
    }

    public Notification publishReaction(boolean like, String username, String topicTitle, int topicId) {
        NotificationType type = like ? NotificationType.LIKE : NotificationType.DISLIKE;
        String action = like ? "a aimé" : "n'a pas aimé";
        Notification notif = new Notification(type, username, action, topicTitle, topicId);
        return addNotification(notif);
    }

    public Notification publishReply(String username, String topicTitle, int topicId) {
        Notification notif = new Notification(NotificationType.REPLY, username, "a répondu à", topicTitle, topicId);
        return addNotification(notif);
    }

    public Notification addNotification(Notification notification) {
        notification.setId(idSequence.getAndIncrement());
        if (Platform.isFxApplicationThread()) {
            notifications.add(0, notification);
        } else {
            Platform.runLater(() -> notifications.add(0, notification));
        }
        return notification;
    }

    public int unreadCount() {
        int count = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    public void markAllAsRead() {
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
    }

    public synchronized void clearAll() {
        notifications.clear();
    }

    public synchronized void startRestApi() throws IOException {
        if (apiServer != null) {
            return;
        }
        IOException lastError = null;
        for (int candidatePort = AppConstants.NOTIFICATION_API_PORT; candidatePort < AppConstants.NOTIFICATION_API_PORT + 10; candidatePort++) {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(candidatePort), 0);
                server.createContext("/api/notifications", new NotificationsHandler());
                server.createContext("/api/notifications/unread-count", new UnreadCountHandler());
                server.createContext("/api/notifications/mark-all-read", new MarkAllReadHandler());
                server.createContext("/api/topics/like", new TopicReactionHandler(true));
                server.createContext("/api/topics/dislike", new TopicReactionHandler(false));
                server.createContext("/api/replies/like", new ReplyReactionHandler(true));
                server.createContext("/api/replies/dislike", new ReplyReactionHandler(false));
                server.createContext("/api/topics/pin-most-liked", new PinMostLikedTopicHandler());
                server.createContext("/api/topics/pinned", new PinnedTopicHandler());
                server.createContext("/api/moderation/check", new ModerationCheckHandler());
                server.createContext("/api/topics/", new TopicsRouterHandler());
                server.createContext("/api/replies/", new RepliesByIdRestHandler());
                server.createContext("/api/share/facebook", new FacebookShareHandler());
                server.createContext("/api/share/facebook-link", new FacebookShareLinkHandler());
                server.setExecutor(Executors.newCachedThreadPool());
                server.start();
                apiServer = server;
                apiPort = candidatePort;
                System.out.println("Notification REST API started on http://localhost:" + apiPort + "/api/notifications");
                return;
            } catch (BindException bindException) {
                lastError = bindException;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
    }

    public synchronized void stopRestApi() {
        if (apiServer != null) {
            apiServer.stop(0);
            apiServer = null;
        }
    }

    public int getApiPort() {
        return apiPort;
    }
    //post et get apinotification
    private final class NotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 200, toJsonArray(notifications));
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readBody(exchange.getRequestBody());
                Notification created = parseAndCreateNotification(body);
                sendJson(exchange, 201, toJsonObject(created));
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }
    //nbr des des notif nn read

    private final class UnreadCountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            sendJson(exchange, 200, "{\"count\":" + unreadCount() + "}");
        }
    }
    //marker tout read

    private final class MarkAllReadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            markAllAsRead();
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        }
    }
    //api like and dislike

    private final class TopicReactionHandler implements HttpHandler {
        private final boolean like;

        private TopicReactionHandler(boolean like) {
            this.like = like;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(exchange.getRequestBody());
            int topicId = extractInt(body, "topicId", -1);
            if (topicId <= 0) {
                sendJson(exchange, 400, "{\"error\":\"topicId is required\"}");
                return;
            }
            String username = extractString(body, "username", AppConstants.FORUM_USER_DISPLAY_NAME);
            try {
                if (like) {
                    forumServices.likeTopic(topicId);
                } else {
                    forumServices.dislikeTopic(topicId);
                }
                Topic topic = forumServices.getTopicById(topicId);
                String topicTitle = topic != null ? topic.getTitle() : "topic";
                publishReaction(like, username, topicTitle, topicId);
                refreshPinnedTopic();
                String response = "{"
                        + "\"status\":\"ok\","
                        + "\"topicId\":" + topicId + ","
                        + "\"reaction\":\"" + (like ? "LIKE" : "DISLIKE") + "\""
                        + "}";
                sendJson(exchange, 200, response);
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private final class ReplyReactionHandler implements HttpHandler {
        private final boolean like;

        private ReplyReactionHandler(boolean like) {
            this.like = like;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(exchange.getRequestBody());
            int replyId = extractInt(body, "replyId", -1);
            if (replyId <= 0) {
                sendJson(exchange, 400, "{\"error\":\"replyId is required\"}");
                return;
            }
            try {
                if (!reponseServices.supportsReactions()) {
                    sendJson(exchange, 409, "{\"error\":\"Reply reaction columns are missing in database\"}");
                    return;
                }
                if (like) {
                    reponseServices.likeReponse(replyId);
                } else {
                    reponseServices.dislikeReponse(replyId);
                }
                sendJson(exchange, 200, "{\"status\":\"ok\",\"replyId\":" + replyId + "}");
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }
    //epingle le topic plus aimer

    private final class PinMostLikedTopicHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                Topic pinned = refreshPinnedTopic();
                if (pinned == null) {
                    sendJson(exchange, 404, "{\"error\":\"No topics found to pin\"}");
                    return;
                }
                sendJson(exchange, 200, toPinnedTopicJson(pinned, true));
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private final class PinnedTopicHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                Topic pinned = getPinnedTopicOrFallback();
                if (pinned == null) {
                    sendJson(exchange, 404, "{\"error\":\"No pinned topic available\"}");
                    return;
                }
                sendJson(exchange, 200, toPinnedTopicJson(pinned, false));
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private final class ModerationCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(exchange.getRequestBody());
            String text = extractString(body, "text", "");
            List<String> blocked = findBlockedWords(text);
            boolean allowed = blocked.isEmpty();
            String blockedCsv = String.join(",", blocked);
            String response = "{"
                    + "\"allowed\":" + allowed + ","
                    + "\"blockedCsv\":\"" + escapeJson(blockedCsv) + "\""
                    + "}";
            sendJson(exchange, 200, response);
        }
    }

    private final class TopicsRouterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || !path.startsWith("/api/topics/")) {
                sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
                return;
            }

            if (path.endsWith("/ai/summary")) {
                handleAiSummary(exchange, path);
                return;
            }

            if (path.endsWith("/replies")) {
                handleTopicReplies(exchange, path);
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
        }
    }

    private void handleAiSummary(HttpExchange exchange, String path) throws IOException {
        Integer topicId = extractResourceId(path, "/api/topics/", "/ai/summary");
        if (topicId == null || topicId <= 0) {
            sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Topic topic = forumServices.getTopicById(topicId);
            if (topic == null) {
                sendJson(exchange, 404, "{\"error\":\"Topic not found\"}");
                return;
            }
            AiTopicInsightsService.AiResult result = aiTopicInsightsService.summarizeAndTag(topic);
            sendJson(exchange, 200, "{"
                    + "\"topicId\":" + topicId + ","
                    + "\"summary\":\"" + escapeJson(result.summary()) + "\","
                    + "\"tags\":{"
                    + "\"labels\":" + result.tagsLabelsJson() + ","
                    + "\"scores\":" + result.tagsScoresJson()
                    + "}"
                    + "}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (SQLException ex) {
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IOException ex) {
            sendJson(exchange, 502, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    private void handleTopicReplies(HttpExchange exchange, String path) throws IOException {
        Integer topicId = extractResourceId(path, "/api/topics/", "/replies");
        if (topicId == null || topicId <= 0) {
            sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
            return;
        }

        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                List<Reponse> replies = reponseServices.afficherParTopic(topicId);
                sendJson(exchange, 200, toRepliesJsonArray(replies));
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(exchange.getRequestBody());
                String content = extractString(body, "content", "").trim();
                String username = extractString(body, "username", AppConstants.FORUM_USER_DISPLAY_NAME);
                if (content.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"content is required\"}");
                    return;
                }

                Topic topic = forumServices.getTopicById(topicId);
                if (topic == null) {
                    sendJson(exchange, 404, "{\"error\":\"Topic not found\"}");
                    return;
                }

                Reponse reponse = new Reponse();
                reponse.setContent(content);
                reponse.setTopic_id(topicId);
                reponse.setCreated_at(new java.util.Date());
                reponse.setUpdated_at(new java.util.Date());
                int createdId = reponseServices.ajouter(reponse);
                Reponse created = reponseServices.getById(createdId);
                publishReply(username, topic.getTitle(), topicId);
                sendJson(exchange, 201, toReplyJsonObject(created != null ? created : reponse));
                return;
            }

            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        } catch (SQLException ex) {
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    private final class RepliesByIdRestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            Integer replyId = extractResourceId(path, "/api/replies/", null);
            if (replyId == null || replyId <= 0) {
                sendJson(exchange, 404, "{\"error\":\"Route not found\"}");
                return;
            }

            String method = exchange.getRequestMethod();
            try {
                Reponse existing = reponseServices.getById(replyId);
                if (existing == null) {
                    sendJson(exchange, 404, "{\"error\":\"Reply not found\"}");
                    return;
                }

                if ("GET".equalsIgnoreCase(method)) {
                    sendJson(exchange, 200, toReplyJsonObject(existing));
                    return;
                }

                if ("PUT".equalsIgnoreCase(method)) {
                    String body = readBody(exchange.getRequestBody());
                    String content = extractString(body, "content", "").trim();
                    if (content.isEmpty()) {
                        sendJson(exchange, 400, "{\"error\":\"content is required\"}");
                        return;
                    }
                    existing.setContent(content);
                    existing.setUpdated_at(new java.util.Date());
                    reponseServices.modifier(existing);
                    Reponse updated = reponseServices.getById(replyId);
                    sendJson(exchange, 200, toReplyJsonObject(updated != null ? updated : existing));
                    return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                    reponseServices.supprimer(replyId);
                    sendJson(exchange, 200, "{\"status\":\"deleted\",\"replyId\":" + replyId + "}");
                    return;
                }

                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            } catch (SQLException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private final class FacebookShareHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange.getRequestBody());
            String message = extractString(body, "message", "").trim();
            try {
                String externalPostId = facebookShareService.share(message);
                sendJson(exchange, 200, "{"
                        + "\"status\":\"ok\","
                        + "\"provider\":\"facebook\","
                        + "\"externalPostId\":\"" + escapeJson(externalPostId) + "\""
                        + "}");
            } catch (IllegalArgumentException ex) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            } catch (IllegalStateException ex) {
                sendJson(exchange, 500, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            } catch (IOException ex) {
                sendJson(exchange, 502, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Plan B: external REST API without OAuth.
     * Calls TinyURL to shorten a link, then returns a Facebook share URL.
     *
     * POST /api/share/facebook-link
     * Body: {"url":"https://example.com/topic/12"}
     */
    private final class FacebookShareLinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readBody(exchange.getRequestBody());
            String url = extractString(body, "url", "").trim();
            try {
                ExternalShareLinkService.ShareLinkResult result = externalShareLinkService.buildFacebookShareLink(url);
                sendJson(exchange, 200, "{"
                        + "\"status\":\"ok\","
                        + "\"provider\":\"tinyurl\","
                        + "\"shortUrl\":\"" + escapeJson(result.shortUrl()) + "\","
                        + "\"facebookShareUrl\":\"" + escapeJson(result.facebookShareUrl()) + "\""
                        + "}");
            } catch (IllegalArgumentException ex) {
                sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            } catch (IOException ex) {
                sendJson(exchange, 502, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
        }
    }

    private Topic refreshPinnedTopic() throws SQLException {
        Topic top = forumServices.getMostLikedTopic();
        pinnedTopicId = top != null ? top.getId() : null;
        return top;
    }

    private Topic getPinnedTopicOrFallback() throws SQLException {
        if (pinnedTopicId != null) {
            Topic pinned = forumServices.getTopicById(pinnedTopicId);
            if (pinned != null) {
                return pinned;
            }
        }
        return refreshPinnedTopic();
    }

    private Notification parseAndCreateNotification(String body) {
        String typeRaw = extractString(body, "type", "NEW_TOPIC");
        NotificationType type;
        try {
            type = NotificationType.valueOf(typeRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            type = NotificationType.NEW_TOPIC;
        }
        String username = extractString(body, "username", AppConstants.FORUM_USER_DISPLAY_NAME);
        String action = extractString(body, "action", "a fait une action");
        String targetTitle = extractString(body, "targetTitle", "topic");
        int topicId = extractInt(body, "topicId", -1);
        Notification notification = new Notification(type, username, action, targetTitle, topicId);
        return addNotification(notification);
    }

    private static String extractString(String json, String key, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1) : defaultValue;
    }

    private static int extractInt(String json, String key, int defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String readBody(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJsonArray(List<Notification> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJsonObject(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toRepliesJsonArray(List<Reponse> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toReplyJsonObject(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toReplyJsonObject(Reponse r) {
        if (r == null) {
            return "{}";
        }
        long createdAt = r.getCreated_at() != null ? r.getCreated_at().getTime() : 0L;
        long updatedAt = r.getUpdated_at() != null ? r.getUpdated_at().getTime() : 0L;
        return "{"
                + "\"id\":" + r.getId() + ","
                + "\"content\":\"" + escapeJson(r.getContent()) + "\","
                + "\"topicId\":" + r.getTopic_id() + ","
                + "\"createdAt\":" + createdAt + ","
                + "\"updatedAt\":" + updatedAt + ","
                + "\"likeCount\":" + r.getLikeCount() + ","
                + "\"dislikeCount\":" + r.getDislikeCount()
                + "}";
    }

    private static String toJsonObject(Notification n) {
        return "{"
                + "\"id\":" + n.getId() + ","
                + "\"message\":\"" + escapeJson(n.getMessage()) + "\","
                + "\"type\":\"" + (n.getType() != null ? n.getType().name() : "NEW_TOPIC") + "\","
                + "\"createdAt\":" + n.getCreatedAt().getTime() + ","
                + "\"read\":" + n.isRead() + ","
                + "\"topicId\":" + n.getTopicId()
                + "}";
    }

    private static String toPinnedTopicJson(Topic t, boolean repinnedNow) {
        return "{"
                + "\"topicId\":" + t.getId() + ","
                + "\"title\":\"" + escapeJson(t.getTitle()) + "\","
                + "\"likes\":" + t.getLikeCount() + ","
                + "\"dislikes\":" + t.getDislikeCount() + ","
                + "\"repinnedNow\":" + repinnedNow
                + "}";
    }

    private List<String> findBlockedWords(String text) {
        String normalized = normalizeText(text);
        Set<String> detected = new LinkedHashSet<>();
        for (String badWord : badWords) {
            String normalizedWord = normalizeWord(badWord);
            if (!normalizedWord.isEmpty() && containsWord(normalized, normalizedWord)) {
                detected.add(badWord);
            }
        }
        return new ArrayList<>(detected);
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
    }

    private static String normalizeWord(String word) {
        if (word == null) {
            return "";
        }
        return word.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static boolean containsWord(String normalizedText, String normalizedWord) {
        Pattern p = Pattern.compile("(^|\\s)" + Pattern.quote(normalizedWord) + "(\\s|$)");
        return p.matcher(normalizedText).find();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Integer extractResourceId(String fullPath, String prefix, String suffix) {
        if (fullPath == null || !fullPath.startsWith(prefix)) {
            return null;
        }
        String remaining = fullPath.substring(prefix.length());
        if (suffix != null) {
            if (!remaining.endsWith(suffix)) {
                return null;
            }
            remaining = remaining.substring(0, remaining.length() - suffix.length());
        }
        if (remaining.contains("/")) {
            return null;
        }
        try {
            return Integer.parseInt(remaining);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
