package com.esprit.utils;

/**
 * Session sans authentification : utilisateur logique #1 + identité saisie à l'inscription (prénom / nom).
 */
public final class AppSession {

    public static final int DEFAULT_USER_ID = 1;

    private static String registrantFirstName = "";
    private static String registrantLastName = "";

    private AppSession() {
    }

    public static int getCurrentUserId() {
        return DEFAULT_USER_ID;
    }

    public static void setRegistrant(String firstName, String lastName) {
        registrantFirstName = firstName == null ? "" : firstName.trim();
        registrantLastName = lastName == null ? "" : lastName.trim();
    }

    public static String getRegistrantFirstName() {
        return registrantFirstName;
    }

    public static String getRegistrantLastName() {
        return registrantLastName;
    }

    public static boolean hasRegistrant() {
        return !registrantFirstName.isEmpty() && !registrantLastName.isEmpty();
    }

    public static void clearRegistrant() {
        registrantFirstName = "";
        registrantLastName = "";
    }
}
