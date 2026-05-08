package org.example.utils;

/**
 * Valeurs affichées côté UI (ex. auteur statique pour les réponses tant qu’il n’y a pas de table user).
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** Nom affiché dans la colonne « From » des réponses (modifiable). */
    public static final String FORUM_USER_DISPLAY_NAME = "Yosra";
    public static final int NOTIFICATION_API_PORT = 8086;
    public static final String REST_API_SECRET_KEY = "mon_secret_pro_123";
}
