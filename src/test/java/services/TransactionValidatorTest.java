package services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import entities.EcoTransaction;

class TransactionValidatorTest {

    @Test
    void validateInsert_incomeNormalizesFieldsAndDisablesImpact() {
        EcoTransaction transaction = new EcoTransaction(
            0,
            "  inc-20260412-1  ",
            "income",
            "donation pool",
            "general operations",
            1234.56789,
            "none",
            100,
            LocalDate.now(),
            "approved",
            "  april cash in  "
        );

        TransactionValidator.validateAndNormalizeForInsert(transaction);

        assertEquals("INC-20260412-1", transaction.getReferenceCode());
        assertEquals("Income", transaction.getTransactionType());
        assertEquals("Donation Pool", transaction.getSourceType());
        assertEquals("General Operations", transaction.getPurpose());
        assertEquals(1234.568, transaction.getAmount());
        assertEquals("None", transaction.getImpactUnit());
        assertNull(transaction.getImpactQuantity());
        assertEquals("Approved", transaction.getStatus());
        assertEquals("april cash in", transaction.getNotes());
    }

    @Test
    void validateInsert_allocationRequiresImpact() {
        EcoTransaction transaction = validTransaction();
        transaction.setTransactionType("Allocation");
        transaction.setImpactUnit("None");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> TransactionValidator.validateAndNormalizeForInsert(transaction)
        );

        assertEquals("Allocation and Expense transactions require an impact unit.", ex.getMessage());
    }

    @Test
    void validateInsert_treePlantingRequiresTreesUnit() {
        EcoTransaction transaction = validTransaction();
        transaction.setPurpose("Tree Planting");
        transaction.setImpactUnit("Volunteers");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> TransactionValidator.validateAndNormalizeForInsert(transaction)
        );

        assertEquals("Tree Planting purpose requires impact unit Trees.", ex.getMessage());
    }

    @Test
    void validateInsert_rejectsInvalidReferencePattern() {
        EcoTransaction transaction = validTransaction();
        transaction.setReferenceCode("ref/2026");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> TransactionValidator.validateAndNormalizeForInsert(transaction)
        );

        assertEquals("Reference code allows only A-Z, 0-9 and -.", ex.getMessage());
    }

    @Test
    void validateUpdate_requiresPositiveId() {
        EcoTransaction transaction = validTransaction();
        transaction.setId(0);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> TransactionValidator.validateAndNormalizeForUpdate(transaction)
        );

        assertEquals("Transaction ID is invalid.", ex.getMessage());
    }

    private EcoTransaction validTransaction() {
        return new EcoTransaction(
            1,
            "EXP-20260412-2",
            "Expense",
            "Donation Pool",
            "Cleanup Campaign",
            300.0,
            "Kg Waste",
            500,
            LocalDate.now(),
            "Completed",
            "cleanup logistics"
        );
    }
}
