package utils;

import entities.User;

public final class UserSession {
    private static String currentUserEmail;
    private static User.AdminType currentUserRole;

    private UserSession() {
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public static void setCurrentUserEmail(String email) {
        currentUserEmail = email == null ? null : email.trim().toLowerCase();
    }

    public static User.AdminType getCurrentUserRole() {
        return currentUserRole;
    }

    public static void setCurrentUserRole(User.AdminType role) {
        currentUserRole = role;
    }

    public static void clear() {
        currentUserEmail = null;
        currentUserRole = null;
    }
}

