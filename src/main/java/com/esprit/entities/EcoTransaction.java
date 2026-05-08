package com.esprit.entities;

import java.time.LocalDate;

public class EcoTransaction {
    private int id;
    private String referenceCode;
    private String transactionType;
    private String sourceType;
    private String purpose;
    private double amount;
    private String impactUnit;
    private Integer impactQuantity;
    private LocalDate transactionDate;
    private String status;
    private String notes;

    public EcoTransaction(int id, String referenceCode, String transactionType, String sourceType, String purpose,
                          double amount, String impactUnit, Integer impactQuantity, LocalDate transactionDate,
                          String status, String notes) {
        this.id = id;
        this.referenceCode = referenceCode;
        this.transactionType = transactionType;
        this.sourceType = sourceType;
        this.purpose = purpose;
        this.amount = amount;
        this.impactUnit = impactUnit;
        this.impactQuantity = impactQuantity;
        this.transactionDate = transactionDate;
        this.status = status;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getImpactUnit() {
        return impactUnit;
    }

    public void setImpactUnit(String impactUnit) {
        this.impactUnit = impactUnit;
    }

    public Integer getImpactQuantity() {
        return impactQuantity;
    }

    public void setImpactQuantity(Integer impactQuantity) {
        this.impactQuantity = impactQuantity;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
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
