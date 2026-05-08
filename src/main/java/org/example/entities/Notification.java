package org.example.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Notification {

    public enum NotificationType {
        LIKE("👍"),
        DISLIKE("👎"),
        REPLY("💬"),
        NEW_TOPIC("📢");

        public final String emoji;

        NotificationType(String emoji) {
            this.emoji = emoji;
        }
    }

    private int id;
    private String message;
    private NotificationType type;
    private Date createdAt;
    private boolean isRead;
    private int topicId;  // ← AJOUTER CETTE LIGNE

    // Constructeur pour les notifications sociales (avec type ET topicId)
    public Notification(NotificationType type, String username, String action, String targetTitle, int topicId) {
        this.type = type;
        this.topicId = topicId;
        this.createdAt = new Date();
        this.isRead = false;
        this.message = username + " " + action + " \"" + targetTitle + "\"";
    }

    // Constructeur simple (pour compatibilité)
    public Notification(String message) {
        this.message = message;
        this.type = NotificationType.NEW_TOPIC;
        this.createdAt = new Date();
        this.isRead = false;
        this.topicId = -1;
    }

    // Getters
    public String getMessage() { return message; }
    public int getId() { return id; }
    public NotificationType getType() { return type; }
    public Date getCreatedAt() { return createdAt; }
    public boolean isRead() { return isRead; }
    public int getTopicId() { return topicId; }  // ← AJOUTER CE GETTER

    public void setId(int id) { this.id = id; }
    public void setRead(boolean read) { isRead = read; }
    public void setMessage(String message) { this.message = message; }

    public String getFormattedTime() {
        Date now = new Date();
        long diff = now.getTime() - createdAt.getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return "Il y a " + minutes + " min";
        if (hours < 24) return "Il y a " + hours + " h";
        if (days < 7) return "Il y a " + days + " j";
        return new SimpleDateFormat("dd MMM, HH:mm").format(createdAt);
    }

    @Override
    public String toString() {
        return type.emoji + " " + message + " · " + getFormattedTime();
    }
}