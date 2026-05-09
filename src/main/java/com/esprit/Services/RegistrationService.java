package com.esprit.services;

import com.esprit.entities.Registration;
import com.esprit.utils.AppSession;
import com.esprit.utils.MyDataBase;
import com.esprit.utils.RegistrationTableSchema;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RegistrationService implements ICrud<Registration> {

    public RegistrationService() {
        ensureIdentityColumns();
    }

    private Connection getConn() {
        return MyDataBase.getInstance().getSharedConnection();
    }

    private RegistrationTableSchema schema;

    private RegistrationTableSchema schema() throws SQLException {
        if (schema == null) {
            schema = new RegistrationTableSchema(getConn());
        }
        return schema;
    }

    /**
     * Light auto-migration to prevent data loss (first_name, last_name, email)
     * when the registrations table is old.
     */
    private void ensureIdentityColumns() {
        String[] ddl = {
                "ALTER TABLE registrations ADD COLUMN first_name VARCHAR(80) NOT NULL DEFAULT ''",
                "ALTER TABLE registrations ADD COLUMN last_name VARCHAR(80) NOT NULL DEFAULT ''",
                "ALTER TABLE registrations ADD COLUMN email VARCHAR(180) NULL",
                "ALTER TABLE registrations ADD COLUMN registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE registrations ADD COLUMN checked_in TINYINT(1) NOT NULL DEFAULT 0",
                "ALTER TABLE registrations ADD COLUMN check_in_time DATETIME NULL",
                "ALTER TABLE registrations ADD COLUMN status VARCHAR(50) DEFAULT 'REGISTERED'",
                "ALTER TABLE registrations ADD COLUMN is_paid TINYINT(1) NOT NULL DEFAULT 0",
                "ALTER TABLE registrations ADD COLUMN budget DOUBLE DEFAULT 0"
        };
        for (String sql : ddl) {
            try (Statement st = getConn().createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException ignored) {
                // Column already exists or insufficient permissions: continue without blocking
            }
        }
        schema = null;
    }

    // ========================= HELPER =========================
    private boolean existsByQuery(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
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
        String emailCol = s.emailColumn(); // Unique declaration here

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

        if (emailCol != null) {
            cols.add(emailCol);
            vals.add(r.getEmail());
        }

        cols.add(s.paymentColumn());
        vals.add(r.getPaymentMethod());

        cols.add("amount");
        vals.add(r.getAmount());

        // ===== NOUVEAUX CHAMPS POUR LE PAIEMENT =====
        if (s.has("budget")) {
            cols.add("budget");
            vals.add(r.getBudget());
        }

        String isPaidCol = s.isPaidColumn();
        if (isPaidCol != null) {
            cols.add(isPaidCol);
            vals.add(r.isPaid() ? 1 : 0);
        }

        if (s.hasStatus()) {
            cols.add("status");
            vals.add(r.getStatus() != null ? r.getStatus() : "REGISTERED");
        }

        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO registrations (" + String.join(", ", cols) + ") VALUES (" + placeholders + ")";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
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
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM registrations WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ========================= AFFICHER =========================
    @Override
    public List<Registration> afficher() throws SQLException {
        RegistrationTableSchema s = schema();
        String sql = selectFromRegistrationsJoinEvents(s) + orderByRegistration(s);

        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
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
                && !"ALL".equalsIgnoreCase(paymentMethodOrNull)) {
            sql.append(and ? " AND " : " WHERE ");
            sql.append(" r.").append(s.paymentColumn()).append(" = ? ");
            params.add(paymentMethodOrNull);
        }

        sql.append(orderByRegistration(s));

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
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
            expr = "CONCAT(COUNT(*), ' registration(s)')";
        }

        String where = s.hasStatus() ? " WHERE r.status IN ('REGISTERED', 'PAID', 'PENDING_PAYMENT') " : "";
        String sql = "SELECT r.event_id, " + expr + " AS names FROM registrations r " + where + " GROUP BY r.event_id";

        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
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
        RegistrationTableSchema s = schema();
        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String emailCol = s.emailColumn();
        StringBuilder sql = new StringBuilder("""
            SELECT r.*, e.name AS event_name
            FROM registrations r
            INNER JOIN events e ON r.event_id = e.id
            WHERE r.id = ?
            """);
        if (fCol != null) sql.insert(sql.indexOf("FROM"), ", r." + fCol + " AS reg_first_name ");
        if (lCol != null) sql.insert(sql.indexOf("FROM"), ", r." + lCol + " AS reg_last_name ");
        if (pCol != null) sql.insert(sql.indexOf("FROM"), ", r." + pCol + " AS reg_participant_name ");
        if (emailCol != null) sql.insert(sql.indexOf("FROM"), ", r." + emailCol + " AS reg_email ");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
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

        String emailCol = s.emailColumn();
        if (emailCol != null) {
            sets.add(emailCol + " = ?");
            vals.add(r.getEmail());
        }

        sets.add("amount = ?");
        vals.add(r.getAmount());
        sets.add(s.paymentColumn() + " = ?");
        vals.add(r.getPaymentMethod());

        if (s.has("budget")) {
            sets.add("budget = ?");
            vals.add(r.getBudget());
        }

        String isPaidCol = s.isPaidColumn();
        if (isPaidCol != null) {
            sets.add(isPaidCol + " = ?");
            vals.add(r.isPaid() ? 1 : 0);
        }

        String pDateCol = s.paymentDateColumn();
        if (pDateCol != null && r.getPaymentDate() != null) {
            sets.add(pDateCol + " = ?");
            vals.add(Timestamp.valueOf(r.getPaymentDate()));
        }

        if (s.hasStatus()) {
            sets.add("status = ?");
            vals.add(r.getStatus() != null ? r.getStatus() : "REGISTERED");
        }

        StringBuilder sql = new StringBuilder("UPDATE registrations SET ");
        sql.append(String.join(", ", sets));
        sql.append(" WHERE id = ?");
        vals.add(r.getId());

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
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

    // ========================= VÉRIFIER SI LE BUDGET EST PAYÉ =========================
    public boolean isBudgetPaid(int registrationId) throws SQLException {
        String isPaidCol = schema().isPaidColumn();
        if (isPaidCol == null) return false;
        String sql = "SELECT " + isPaidCol + " FROM registrations WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    // ========================= EFFECTUER PAIEMENT =========================
    public void effectuerPaiement(int registrationId) throws SQLException {
        RegistrationTableSchema s = schema();
        List<String> sets = new ArrayList<>();
        String isPaidCol = s.isPaidColumn();
        if (isPaidCol != null) {
            sets.add(isPaidCol + " = TRUE");
        }
        String pDateCol = s.paymentDateColumn();
        if (pDateCol != null) {
            sets.add(pDateCol + " = NOW()");
        }
        if (s.has("payment_status")) {
            sets.add("payment_status = 'PAID'");
        }
        if (s.hasStatus()) {
            sets.add("status = 'PAID'");
        }
        if (sets.isEmpty()) {
            return;
        }
        String sql = "UPDATE registrations SET " + String.join(", ", sets) + " WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.executeUpdate();
        }
    }

    public void saveStripeSession(int registrationId, String sessionId, String paymentStatus) throws SQLException {
        RegistrationTableSchema s = schema();
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        String normalizedStatus = (paymentStatus == null || paymentStatus.isBlank()) ? "PENDING" : paymentStatus;
        if (s.has("stripe_session_id")) {
            sets.add("stripe_session_id = ?");
            vals.add(sessionId);
        }
        if (s.has("payment_status")) {
            sets.add("payment_status = ?");
            vals.add(normalizedStatus);
        }
        if (s.hasStatus()) {
            sets.add("status = ?");
            vals.add("PAID".equalsIgnoreCase(normalizedStatus) ? "PAID" : "PENDING_PAYMENT");
        }
        if (sets.isEmpty()) {
            return;
        }
        String sql = "UPDATE registrations SET " + String.join(", ", sets) + " WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            int i = 1;
            for (Object v : vals) {
                ps.setString(i++, v != null ? v.toString() : null);
            }
            ps.setInt(i, registrationId);
            ps.executeUpdate();
        }
    }

    // ========================= VÉRIFIER SI PEUT S'INSCRIRE (PLACES DISPOS) =========================
    public boolean peutSInscrire(int eventId) throws SQLException {
        String sql = "SELECT max_places FROM events WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxPlaces = rs.getInt("max_places");
                    if (maxPlaces <= 0) return true; // Unlimited places

                    String isPaidCol = schema().isPaidColumn();
                    if (isPaidCol == null) {
                        String countSql = "SELECT COUNT(*) FROM registrations WHERE event_id = ?";
                        try (PreparedStatement ps2 = getConn().prepareStatement(countSql)) {
                            ps2.setInt(1, eventId);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    return rs2.getInt(1) < maxPlaces;
                                }
                            }
                        }
                    } else {
                        // Count paid registrations
                        String countSql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND " + isPaidCol + " = TRUE";
                        try (PreparedStatement ps2 = getConn().prepareStatement(countSql)) {
                            ps2.setInt(1, eventId);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    return rs2.getInt(1) < maxPlaces;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // ========================= HELPERS PRIVÉS =========================
    private String selectFromRegistrationsJoinEvents(RegistrationTableSchema s) {
        StringBuilder sb = new StringBuilder("SELECT r.*, e.name AS event_name");
        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String emailCol = s.emailColumn();
        if (fCol != null) sb.append(", r.").append(fCol).append(" AS reg_first_name");
        if (lCol != null) sb.append(", r.").append(lCol).append(" AS reg_last_name");
        if (pCol != null) sb.append(", r.").append(pCol).append(" AS reg_participant_name");
        if (emailCol != null) sb.append(", r.").append(emailCol).append(" AS reg_email");
        sb.append(" FROM registrations r ");
        sb.append("INNER JOIN events e ON r.event_id = e.id ");
        if (s.hasStatus()) {
            sb.append("WHERE r.status IN ('REGISTERED', 'PAID', 'PENDING_PAYMENT') ");
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
                case "reg_first_name" -> r.setFirstName(rs.getString(i));
                case "reg_last_name" -> r.setLastName(rs.getString(i));
                case "participant_name", "full_name", "nom_complet", "participant" ->
                        applyParticipantFull(rs.getString(i), r);
                case "reg_participant_name" -> applyParticipantFull(rs.getString(i), r);
                case "registration_date" -> {
                    Timestamp ts = rs.getTimestamp(i);
                    if (ts != null) {
                        r.setRegistrationDate(ts.toLocalDateTime());
                    }
                }
                case "email", "mail" -> r.setEmail(rs.getString(i));
                case "reg_email" -> r.setEmail(rs.getString(i));
                case "amount" -> r.setAmount(rs.getDouble(i));
                case "budget" -> r.setBudget(rs.getDouble(i));
                case "is_paid", "ispaid" -> r.setPaid(rs.getBoolean(i));
                case "payment_date", "paymentdate" -> {
                    Timestamp ts = rs.getTimestamp(i);
                    if (ts != null) {
                        r.setPaymentDate(ts.toLocalDateTime());
                    }
                }
                case "payment_method", "paymentmethod" -> r.setPaymentMethod(rs.getString(i));
                case "status" -> r.setStatus(rs.getString(i));
                case "event_name" -> r.setEventName(rs.getString(i));
            }
        }
        return r;
    }

    // ========================= TROUVER PAR USER + EVENT + NOM =========================
    public Registration trouverParIdUtilisateur(int userId, int eventId, String firstName, String lastName)
            throws SQLException {
        RegistrationTableSchema s = schema();
        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String emailCol = s.emailColumn(); // Ajouté ici

        StringBuilder sql = new StringBuilder("""
        SELECT r.*, e.name AS event_name
        FROM registrations r
        INNER JOIN events e ON r.event_id = e.id
        WHERE r.user_id = ? AND r.event_id = ?
        """);
        if (fCol != null) sql.insert(sql.indexOf("FROM"), ", r." + fCol + " AS reg_first_name ");
        if (lCol != null) sql.insert(sql.indexOf("FROM"), ", r." + lCol + " AS reg_last_name ");
        if (pCol != null) sql.insert(sql.indexOf("FROM"), ", r." + pCol + " AS reg_participant_name ");
        if (emailCol != null) sql.insert(sql.indexOf("FROM"), ", r." + emailCol + " AS reg_email ");

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(eventId);

        String fn = firstName != null ? firstName.trim() : "";
        String ln = lastName != null ? lastName.trim() : "";
        String full = (fn + " " + ln).trim();

        if (fCol != null && lCol != null) {
            sql.append(" AND LOWER(TRIM(r.").append(fCol).append(")) = LOWER(TRIM(?))");
            sql.append(" AND LOWER(TRIM(r.").append(lCol).append(")) = LOWER(TRIM(?))");
            params.add(fn);
            params.add(ln);
        } else if (fCol != null || lCol != null) {
            String one = fCol != null ? fCol : lCol;
            sql.append(" AND LOWER(TRIM(r.").append(one).append(")) = LOWER(TRIM(?))");
            params.add(full);
        } else if (pCol != null) {
            sql.append(" AND LOWER(TRIM(r.").append(pCol).append(")) = LOWER(TRIM(?))");
            params.add(full);
        }

        sql.append(" ORDER BY r.id DESC LIMIT 1");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, p != null ? p.toString() : "");
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Met à jour l'email d'une inscription par son ID.
     */
    public void updateEmailById(int registrationId, String newEmail) throws SQLException {
        RegistrationTableSchema s = schema();
        String emailCol = s.emailColumn();
        if (emailCol == null) {
            return; // email column doesn't exist
        }
        try (PreparedStatement ps = getConn().prepareStatement("UPDATE registrations SET " + emailCol + " = ? WHERE id = ?")) {
            ps.setString(1, newEmail != null ? newEmail.trim() : "");
            ps.setInt(2, registrationId);
            ps.executeUpdate();
        }
    }

    public Map<Integer, Integer> loadRegistrantCountsByEvent() throws SQLException {
        RegistrationTableSchema s = schema();
        Map<Integer, Integer> map = new HashMap<>();
        String where = s.hasStatus() ? " WHERE r.status IN ('REGISTERED', 'PAID', 'PENDING_PAYMENT') " : "";
        String sql = "SELECT r.event_id, COUNT(*) AS cnt FROM registrations r " + where + " GROUP BY r.event_id";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getInt("event_id"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public List<RegistrantExportRow> listRegistrantsForEvent(int eventId) throws SQLException {
        RegistrationTableSchema s = schema();
        String fCol = s.firstNameColumn();
        String lCol = s.lastNameColumn();
        String pCol = s.participantNameColumn();
        String eCol = s.emailColumn();

        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> cols = new ArrayList<>();
        cols.add("r.id AS registration_id");
        cols.add((fCol != null ? "r." + fCol : "''") + " AS first_name");
        cols.add((lCol != null ? "r." + lCol : "''") + " AS last_name");
        cols.add((pCol != null ? "r." + pCol : "''") + " AS participant_name");
        cols.add((eCol != null ? "r." + eCol : "''") + " AS email");
        cols.add((s.has("checked_in") ? "r.checked_in" : "0") + " AS checked_in");
        sql.append(String.join(", ", cols));
        sql.append(" FROM registrations r WHERE r.event_id = ? ");
        if (s.hasStatus()) {
            sql.append(" AND r.status IN ('REGISTERED', 'PAID', 'PENDING_PAYMENT') ");
        }
        sql.append(" ORDER BY r.id ASC ");

        List<RegistrantExportRow> out = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int registrationId = rs.getInt("registration_id");
                    String first = safe(rs.getString("first_name"));
                    String last = safe(rs.getString("last_name"));
                    String participant = safe(rs.getString("participant_name"));
                    String email = safe(rs.getString("email"));
                    boolean checkedIn = rs.getInt("checked_in") == 1;

                    if ((first.isBlank() || last.isBlank()) && !participant.isBlank()) {
                        String[] parts = participant.trim().split("\\s+", 2);
                        if (first.isBlank()) first = parts[0];
                        if (last.isBlank()) last = parts.length > 1 ? parts[1] : "";
                    }
                    out.add(new RegistrantExportRow(registrationId, first, last, email, checkedIn));
                }
            }
        }
        return out;
    }

    public boolean markCheckedInFromReceiptCode(int eventId, String rawScan) throws SQLException {
        String code = extractReceiptCode(rawScan);
        if (code == null || code.isBlank()) {
            return false;
        }
        Integer registrationId = parseRegistrationId(code);
        if (registrationId == null) {
            return false;
        }
        String sql = "UPDATE registrations SET checked_in = 1, check_in_time = NOW() WHERE id = ? AND event_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.setInt(2, eventId);
            return ps.executeUpdate() > 0;
        }
    }

    private static String extractReceiptCode(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.contains("receipt=")) {
            for (String part : t.split("\\|")) {
                String p = part.trim();
                if (p.startsWith("receipt=")) {
                    return p.substring("receipt=".length()).trim();
                }
            }
        }
        return t;
    }

    private static Integer parseRegistrationId(String code) {
        String prefix = "BLD-REG-";
        if (!code.toUpperCase(Locale.ROOT).startsWith(prefix)) {
            return null;
        }
        try {
            return Integer.parseInt(code.substring(prefix.length()).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public record RegistrantExportRow(int registrationId, String firstName, String lastName, String email, boolean checkedIn) {}

    /**
     * Returns "event tomorrow" notifications for the user.
     * Displayed throughout the entire day of D-1.
     */
    public List<String> remindersForTomorrow(int userId) throws SQLException {
        RegistrationTableSchema s = schema();
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT e.name, e.event_date
                FROM registrations r
                INNER JOIN events e ON e.id = r.event_id
                WHERE e.event_date >= ? AND e.event_date < ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(start));
        params.add(Timestamp.valueOf(end));

        if (s.has("user_id")) {
            // In demo mode, some old rows may have NULL user_id.
            // We keep the user filter but do not exclude old NULL registrations.
            sql.append(" AND (r.user_id = ? OR r.user_id IS NULL) ");
            params.add(userId);
        }
        if (s.hasStatus()) {
            // Old data may have NULL status.
            sql.append(" AND (r.status IN ('REGISTERED', 'PAID', 'PENDING_PAYMENT') OR r.status IS NULL) ");
        }
        sql.append(" ORDER BY e.event_date ASC ");

        List<String> out = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Timestamp ts) {
                    ps.setTimestamp(i + 1, ts);
                } else if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    Timestamp ts = rs.getTimestamp("event_date");
                    if (name == null || name.isBlank() || ts == null) {
                        continue;
                    }
                    out.add(name.trim() + " (" + ts.toLocalDateTime().format(fmt) + ")");
                }
            }
        }
        return out;
    }
}

