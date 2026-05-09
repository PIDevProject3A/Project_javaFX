package com.esprit.services;

import com.esprit.entities.Event;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.RegistrationTableSchema;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService implements ICrud<Event> {

    public EventService() {
    }

    private Connection getConn() {
        return MyDataBase.getInstance().getSharedConnection();
    }

    /** Explicit connection (H2 tests, etc.). */

    private RegistrationTableSchema registrationSchema;

    private RegistrationTableSchema registrationSchema() throws SQLException {
        if (registrationSchema == null) {
            registrationSchema = new RegistrationTableSchema(getConn());
        }
        return registrationSchema;
    }

    private String registrationCountSubquerySql() throws SQLException {
        if (registrationSchema().hasStatus()) {
            return """
                    SELECT event_id, COUNT(*) AS cnt
                    FROM registrations
                    WHERE status = 'REGISTERED'
                    GROUP BY event_id
                    """;
        }
        return """
                SELECT event_id, COUNT(*) AS cnt
                FROM registrations
                GROUP BY event_id
                """;
    }

    @Override
    public void ajouter(Event e) throws SQLException {

        String sql = "INSERT INTO events (name, description, event_date, location, budget, status, max_places, organizer_id, payment_type, event_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getEventDate()));
            ps.setString(4, e.getLocation());
            ps.setDouble(5, e.getPrice());
            ps.setString(6, e.getStatus() != null ? e.getStatus() : "PLANNED");
            ps.setInt(7, e.getMaxPlaces());
            ps.setNull(8, java.sql.Types.INTEGER);
            ps.setString(9, e.getPaymentType() != null ? e.getPaymentType() : "CASH");
            ps.setString(10, e.getEventType() != null ? e.getEventType() : "FREE");
            ps.executeUpdate();
        }
        System.out.println("✅ Event added!");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps1 = getConn().prepareStatement("DELETE FROM registrations WHERE event_id=?")) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        }
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM events WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }


    @Override
    public List<Event> afficher() throws SQLException {

        List<Event> list = new ArrayList<>();

        String sql = """
                SELECT e.id, e.name, e.description, e.event_date, e.location, e.budget, e.status, e.max_places,
                       e.payment_type, e.event_type,
                       COALESCE(rc.cnt, 0) AS registration_count
                FROM events e
                LEFT JOIN (
                %s
                ) rc ON e.id = rc.event_id
                """.formatted(registrationCountSubquerySql().trim().replace("\n", " "));

        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setName(rs.getString("name"));
                e.setDescription(rs.getString("description"));
                e.setLocation(rs.getString("location"));
                e.setPrice(rs.getDouble("budget"));
                e.setStatus(rs.getString("status"));
                Timestamp ts = rs.getTimestamp("event_date");
                if (ts != null) e.setEventDate(ts.toLocalDateTime());
                e.setMaxPlaces(rs.getInt("max_places"));
                e.setPaymentType(rs.getString("payment_type"));
                e.setEventType(rs.getString("event_type"));
                e.setCurrentParticipants(rs.getInt("registration_count"));
                list.add(e);
            }
        }

        return list;
    }

    @Override
    public void modifier(Event e) throws SQLException {
        String sql = "UPDATE events SET name=?, description=?, event_date=?, location=?, budget=?, status=?, max_places=?, payment_type=?, event_type=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getEventDate()));
            ps.setString(4, e.getLocation());
            ps.setDouble(5, e.getPrice());
            ps.setString(6, e.getStatus() != null ? e.getStatus() : "PLANNED");
            ps.setInt(7, e.getMaxPlaces());
            ps.setString(8, e.getPaymentType());
            ps.setString(9, e.getEventType());
            ps.setInt(10, e.getId());
            ps.executeUpdate();
        }
        System.out.println("✅ Event modified!");
    }

    public Event trouverParId(int id) throws SQLException {
        String sql = """
                SELECT id, name, description, event_date, location, budget, status, max_places, payment_type, event_type
                FROM events
                WHERE id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Event e = new Event();
                    e.setId(rs.getInt("id"));
                    e.setName(rs.getString("name"));
                    e.setDescription(rs.getString("description"));
                    Timestamp ts = rs.getTimestamp("event_date");
                    if (ts != null) {
                        e.setEventDate(ts.toLocalDateTime());
                    }
                    e.setLocation(rs.getString("location"));
                    e.setPrice(rs.getDouble("budget"));
                    e.setStatus(rs.getString("status"));
                    e.setMaxPlaces(rs.getInt("max_places"));
                    e.setPaymentType(rs.getString("payment_type"));
                    e.setEventType(rs.getString("event_type"));
                    return e;
                }
            }
        }
        return null;
    }
}

