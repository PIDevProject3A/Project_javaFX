package com.esprit.Services;

import com.esprit.entities.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du service événements (même logique que les TP type {@code PersonneServiceTest}).
 */
class EventServiceTest extends AbstractServiceTest {

    private EventService eventService;

    @BeforeEach
    void setUp() throws SQLException {
        openConnection();
        createBaseSchema();
        eventService = new EventService(conn);
    }

    @AfterEach
    void tearDown() throws SQLException {
        closeConnection();
    }

    @Test
    void ajouterPuisAfficher_contientLEvenement() throws SQLException {
        Event e = new Event();
        e.setName("Conférence Java");
        e.setDescription("Intro aux services");
        e.setEventDate(LocalDateTime.of(2026, 5, 1, 10, 0));
        e.setLocation("Tunis");
        e.setPrice(0);
        e.setPaymentType("CASH");
        e.setEventType("FREE");
        e.setMaxPlaces(100);

        eventService.ajouter(e);

        List<Event> list = eventService.afficher();
        assertEquals(1, list.size());
        Event loaded = list.get(0);
        assertTrue(loaded.getId() > 0);
        assertEquals("Conférence Java", loaded.getName());
        assertEquals(0, loaded.getCurrentParticipants());
    }

    @Test
    void afficher_avecInscription_compteParticipants() throws SQLException {
        insertEvent(1, "Beach", 50);
        conn.createStatement().execute("""
                INSERT INTO registrations (user_id, event_id, first_name, last_name, amount, payment_method, status)
                VALUES (1, 1, 'Mariem', 'Abbes', 0, 'CASH', 'REGISTERED')
                """);

        List<Event> list = eventService.afficher();
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getCurrentParticipants());
    }

    @Test
    void supprimer_retireEvenementEtInscriptions() throws SQLException {
        insertEvent(1, "X", 10);
        conn.createStatement().execute("""
                INSERT INTO registrations (user_id, event_id, first_name, last_name, amount, payment_method, status)
                VALUES (1, 1, 'A', 'B', 0, 'CASH', 'REGISTERED')
                """);

        eventService.supprimer(1);

        assertEquals(0, eventService.afficher().size());
    }

    @Test
    void modifier_metAJourLesChamps() throws SQLException {
        insertEvent(1, "Ancien", 20);
        List<Event> before = eventService.afficher();
        Event e = before.get(0);
        e.setName("Nouveau nom");
        e.setMaxPlaces(99);

        eventService.modifier(e);

        Event reloaded = eventService.afficher().get(0);
        assertEquals("Nouveau nom", reloaded.getName());
        assertEquals(99, reloaded.getMaxPlaces());
    }

    private void insertEvent(int id, String name, int maxPlaces) throws SQLException {
        var ps = conn.prepareStatement("""
                INSERT INTO events (id, name, description, eventdate, location, price, payment_type, event_type, maxplaces)
                VALUES (?, ?, 'd', ?, 'loc', 0, 'CASH', 'FREE', ?)
                """);
        ps.setInt(1, id);
        ps.setString(2, name);
        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 12, 0)));
        ps.setInt(4, maxPlaces);
        ps.executeUpdate();
        ps.close();
    }
}
