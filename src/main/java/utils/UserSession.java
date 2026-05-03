package utils;

import entities.User;

public final class UserSession {
    private static String currentUserEmail;
    private static User.AdminType currentUserRole;
    private static User userToEdit;
    private static int currentLoginLogId = -1;

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

    public static User getUserToEdit() {
        return userToEdit;
    }

    public static void setUserToEdit(User user) {
        userToEdit = user;
    }

    public static int getCurrentLoginLogId() {
        return currentLoginLogId;
    }

    public static void setCurrentLoginLogId(int id) {
        currentLoginLogId = id;
    }

    public static void clear() {
        currentUserEmail = null;
        currentUserRole = null;
        userToEdit = null;
        currentLoginLogId = -1;
    }
}

