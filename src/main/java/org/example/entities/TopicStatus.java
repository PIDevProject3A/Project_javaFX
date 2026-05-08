package org.example.entities;

import java.util.Locale;

/**
 * Topic status stored in the database as a lowercase string ({@code pending}, {@code accepted}, {@code refused}).
 */
public enum TopicStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REFUSED("refused");

    private final String dbValue;

    TopicStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    /** Plain English label for lists and forms. */
    public String getDisplayLabel() {
        return switch (this) {
            case PENDING -> "Waiting review";
            case ACCEPTED -> "Approved";
            case REFUSED -> "Declined";
        };
    }

    /** Resolves the combo / filter label shown in the UI. */
    public static TopicStatus fromDisplayLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        for (TopicStatus ts : values()) {
            if (ts.getDisplayLabel().equalsIgnoreCase(s)) {
                return ts;
            }
        }
        return null;
    }

    /**
     * Reads a SQL or legacy value; maps older {@code open} / {@code closed} values.
     */
    public static TopicStatus fromDb(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        for (TopicStatus ts : values()) {
            if (ts.dbValue.equals(s)) {
                return ts;
            }
        }
        if ("open".equals(s) || "opennn".equals(s)) {
            return PENDING;
        }
        if ("closed".equals(s) || "close".equals(s)) {
            return REFUSED;
        }
        return PENDING;
    }
}
