package services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import entities.Donation;

class DonationValidatorTest {

    @Test
    void validateInsert_cashDonation_normalizesFieldsAndAmount() {
        Donation donation = new Donation(
            0,
            "  Ahmed Trabelsi  ",
            "cash",
            25.6789,
            "card",
            LocalDate.now(),
            "pending",
            "  sponsor for April campaign  ",
            null
        );

        DonationValidator.validateAndNormalizeForInsert(donation);

        assertEquals("Ahmed Trabelsi", donation.getDonorName());
        assertEquals("Cash", donation.getDonationType());
        assertEquals("Card", donation.getPaymentMethod());
        assertEquals("Pending", donation.getStatus());
        assertEquals(25.679, donation.getAmount());
        assertNull(donation.getTreeCount());
        assertEquals("sponsor for April campaign", donation.getNotes());
    }

    @Test
    void validateInsert_goodsDonation_setsAmountAndTreeCount() {
        Donation donation = new Donation(
            0,
            "Nour",
            "goods",
            999.0,
            "drop-off",
            LocalDate.now(),
            "confirmed",
            "goods package",
            12
        );

        DonationValidator.validateAndNormalizeForInsert(donation);

        assertEquals("Goods", donation.getDonationType());
        assertEquals("Drop-off", donation.getPaymentMethod());
        assertEquals("Confirmed", donation.getStatus());
        assertEquals(0.0, donation.getAmount());
        assertNull(donation.getTreeCount());
    }

    @Test
    void validateInsert_treeSponsorship_setsAmountFromTreeCount() {
        Donation donation = new Donation(
            0,
            "Corporate Partner",
            "tree sponsorship",
            100.0,
            "bank transfer",
            LocalDate.now(),
            "allocated",
            null,
            30
        );

        DonationValidator.validateAndNormalizeForInsert(donation);

        assertEquals("Tree Sponsorship", donation.getDonationType());
        assertEquals("Bank Transfer", donation.getPaymentMethod());
        assertEquals("Allocated", donation.getStatus());
        assertEquals(30.0, donation.getAmount());
        assertEquals(30, donation.getTreeCount());
    }

    @Test
    void validateInsert_rejectsFutureDate() {
        Donation donation = validCashDonation();
        donation.setDonationDate(LocalDate.now().plusDays(1));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForInsert(donation)
        );

        assertEquals("Donation date cannot be in the future.", ex.getMessage());
    }

    @Test
    void validateInsert_rejectsInvalidType() {
        Donation donation = validCashDonation();
        donation.setDonationType("Crypto");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForInsert(donation)
        );

        assertEquals("Donation type is invalid.", ex.getMessage());
    }

    @Test
    void validateInsert_rejectsInvalidPaymentMethodForGoods() {
        Donation donation = validCashDonation();
        donation.setDonationType("Goods");
        donation.setPaymentMethod("Card");

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForInsert(donation)
        );

        assertEquals("Delivery method is invalid.", ex.getMessage());
    }

    @Test
    void validateInsert_rejectsNonPositiveAmountForCash() {
        Donation donation = validCashDonation();
        donation.setAmount(0.0);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForInsert(donation)
        );

        assertEquals("Amount must be greater than zero.", ex.getMessage());
    }

    @Test
    void validateInsert_rejectsMissingTreeCount() {
        Donation donation = validCashDonation();
        donation.setDonationType("Tree Sponsorship");
        donation.setTreeCount(null);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForInsert(donation)
        );

        assertEquals("Trees count must be greater than zero.", ex.getMessage());
    }

    @Test
    void validateUpdate_requiresPositiveId() {
        Donation donation = validCashDonation();
        donation.setId(0);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> DonationValidator.validateAndNormalizeForUpdate(donation)
        );

        assertEquals("Donation ID is invalid.", ex.getMessage());
    }

    private Donation validCashDonation() {
        return new Donation(
            1,
            "Valid Donor",
            "Cash",
            50.0,
            "Card",
            LocalDate.now(),
            "Pending",
            "note",
            null
        );
    }
}
