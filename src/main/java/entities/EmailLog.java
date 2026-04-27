package entities;

import java.time.LocalDateTime;

public class EmailLog {
    private final int id;
    private final String sender;
    private final String recipient;
    private final String subject;
    private final String status;
    private final LocalDateTime sentAt;

    public EmailLog(int id, String sender, String recipient, String subject, String status, LocalDateTime sentAt) {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.status = status;
        this.sentAt = sentAt;
    }

    public int getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
