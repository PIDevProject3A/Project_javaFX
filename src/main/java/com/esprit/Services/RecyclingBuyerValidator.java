package com.esprit.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.esprit.entities.RecyclingBuyer;

public final class RecyclingBuyerValidator {
    private static final List<String> RECYCLING_TYPES = List.of(
        "Plastic",
        "Metal",
        "Glass",
        "Paper",
        "Mixed",
        "Organic"
    );

    private static final List<String> STATUSES = List.of(
        "Active",
        "Temporarily Closed",
        "Inactive"
    );

    private RecyclingBuyerValidator() {
    }

    public static void validateAndNormalizeForInsert(RecyclingBuyer buyer) {
        validateAndNormalize(buyer, false);
    }

    public static void validateAndNormalizeForUpdate(RecyclingBuyer buyer) {
        validateAndNormalize(buyer, true);
    }

    private static void validateAndNormalize(RecyclingBuyer buyer, boolean requireId) {
        if (buyer == null) {
            throw new IllegalArgumentException("Buyer payload is required.");
        }

        if (requireId && buyer.getId() <= 0) {
            throw new IllegalArgumentException("Buyer ID is invalid.");
        }

        buyer.setBuyerName(requireText(buyer.getBuyerName(), "Buyer name", 120));
        buyer.setRecyclingType(requireChoice(buyer.getRecyclingType(), RECYCLING_TYPES, "Recycling type"));
        buyer.setAddress(requireText(buyer.getAddress(), "Address", 200));
        buyer.setCity(requireText(buyer.getCity(), "City", 100));
        buyer.setContactPhone(normalizeOptionalText(buyer.getContactPhone(), "Contact phone", 40));
        buyer.setStatus(requireChoice(buyer.getStatus(), STATUSES, "Status"));
        buyer.setNotes(normalizeOptionalText(buyer.getNotes(), "Notes", 2000));

        double latitude = buyer.getLatitude();
        double longitude = buyer.getLongitude();

        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }

        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }

        buyer.setLatitude(roundTo6(latitude));
        buyer.setLongitude(roundTo6(longitude));
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

    private static double roundTo6(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
