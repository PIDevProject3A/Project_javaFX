package com.esprit.Services;

import com.esprit.entities.Event;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.RegistrationTableSchema;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService implements ICrud<Event> {

    private final Connection conn;

    public EventService() {
        this(MyDataBase.getInstance().getConnection());
    }

    /** Connexion explicite (tests H2, etc.). */
    public EventService(Connection connection) {
        this.conn = connection;
    }

    private RegistrationTableSchema registrationSchema;

    private RegistrationTableSchema registrationSchema() throws SQLException {
        if (registrationSchema == null) {
            registrationSchema = new RegistrationTableSchema(conn);
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

        String sql = "INSERT INTO events (name, description, eventDate, location, price, payment_type, event_type, maxPlaces) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getEventDate()));
            ps.setString(4, e.getLocation());
            ps.setDouble(5, e.getPrice());
            ps.setString(6, e.getPaymentType());
            ps.setString(7, e.getEventType());
            ps.setInt(8, e.getMaxPlaces());
            ps.executeUpdate();
        }
        System.out.println("✅ Event ajouté !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM registrations WHERE event_id=?")) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM events WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }


    @Override
    public List<Event> afficher() throws SQLException {

        List<Event> list = new ArrayList<>();

        String sql = """
                SELECT e.id, e.name, e.description, e.eventDate, e.location, e.price, e.payment_type, e.event_type, e.maxPlaces,
                       COALESCE(rc.cnt, 0) AS registration_count
                FROM events e
                LEFT JOIN (
                %s
                ) rc ON e.id = rc.event_id
                """.formatted(registrationCountSubquerySql().trim().replace("\n", " "));

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setName(rs.getString("name"));
                e.setDescription(rs.getString("description"));
                e.setLocation(rs.getString("location"));
                e.setPrice(rs.getDouble("price"));
                e.setEventType(rs.getString("event_type"));
                e.setEventDate(rs.getTimestamp("eventDate").toLocalDateTime());
                e.setPaymentType(rs.getString("payment_type"));
                e.setMaxPlaces(rs.getInt("maxPlaces"));
                e.setCurrentParticipants(rs.getInt("registration_count"));
                list.add(e);
            }
        }

        return list;
    }

    @Override
    public void modifier(Event e) throws SQLException {
        String sql = "UPDATE events SET name=?, description=?, eventDate=?, location=?, price=?, payment_type=?, event_type=?, maxPlaces=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(e.getEventDate()));
            ps.setString(4, e.getLocation());
            ps.setDouble(5, e.getPrice());
            ps.setString(6, e.getPaymentType());
            ps.setString(7, e.getEventType());
            ps.setInt(8, e.getMaxPlaces());
            ps.setInt(9, e.getId());
            ps.executeUpdate();
        }
        System.out.println("✅ Event modifié !");
    }

    public Event trouverParId(int id) throws SQLException {
        String sql = """
                SELECT id, name, description, eventDate, location, price, payment_type, event_type, maxPlaces
                FROM events
                WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Event e = new Event();
                    e.setId(rs.getInt("id"));
                    e.setName(rs.getString("name"));
                    e.setDescription(rs.getString("description"));
                    Timestamp ts = rs.getTimestamp("eventDate");
                    if (ts != null) {
                        e.setEventDate(ts.toLocalDateTime());
                    }
                    e.setLocation(rs.getString("location"));
                    e.setPrice(rs.getDouble("price"));
                    e.setPaymentType(rs.getString("payment_type"));
                    e.setEventType(rs.getString("event_type"));
                    e.setMaxPlaces(rs.getInt("maxPlaces"));
                    return e;
                }
            }
        }
        return null;
    }
}
