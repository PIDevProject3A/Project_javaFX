package com.esprit.Services;

import com.esprit.entities.Registration;
import com.esprit.utils.AppSession;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.RegistrationTableSchema;

import java.sql.*;
import java.util.*;

public class RegistrationService implements ICrud<Registration> {

    private final Connection conn;

    public RegistrationService() {
        this(MyDataBase.getInstance().getConnection());
    }

    public RegistrationService(Connection connection) {
        this.conn = connection;
    }

    private RegistrationTableSchema schema;

    private RegistrationTableSchema schema() throws SQLException {
        if (schema == null) {
            schema = new RegistrationTableSchema(conn);
        }
        return schema;
    }

    // ========================= HELPER =========================
    private boolean existsByQuery(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, p != null ? p.toString() : "");
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ========================= AJOUTER =========================
    @Override
    public void ajouter(Registration r) throws SQLException {
        RegistrationTableSchema s = schema();
        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        if (s.has("user_id")) {
            cols.add("user_id");
            vals.add(r.getUserId() > 0 ? r.getUserId() : AppSession.getCurrentUserId());
        }

        cols.add("event_id");
        vals.add(r.getEventId());

        String fn = r.getFirstName() != null ? r.getFirstName().trim() : "";
        String ln = r.getLastName() != null ? r.getLastName().trim() : "";
        String full = (fn + " " + ln).trim();

        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();

        if (fCol != null && lCol != null) {
            cols.add(fCol);
            vals.add(fn);
            cols.add(lCol);
            vals.add(ln);
        } else if (fCol != null) {
            cols.add(fCol);
            vals.add(full);
        } else if (lCol != null) {
            cols.add(lCol);
            vals.add(full);
        } else if (pCol != null) {
            cols.add(pCol);
            vals.add(full);
        }

        if (s.hasRegistrationDate()) {
            cols.add("registration_date");
            vals.add(Timestamp.valueOf(
                    r.getRegistrationDate() != null ? r.getRegistrationDate() : java.time.LocalDateTime.now()));
        }

        cols.add(s.paymentColumn());
        vals.add(r.getPaymentMethod());

        cols.add("amount");
        vals.add(r.getAmount());

        if (s.hasStatus()) {
            cols.add("status");
            vals.add(r.getStatus() != null ? r.getStatus() : "REGISTERED");
        }

        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO registrations (" + String.join(", ", cols) + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < vals.size(); i++) {
                Object v = vals.get(i);
                if (v instanceof Integer n) ps.setInt(i + 1, n);
                else if (v instanceof Double d) ps.setDouble(i + 1, d);
                else if (v instanceof Timestamp t) ps.setTimestamp(i + 1, t);
                else ps.setString(i + 1, v != null ? v.toString() : null);
            }
            ps.executeUpdate();
        }
    }

    // ========================= SUPPRIMER =========================
    @Override
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM registrations WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ========================= AFFICHER (NÉCESSAIRE POUR ICrud) =========================
    @Override
    public List<Registration> afficher() throws SQLException {
        RegistrationTableSchema s = schema();
        String sql = selectFromRegistrationsJoinEvents(s) + orderByRegistration(s);

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return mapResultSet(rs);
        }
    }

    // ========================= LISTER FILTRÉ =========================
    public List<Registration> listerFiltre(int userId, String firstName, String lastName,
                                           String eventNameContains, String paymentMethodOrNull)
            throws SQLException {
        RegistrationTableSchema s = schema();
        StringBuilder sql = new StringBuilder(selectFromRegistrationsJoinEvents(s));
        List<Object> params = new ArrayList<>();
        boolean and = s.hasStatus();

        if (s.has("user_id")) {
            sql.append(and ? " AND " : " WHERE ");
            and = true;
            sql.append(" r.user_id = ? ");
            params.add(userId);
        }

        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();

        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            String combined = (firstName.trim() + " " + lastName.trim()).trim();
            if (fCol != null && lCol != null) {
                sql.append(and ? " AND " : " WHERE ");
                and = true;
                sql.append(" r.").append(fCol).append(" = ? AND r.").append(lCol).append(" = ? ");
                params.add(firstName.trim());
                params.add(lastName.trim());
            } else if (fCol != null || lCol != null) {
                String one = fCol != null ? fCol : lCol;
                sql.append(and ? " AND " : " WHERE ");
                and = true;
                sql.append(" LOWER(TRIM(r.").append(one).append(")) = LOWER(TRIM(?)) ");
                params.add(combined);
            } else if (pCol != null) {
                sql.append(and ? " AND " : " WHERE ");
                and = true;
                sql.append(" LOWER(TRIM(r.").append(pCol).append(")) = LOWER(TRIM(?)) ");
                params.add(combined);
            }
        }

        if (eventNameContains != null && !eventNameContains.isBlank()) {
            sql.append(and ? " AND " : " WHERE ");
            and = true;
            sql.append(" e.name LIKE ? ");
            params.add("%" + eventNameContains.trim() + "%");
        }

        if (paymentMethodOrNull != null && !paymentMethodOrNull.isBlank()
                && !"TOUTES".equalsIgnoreCase(paymentMethodOrNull)) {
            sql.append(and ? " AND " : " WHERE ");
            sql.append(" r.").append(s.paymentColumn()).append(" = ? ");
            params.add(paymentMethodOrNull);
        }

        sql.append(orderByRegistration(s));

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String str) {
                    ps.setString(i + 1, str);
                } else if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                }
            }
            return mapResultSet(ps.executeQuery());
        }
    }

    // ========================= CHARGER RÉSUMÉS INSCRITS =========================
    public Map<Integer, String> loadRegistrantSummariesByEvent() throws SQLException {
        RegistrationTableSchema s = schema();
        Map<Integer, String> map = new HashMap<>();

        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();

        String expr;
        if (fCol != null && lCol != null) {
            expr = "GROUP_CONCAT(CONCAT(TRIM(r." + fCol + "), ' ', TRIM(r." + lCol + ")) ORDER BY r.id SEPARATOR ' · ')";
        } else if (fCol != null) {
            expr = "GROUP_CONCAT(TRIM(r." + fCol + ") ORDER BY r.id SEPARATOR ' · ')";
        } else if (lCol != null) {
            expr = "GROUP_CONCAT(TRIM(r." + lCol + ") ORDER BY r.id SEPARATOR ' · ')";
        } else if (pCol != null) {
            expr = "GROUP_CONCAT(TRIM(r." + pCol + ") ORDER BY r.id SEPARATOR ' · ')";
        } else {
            expr = "CONCAT(COUNT(*), ' inscription(s)')";
        }

        String where = s.hasStatus() ? " WHERE r.status = 'REGISTERED' " : "";
        String sql = "SELECT r.event_id, " + expr + " AS names FROM registrations r " + where + " GROUP BY r.event_id";

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int eid = rs.getInt("event_id");
                String names = rs.getString("names");
                map.put(eid, names != null && !names.isBlank() ? names : "—");
            }
        }
        return map;
    }

    // ========================= TROUVER PAR ID =========================
    public Registration trouverParId(int id) throws SQLException {
        String sql = """
            SELECT r.*, e.name AS event_name
            FROM registrations r
            INNER JOIN events e ON r.event_id = e.id
            WHERE r.id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // ========================= EXISTE INSCRIPTION SAME USER+EVENT =========================
    public boolean existeInscriptionUserEtEvent(int userId, int eventId) throws SQLException {
        if (!schema().has("user_id")) return false;

        String w = schema().hasStatus() ? " AND status = 'REGISTERED' " : "";
        String sql = "SELECT COUNT(*) FROM registrations WHERE user_id = ? AND event_id = ? " + w;

        return existsByQuery(sql, userId, eventId);
    }

    // ========================= EXISTE INSCRIPTION MÊME PERSONNE =========================
    public boolean existeInscriptionMemePersonne(int eventId, int userId, String firstName, String lastName)
            throws SQLException {
        RegistrationTableSchema s = schema();
        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String status = s.hasStatus() ? " AND status = 'REGISTERED' " : "";
        String fullName = ((firstName != null ? firstName : "").trim() + " "
                + (lastName != null ? lastName : "").trim()).trim();

        if (fCol != null && lCol != null) {
            String sql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? " + status +
                    " AND LOWER(TRIM(" + fCol + ")) = LOWER(TRIM(?)) AND LOWER(TRIM(" + lCol + ")) = LOWER(TRIM(?))";
            return existsByQuery(sql, eventId, firstName, lastName);
        }

        if (fCol != null || lCol != null) {
            String col = fCol != null ? fCol : lCol;
            String sql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? " + status +
                    " AND LOWER(TRIM(" + col + ")) = LOWER(TRIM(?))";
            return existsByQuery(sql, eventId, fullName);
        }

        if (pCol != null) {
            String sql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? " + status +
                    " AND LOWER(TRIM(" + pCol + ")) = LOWER(TRIM(?))";
            return existsByQuery(sql, eventId, fullName);
        }

        return existeInscriptionUserEtEvent(userId, eventId);
    }

    // ========================= MODIFIER =========================
    @Override
    public void modifier(Registration r) throws SQLException {
        RegistrationTableSchema s = schema();
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();

        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String fn = r.getFirstName() != null ? r.getFirstName().trim() : "";
        String ln = r.getLastName() != null ? r.getLastName().trim() : "";
        String full = (fn + " " + ln).trim();

        if (fCol != null && lCol != null) {
            sets.add(fCol + " = ?");
            vals.add(fn);
            sets.add(lCol + " = ?");
            vals.add(ln);
        } else if (fCol != null) {
            sets.add(fCol + " = ?");
            vals.add(full);
        } else if (lCol != null) {
            sets.add(lCol + " = ?");
            vals.add(full);
        } else if (pCol != null) {
            sets.add(pCol + " = ?");
            vals.add(full);
        }

        if (s.hasRegistrationDate() && r.getRegistrationDate() != null) {
            sets.add("registration_date = ?");
            vals.add(Timestamp.valueOf(r.getRegistrationDate()));
        }

        sets.add("amount = ?");
        vals.add(r.getAmount());
        sets.add(s.paymentColumn() + " = ?");
        vals.add(r.getPaymentMethod());

        if (s.hasStatus()) {
            sets.add("status = ?");
            vals.add(r.getStatus() != null ? r.getStatus() : "REGISTERED");
        }

        StringBuilder sql = new StringBuilder("UPDATE registrations SET ");
        sql.append(String.join(", ", sets));
        sql.append(" WHERE id = ?");
        vals.add(r.getId());

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < vals.size(); i++) {
                Object v = vals.get(i);
                int j = i + 1;
                if (v instanceof Double d) {
                    ps.setDouble(j, d);
                } else if (v instanceof Timestamp t) {
                    ps.setTimestamp(j, t);
                } else if (v instanceof Integer n) {
                    ps.setInt(j, n);
                } else {
                    ps.setString(j, v != null ? v.toString() : null);
                }
            }
            ps.executeUpdate();
        }
    }

    // ========================= HELPERS PRIVÉS =========================
    private String selectFromRegistrationsJoinEvents(RegistrationTableSchema s) {
        StringBuilder sb = new StringBuilder("SELECT r.*, e.name AS event_name FROM registrations r ");
        sb.append("INNER JOIN events e ON r.event_id = e.id ");
        if (s.hasStatus()) {
            sb.append("WHERE r.status = 'REGISTERED' ");
        }
        return sb.toString();
    }

    private String orderByRegistration(RegistrationTableSchema s) {
        if (s.hasRegistrationDate()) {
            return " ORDER BY r.registration_date DESC ";
        }
        return " ORDER BY r.id DESC ";
    }

    private static List<Registration> mapResultSet(ResultSet rs) throws SQLException {
        List<Registration> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private static void applyParticipantFull(String value, Registration r) {
        if (value == null || value.isBlank()) {
            return;
        }
        String t = value.trim();
        int sp = t.indexOf(' ');
        if (sp < 0) {
            r.setFirstName(t);
            r.setLastName("");
        } else {
            r.setFirstName(t.substring(0, sp).trim());
            r.setLastName(t.substring(sp + 1).trim());
        }
    }

    private static Registration mapRow(ResultSet rs) throws SQLException {
        Registration r = new Registration();
        ResultSetMetaData md = rs.getMetaData();

        for (int i = 1; i <= md.getColumnCount(); i++) {
            String lab = md.getColumnLabel(i).toLowerCase(Locale.ROOT);

            switch (lab) {
                case "id" -> r.setId(rs.getInt(i));
                case "user_id" -> r.setUserId(rs.getInt(i));
                case "event_id" -> r.setEventId(rs.getInt(i));
                case "first_name", "prenom", "firstname" -> r.setFirstName(rs.getString(i));
                case "last_name", "nom", "lastname" -> r.setLastName(rs.getString(i));
                case "participant_name", "full_name", "nom_complet", "participant" ->
                        applyParticipantFull(rs.getString(i), r);
                case "registration_date" -> {
                    Timestamp ts = rs.getTimestamp(i);
                    if (ts != null) {
                        r.setRegistrationDate(ts.toLocalDateTime());
                    }
                }
                case "amount" -> r.setAmount(rs.getDouble(i));
                case "payment_method", "paymentmethod" -> r.setPaymentMethod(rs.getString(i));
                case "status" -> r.setStatus(rs.getString(i));
                case "event_name" -> r.setEventName(rs.getString(i));
            }
        }
        return r;
    }
}
