package org.example.entities;

public enum TopicCategory {
    RECLAMATION("reclamation", "Reclamation"),
    FEEDBACK("feedback", "Feedback"),
    EVENT_PROPOSAL("event_proposal", "Event proposal");

    private final String dbValue;
    private final String displayLabel;

    TopicCategory(String dbValue, String displayLabel) {
        this.dbValue = dbValue;
        this.displayLabel = displayLabel;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public static TopicCategory fromDb(String value) {
        if (value == null || value.isBlank()) {
            return FEEDBACK;
        }
        for (TopicCategory c : values()) {
            if (c.dbValue.equalsIgnoreCase(value.trim())) {
                return c;
            }
        }
        return FEEDBACK;
    }
}
