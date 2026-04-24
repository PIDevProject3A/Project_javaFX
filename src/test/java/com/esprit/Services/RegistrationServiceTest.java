package com.esprit.Services;

import com.esprit.entities.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du service inscriptions (même logique que les TP type {@code PersonneServiceTest}).
 */
class RegistrationServiceTest extends AbstractServiceTest {

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() throws SQLException {
        openConnection();
        createBaseSchema();
        registrationService = new RegistrationService(conn);
        seedEvent(10, "Beach");
        seedEvent(11, "Gala");
    }

    @AfterEach
    void tearDown() throws SQLException {
        closeConnection();
    }

    @Test
    void loadRegistrantSummariesByEvent_affichePrenomEtNom() throws SQLException {
        insertRegistration(10, "Mariem", "Abbes");
        insertRegistration(10, "Ali", "Ben Salah");

        Map<Integer, String> map = registrationService.loadRegistrantSummariesByEvent();

        String summary = map.get(10);
        assertNotNull(summary);
        assertTrue(summary.contains("Mariem"));
        assertTrue(summary.contains("Abbes"));
        assertTrue(summary.contains("Ali"));
        assertTrue(summary.contains("Ben Salah"));
    }

    @Test
    void ajouterEtAfficher_persistePrenomNom() throws SQLException {
        Registration r = new Registration();
        r.setUserId(1);
        r.setEventId(11);
        r.setFirstName("Sara");
        r.setLastName("Mezzi");
        r.setRegistrationDate(LocalDateTime.now());
        r.setAmount(0);
        r.setPaymentMethod("CASH");
        r.setStatus("REGISTERED");

        registrationService.ajouter(r);

        List<Registration> all = registrationService.afficher();
        assertEquals(1, all.size());
        assertEquals("Sara", all.get(0).getFirstName());
        assertEquals("Mezzi", all.get(0).getLastName());
        assertEquals("Gala", all.get(0).getEventName());
    }

    @Test
    void existeInscriptionMemePersonne_vraiSiDoublon() throws SQLException {
        insertRegistration(10, "Same", "User");

        boolean dup = registrationService.existeInscriptionMemePersonne(10, 1, "Same", "User");
        assertTrue(dup);

        boolean other = registrationService.existeInscriptionMemePersonne(10, 1, "Other", "Person");
        assertFalse(other);
    }

    @Test
    void supprimer_retireLInscription() throws SQLException {
        insertRegistration(10, "X", "Y");
        List<Registration> before = registrationService.afficher();
        int id = before.get(0).getId();

        registrationService.supprimer(id);

        assertTrue(registrationService.afficher().isEmpty());
    }

    private void seedEvent(int id, String name) throws SQLException {
        var ps = conn.prepareStatement("""
                INSERT INTO events (id, name, description, eventdate, location, price, payment_type, event_type, maxplaces)
                VALUES (?, ?, 'd', ?, 'loc', 0, 'CASH', 'FREE', 100)
                """);
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2026, 6, 1, 14, 0)));
        ps.executeUpdate();
        ps.close();
    }

    private void insertRegistration(int eventId, String fn, String ln) throws SQLException {
        var ps = conn.prepareStatement("""
                INSERT INTO registrations (user_id, event_id, first_name, last_name, amount, payment_method, status)
                VALUES (1, ?, ?, ?, 0, 'CASH', 'REGISTERED')
                """);
        ps.setInt(1, eventId);
        ps.setString(2, fn);
        ps.setString(3, ln);
        ps.executeUpdate();
        ps.close();
    }
}
