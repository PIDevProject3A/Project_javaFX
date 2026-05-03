package com.esprit.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationTableSchemaTest {

    @Test
    void fromKnownColumns_reconnaitPrenomEtNom() {
        RegistrationTableSchema s = RegistrationTableSchema.fromKnownColumns(
                List.of("PRENOM", "nom", "event_id", "payment_method", "status"));
        assertEquals("prenom", s.firstNameColumn());
        assertEquals("nom", s.lastNameColumn());
        assertTrue(s.hasStatus());
    }

    @Test
    void fromKnownColumns_reconnaitFirstNameLastName() {
        RegistrationTableSchema s = RegistrationTableSchema.fromKnownColumns(
                List.of("first_name", "last_name"));
        assertEquals("first_name", s.firstNameColumn());
        assertEquals("last_name", s.lastNameColumn());
    }
}

