package com.esprit.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Détecte les colonnes réelles de la table {@code registrations} (BDD existantes variées).
 */
public final class RegistrationTableSchema {

    private final Set<String> columnsLower;
    private final String paymentColumn;
    private final boolean hasStatus;

    public RegistrationTableSchema(Connection conn) throws SQLException {
        this(loadColumnNamesFromDb(conn));
    }

    /**
     * Pour tests ou outils : colonnes telles qu'en base (insensible à la casse).
     */
    public static RegistrationTableSchema fromKnownColumns(Collection<String> columnNames) {
        Set<String> lower = new HashSet<>();
        for (String c : columnNames) {
            if (c != null) {
                lower.add(c.toLowerCase(Locale.ROOT));
            }
        }
        return new RegistrationTableSchema(lower);
    }

    private RegistrationTableSchema(Set<String> columnsLower) {
        this.columnsLower = Set.copyOf(columnsLower);
        if (this.columnsLower.contains("payment_method")) {
            paymentColumn = "payment_method";
        } else if (this.columnsLower.contains("paymentmethod")) {
            paymentColumn = "paymentMethod";
        } else {
            paymentColumn = "payment_method";
        }
        hasStatus = this.columnsLower.contains("status");
    }

    private static Set<String> loadColumnNamesFromDb(Connection conn) throws SQLException {
        Set<String> set = new HashSet<>();
        DatabaseMetaData md = conn.getMetaData();
        String catalog = conn.getCatalog();
        if (catalog != null && !catalog.isBlank()) {
            addColumns(md, catalog, "registrations", set);
            if (set.isEmpty()) {
                addColumns(md, catalog, "REGISTRATIONS", set);
            }
        }
        if (set.isEmpty()) {
            addColumns(md, null, "registrations", set);
        }
        if (set.isEmpty()) {
            addColumns(md, null, "REGISTRATIONS", set);
        }
        return set;
    }

    private static void addColumns(DatabaseMetaData md, String catalog, String table, Set<String> out)
            throws SQLException {
        try (ResultSet rs = md.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col != null) {
                    out.add(col.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public boolean has(String column) {
        return columnsLower.contains(column.toLowerCase(Locale.ROOT));
    }

    public String paymentColumn() {
        return paymentColumn;
    }

    public boolean hasStatus() {
        return hasStatus;
    }

    /** Colonne prénom (anglais, français ou sans underscore). */
    public String firstNameColumn() {
        if (has("first_name")) {
            return "first_name";
        }
        if (has("firstname")) {
            return "firstname";
        }
        if (has("prenom")) {
            return "prenom";
        }
        return null;
    }

    /** Colonne nom de famille. */
    public String lastNameColumn() {
        if (has("last_name")) {
            return "last_name";
        }
        if (has("lastname")) {
            return "lastname";
        }
        if (has("nom")) {
            return "nom";
        }
        return null;
    }

    /** Une seule colonne texte pour le participant. */
    public String participantNameColumn() {
        if (has("participant_name")) {
            return "participant_name";
        }
        if (has("participantname")) {
            return "participantname";
        }
        if (has("full_name")) {
            return "full_name";
        }
        if (has("fullname")) {
            return "fullname";
        }
        if (has("nom_complet")) {
            return "nom_complet";
        }
        if (has("participant")) {
            return "participant";
        }
        return null;
    }

    /** Colonne email selon schémas possibles. */
    public String emailColumn() {
        if (has("email")) {
            return "email";
        }
        if (has("mail")) {
            return "mail";
        }
        return null;
    }

    public boolean canStoreNames() {
        return (firstNameColumn() != null && lastNameColumn() != null)
                || firstNameColumn() != null
                || lastNameColumn() != null
                || participantNameColumn() != null;
    }

    public boolean hasRegistrationDate() {
        return has("registration_date");
    }

    public String isPaidColumn() {
        if (has("is_paid")) return "is_paid";
        if (has("ispaid")) return "isPaid";
        return null;
    }

    public String paymentDateColumn() {
        if (has("payment_date")) return "payment_date";
        if (has("paymentdate")) return "paymentDate";
        return null;
    }
}

