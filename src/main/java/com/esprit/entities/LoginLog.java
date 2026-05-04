package com.esprit.entities;

import java.time.LocalDateTime;

public class LoginLog {
    private final int id;
    private final int userId;
    private final String userRole;
    private final LocalDateTime loginTime;
    private final LocalDateTime logoutTime;

    public LoginLog(int id, int userId, String userRole, LocalDateTime loginTime, LocalDateTime logoutTime) {
        this.id = id;
        this.userId = userId;
        this.userRole = userRole;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }
}

