package services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import entities.EcoTransaction;

public final class TransactionValidator {
    private static final String TYPE_INCOME = "Income";
    private static final String TYPE_ALLOCATION = "Allocation";
    private static final String TYPE_EXPENSE = "Expense";
    private static final String TYPE_REFUND = "Refund";

    private static final String IMPACT_NONE = "None";
    private static final String IMPACT_TREES = "Trees";
    private static final String IMPACT_WASTE = "Kg Waste";
    private static final String IMPACT_VOLUNTEERS = "Volunteers";

    private static final String PURPOSE_TREE_PLANTING = "Tree Planting";
    private static final String PURPOSE_CLEANUP = "Cleanup Campaign";

    private static final List<String> TRANSACTION_TYPES = List.of(
        TYPE_INCOME,
        TYPE_ALLOCATION,
        TYPE_EXPENSE,
        TYPE_REFUND
    );

    private static final List<String> SOURCE_TYPES = List.of(
        "Donation Pool",
        "Recycling Sales",
        "Corporate Sponsor",
        "Grant",
        "Event Revenue",
        "Partner Contribution"
    );

    private static final List<String> PURPOSES = List.of(
        PURPOSE_TREE_PLANTING,
        PURPOSE_CLEANUP,
        "Community Event",
        "Equipment Purchase",
        "Awareness Program",
        "Emergency Fund",
        "General Operations"
    );

    private static final List<String> IMPACT_UNITS = List.of(
        IMPACT_NONE,
        IMPACT_TREES,
        IMPACT_WASTE,
        IMPACT_VOLUNTEERS
    );

    private static final List<String> STATUSES = List.of(
        "Draft",
        "Approved",
        "Completed",
        "Cancelled"
    );

    private TransactionValidator() {
    }

    public static void validateAndNormalizeForInsert(EcoTransaction transaction) {
        validateAndNormalize(transaction, false);
    }

    public static void validateAndNormalizeForUpdate(EcoTransaction transaction) {
        validateAndNormalize(transaction, true);
    }

    private static void validateAndNormalize(EcoTransaction transaction, boolean requireId) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction payload is required.");
        }

        if (requireId && transaction.getId() <= 0) {
            throw new IllegalArgumentException("Transaction ID is invalid.");
        }

        transaction.setReferenceCode(normalizeReference(transaction.getReferenceCode()));
        transaction.setTransactionType(requireChoice(transaction.getTransactionType(), TRANSACTION_TYPES, "Transaction type"));
        transaction.setSourceType(requireChoice(transaction.getSourceType(), SOURCE_TYPES, "Source type"));
        transaction.setPurpose(requireChoice(transaction.getPurpose(), PURPOSES, "Purpose"));
        transaction.setImpactUnit(requireChoice(transaction.getImpactUnit(), IMPACT_UNITS, "Impact unit"));
        transaction.setStatus(requireChoice(transaction.getStatus(), STATUSES, "Status"));

        if (transaction.getTransactionDate() == null) {
            throw new IllegalArgumentException("Transaction date is required.");
        }

        if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future.");
        }

        transaction.setNotes(normalizeOptionalText(transaction.getNotes(), "Notes", 2000));

        double amount = transaction.getAmount();
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        if (amount > 999_999_999d) {
            throw new IllegalArgumentException("Amount is too large.");
        }

        transaction.setAmount(roundTo3(amount));

        validateImpactRules(transaction);
    }

    private static void validateImpactRules(EcoTransaction transaction) {
        String type = transaction.getTransactionType();
        String purpose = transaction.getPurpose();
        String impactUnit = transaction.getImpactUnit();
        Integer impactQuantity = transaction.getImpactQuantity();

        boolean requiresImpact = TYPE_ALLOCATION.equals(type) || TYPE_EXPENSE.equals(type);
        if (requiresImpact && IMPACT_NONE.equals(impactUnit)) {
            throw new IllegalArgumentException("Allocation and Expense transactions require an impact unit.");
        }

        if ((TYPE_INCOME.equals(type) || TYPE_REFUND.equals(type)) && !IMPACT_NONE.equals(impactUnit)) {
            throw new IllegalArgumentException("Income and Refund transactions must use impact unit None.");
        }

        if (PURPOSE_TREE_PLANTING.equals(purpose) && !IMPACT_TREES.equals(impactUnit) && !IMPACT_NONE.equals(impactUnit)) {
            throw new IllegalArgumentException("Tree Planting purpose requires impact unit Trees.");
        }

        if (PURPOSE_CLEANUP.equals(purpose) && !IMPACT_WASTE.equals(impactUnit) && !IMPACT_NONE.equals(impactUnit)) {
            throw new IllegalArgumentException("Cleanup Campaign purpose requires impact unit Kg Waste.");
        }

        if (IMPACT_NONE.equals(impactUnit)) {
            transaction.setImpactQuantity(null);
            return;
        }

        if (impactQuantity == null || impactQuantity <= 0) {
            throw new IllegalArgumentException("Impact quantity must be greater than zero.");
        }

        if (impactQuantity > 10_000_000) {
            throw new IllegalArgumentException("Impact quantity is too large.");
        }
    }

    private static String normalizeReference(String value) {
        String normalized = requireText(value, "Reference code", 40).toUpperCase();
        if (!normalized.matches("[A-Z0-9-]+")) {
            throw new IllegalArgumentException("Reference code allows only A-Z, 0-9 and -.");
        }
        return normalized;
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
