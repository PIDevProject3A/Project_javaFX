package com.esprit.entities;

public class RecyclingBuyer {
    private int id;
    private String buyerName;
    private String recyclingType;
    private String address;
    private String city;
    private String gpsLocation; // Format: "lat,lng"
    private String contactPhone;
    private String status;
    private String notes;

    // Symfony-aligned fields
    private Integer userId;
    private String buyerType;
    private String conditions;
    private String contactEmail;
    private String website;

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

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getGpsLocation() {
        return gpsLocation;
    }

    public void setGpsLocation(String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public double getLatitude() {
        if (gpsLocation == null || !gpsLocation.contains(",")) return 0;
        try { return Double.parseDouble(gpsLocation.split(",")[0]); } catch (Exception e) { return 0; }
    }

    public double getLongitude() {
        if (gpsLocation == null || !gpsLocation.contains(",")) return 0;
        try { return Double.parseDouble(gpsLocation.split(",")[1]); } catch (Exception e) { return 0; }
    }

    public void setCoordinates(double lat, double lng) {
        this.gpsLocation = String.format(java.util.Locale.US, "%.6f,%.6f", lat, lng);
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getBuyerType() {
        return buyerType;
    }

    public void setBuyerType(String buyerType) {
        this.buyerType = buyerType;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
