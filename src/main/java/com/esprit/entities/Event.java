package com.esprit.entities;

import java.time.LocalDateTime;

public class Event {

    private int id;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private String location;

    private double price;
    private String paymentType; // CARD / CASH
    private String eventType;   // FREE / PAID

    private int maxPlaces;
    private int currentParticipants;

    private String status; // OPEN / CLOSED / CANCELLED

    /** Rempli côté admin : liste « Prénom Nom » des inscrits (non persisté). */
    private String registrantsSummary;

    // 🔹 Constructor vide
    public Event() {}

    // 🔹 Constructor complet
    public Event(int id, String name, String description, LocalDateTime eventDate, String location,
                 double price, String paymentType, String eventType,
                 int maxPlaces, int currentParticipants, String status) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.price = price;
        this.paymentType = paymentType;
        this.eventType = eventType;
        this.maxPlaces = maxPlaces;
        this.currentParticipants = currentParticipants;
        this.status = status;
    }

    // 🔹 Getters / Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public int getMaxPlaces() { return maxPlaces; }
    public void setMaxPlaces(int maxPlaces) { this.maxPlaces = maxPlaces; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRegistrantsSummary() { return registrantsSummary; }
    public void setRegistrantsSummary(String registrantsSummary) { this.registrantsSummary = registrantsSummary; }
}
