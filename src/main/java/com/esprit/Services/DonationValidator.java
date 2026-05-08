package com.esprit.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.esprit.entities.Donation;

public final class DonationValidator {
    private static final String TYPE_CASH = "Cash";
    private static final String TYPE_GOODS = "Goods";
    private static final String TYPE_CORPORATE = "Corporate";
    private static final String TYPE_EVENT_SUPPORT = "Event Support";
    private static final String TYPE_TREE_SPONSORSHIP = "Tree Sponsorship";

    private static final List<String> DONATION_TYPES = List.of(
        TYPE_CASH,
        TYPE_GOODS,
        TYPE_CORPORATE,
        TYPE_EVENT_SUPPORT,
        TYPE_TREE_SPONSORSHIP
    );

    private static final List<String> PAYMENT_METHODS = List.of(
        "Card",
        "Bank Transfer",
        "Mobile Wallet",
        "Cash"
    );

    private static final List<String> DELIVERY_METHODS = List.of(
        "Drop-off",
        "Partner Pickup",
        "Collection Point"
    );

    private static final List<String> STATUSES = List.of(
        "Pending",
        "Confirmed",
        "Allocated",
        "Cancelled"
    );

    private DonationValidator() {
    }

    public static void validateAndNormalizeForInsert(Donation donation) {
        validateAndNormalize(donation, false);
    }

    public static void validateAndNormalizeForUpdate(Donation donation) {
        validateAndNormalize(donation, true);
    }

    private static void validateAndNormalize(Donation donation, boolean requireId) {
        if (donation == null) {
            throw new IllegalArgumentException("Donation payload is required.");
        }

        if (requireId && donation.getId() <= 0) {
            throw new IllegalArgumentException("Donation ID is invalid.");
        }

        donation.setDonorName(requireText(donation.getDonorName(), "Donor name", 120));
        donation.setDonationType(requireChoice(donation.getDonationType(), DONATION_TYPES, "Donation type"));
        donation.setStatus(requireChoice(donation.getStatus(), STATUSES, "Status"));

        if (donation.getDonationDate() == null) {
            throw new IllegalArgumentException("Donation date is required.");
        }

        if (donation.getDonationDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Donation date cannot be in the future.");
        }

        donation.setNotes(normalizeOptionalText(donation.getNotes(), "Notes", 2000));

        if (TYPE_GOODS.equals(donation.getDonationType())) {
            donation.setPaymentMethod(requireChoice(donation.getPaymentMethod(), DELIVERY_METHODS, "Delivery method"));
            donation.setAmount(0.0);
            donation.setTreeCount(null);
            return;
        }

        donation.setPaymentMethod(requireChoice(donation.getPaymentMethod(), PAYMENT_METHODS, "Payment method"));

        if (TYPE_TREE_SPONSORSHIP.equals(donation.getDonationType())) {
            Integer treeCount = donation.getTreeCount();
            if (treeCount == null || treeCount <= 0) {
                throw new IllegalArgumentException("Trees count must be greater than zero.");
            }

            if (treeCount > 1_000_000) {
                throw new IllegalArgumentException("Trees count is too large.");
            }

            donation.setAmount(roundTo3(treeCount.doubleValue()));
            return;
        }

        donation.setTreeCount(null);

        double amount = donation.getAmount();
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        if (amount > 999_999_999d) {
            throw new IllegalArgumentException("Amount is too large.");
        }

        donation.setAmount(roundTo3(amount));
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long (max " + maxLength + " chars).");
        }

        return normalized;
    }

    private static String normalizeOptionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " is too long (max " + maxLength + " chars).");
        }

        return normalized;
    }

    private static String requireChoice(String value, List<String> allowedValues, String fieldName) {
        String normalized = requireText(value, fieldName, 120);
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(normalized)) {
                return allowedValue;
            }
        }

        throw new IllegalArgumentException(fieldName + " is invalid.");
    }

    private static double roundTo3(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
