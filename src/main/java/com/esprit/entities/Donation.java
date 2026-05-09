package com.esprit.entities;

import java.time.LocalDate;

public class Donation {
    private int id;
    private String donorName;
    private String donationType;
    private double amount;
    private String paymentMethod;
    private LocalDate donationDate;
    private String status;
    private String notes;
    private Integer treeCount;

    // Symfony-aligned fields
    private Integer userId;
    private String transactionStatus;

    public Donation(int id, String donorName, String donationType, double amount, String paymentMethod,
                    LocalDate donationDate, String status, String notes, Integer treeCount) {
        this.id = id;
        this.donorName = donorName;
        this.donationType = donationType;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.donationDate = donationDate;
        this.status = status;
        this.notes = notes;
        this.treeCount = treeCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonationType() {
        return donationType;
    }

    public void setDonationType(String donationType) {
        this.donationType = donationType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
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

    public Integer getTreeCount() {
        return treeCount;
    }

    public void setTreeCount(Integer treeCount) {
        this.treeCount = treeCount;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
}