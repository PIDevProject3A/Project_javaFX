package com.esprit.utils;

import com.esprit.entities.AppUser;
import com.esprit.entities.User;

public final class UserSession {
    private static String currentUserEmail;
    private static User.AdminType currentUserRole;
    private static User userToEdit;
    private static AppUser appUserToEdit;
    private static int currentLoginLogId = -1;
    private static boolean isAppUser = false;
    private static AppUser.UserType currentAppUserType;

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

    public static AppUser getAppUserToEdit() {
        return appUserToEdit;
    }

    public static void setAppUserToEdit(AppUser user) {
        appUserToEdit = user;
    }

    public static int getCurrentLoginLogId() {
        return currentLoginLogId;
    }

    public static void setCurrentLoginLogId(int id) {
        currentLoginLogId = id;
    }

    public static boolean isAppUser() {
        return isAppUser;
    }

    public static void setIsAppUser(boolean value) {
        isAppUser = value;
    }

    public static AppUser.UserType getCurrentAppUserType() {
        return currentAppUserType;
    }

    public static void setCurrentAppUserType(AppUser.UserType type) {
        currentAppUserType = type;
    }

    public static void clear() {
        currentUserEmail = null;
        currentUserRole = null;
        userToEdit = null;
        appUserToEdit = null;
        currentLoginLogId = -1;
        isAppUser = false;
        currentAppUserType = null;
    }
}

