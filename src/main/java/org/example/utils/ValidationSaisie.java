package org.example.utils;

import org.example.entities.TopicStatus;
import org.example.entities.TopicCategory;

import java.sql.Timestamp;

/**
 * Simple input checks for topics and replies (English messages for the user).
 */
public final class ValidationSaisie {

    public static final int TOPIC_TITLE_MIN = 2;
    public static final int TOPIC_TITLE_MAX = 200;
    public static final int TOPIC_CONTENT_MIN = 1;
    public static final int TOPIC_CONTENT_MAX = 10_000;

    public static final int REPONSE_CONTENT_MIN = 1;
    public static final int REPONSE_CONTENT_MAX = 10_000;

    private static final String DATE_PATTERN_HINT = "Use format: yyyy-MM-dd HH:mm:ss";

    private ValidationSaisie() {
    }

    /** @return null if valid, otherwise a short error message */
    public static String validerTitreTopic(String titre) {
        if (titre == null || titre.isBlank()) {
            return "Please enter a title.";
        }
        String t = titre.trim();
        if (t.length() < TOPIC_TITLE_MIN) {
            return "Title must be at least " + TOPIC_TITLE_MIN + " characters.";
        }
        if (t.length() > TOPIC_TITLE_MAX) {
            return "Title must be at most " + TOPIC_TITLE_MAX + " characters.";
        }
        if (isNumericOnly(t)) {
            return "Title must contain letters, not only numbers.";
        }
        return null;
    }

    public static String validerContenuTopic(String contenu) {
        if (contenu == null || contenu.isBlank()) {
            return "Please enter some text for this topic.";
        }
        if (contenu.trim().length() < TOPIC_CONTENT_MIN) {
            return "Text is too short.";
        }
        if (contenu.length() > TOPIC_CONTENT_MAX) {
            return "Text must be at most " + TOPIC_CONTENT_MAX + " characters.";
        }
        if (isNumericOnly(contenu)) {
            return "Text must contain letters, not only numbers.";
        }
        return null;
    }

    public static String validerStatutTopic(TopicStatus statut) {
        if (statut == null) {
            return "Please pick a status.";
        }
        return null;
    }

    public static String validerCategorieTopic(TopicCategory category) {
        if (category == null) {
            return "Please pick a category.";
        }
        return null;
    }

    public static String validerDateHeureTopic(String texte) {
        if (texte == null || texte.isBlank()) {
            return "Created date is required. " + DATE_PATTERN_HINT;
        }
        try {
            Timestamp.valueOf(texte.trim());
            return null;
        } catch (IllegalArgumentException e) {
            return "That date is not valid. " + DATE_PATTERN_HINT;
        }
    }

    public static String validerContenuReponse(String contenu) {
        if (contenu == null || contenu.isBlank()) {
            return "Please type your reply.";
        }
        String t = contenu.trim();
        if (t.length() < REPONSE_CONTENT_MIN) {
            return "Reply is too short.";
        }
        if (contenu.length() > REPONSE_CONTENT_MAX) {
            return "Reply must be at most " + REPONSE_CONTENT_MAX + " characters.";
        }
        if (isNumericOnly(t)) {
            return "Reply must contain letters, not only numbers.";
        }
        return null;
    }

    private static boolean isNumericOnly(String value) {
        if (value == null) {
            return false;
        }
        String compact = value.replaceAll("\\s+", "");
        return !compact.isEmpty() && compact.matches("\\d+");
    }

    public static String validerDateHeureReponse(String texte) {
        if (texte == null || texte.isBlank()) {
            return "Created date is required. " + DATE_PATTERN_HINT;
        }
        try {
            Timestamp.valueOf(texte.trim());
            return null;
        } catch (IllegalArgumentException e) {
            return "That date is not valid. " + DATE_PATTERN_HINT;
        }
    }
}
