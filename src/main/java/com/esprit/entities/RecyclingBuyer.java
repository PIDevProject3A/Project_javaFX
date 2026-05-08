package com.esprit.entities;

public class RecyclingBuyer {
    private int id;
    private String buyerName;
    private String recyclingType;
    private String address;
    private String city;
    private double latitude;
    private double longitude;
    private String contactPhone;
    private String status;
    private String notes;

    public RecyclingBuyer(int id, String buyerName, String recyclingType, String address, String city,
                          double latitude, double longitude, String contactPhone, String status, String notes) {
        this.id = id;
        this.buyerName = buyerName;
        this.recyclingType = recyclingType;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.contactPhone = contactPhone;
        this.status = status;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getRecyclingType() {
        return recyclingType;
    }

    public void setRecyclingType(String recyclingType) {
        this.recyclingType = recyclingType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
