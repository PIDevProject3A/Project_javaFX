package com.esprit.entities;

import java.time.LocalDateTime;

public class AppUser {
    public enum UserType {
        Collector,
        Buyer,
        Donator
    }

    private final int id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String passwordHash;
    private final UserType userType;
    private final LocalDateTime createdAt;
    private final String createdByAdmin;

    public AppUser(int id, String firstName, String lastName, String email,
                   String passwordHash, UserType userType, LocalDateTime createdAt, String createdByAdmin) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.createdAt = createdAt;
        this.createdByAdmin = createdByAdmin;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserType getUserType() {
        return userType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedByAdmin() {
        return createdByAdmin;
    }

    @Override
    public String toString() {
        return email + " (" + userType + ")";
    }
}
