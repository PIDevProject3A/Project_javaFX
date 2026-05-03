package com.bledna.dao;

import com.bledna.db.DatabaseConnection;
import com.bledna.model.PrevueCollection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for prevue_collection table — CRUD operations.
 */
public class PrevueCollectionDAO {

    // ── READ ALL ─────────────────────────────────────────────────────────────
    public List<PrevueCollection> getAll() throws SQLException {
        List<PrevueCollection> list = new ArrayList<>();
        String sql = "SELECT * FROM prevue_collection ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<PrevueCollection> getByCollector(int collectorId) throws SQLException {
        List<PrevueCollection> list = new ArrayList<>();
        String sql = "SELECT * FROM prevue_collection WHERE collector_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, collectorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // ── INSERT ───────────────────────────────────────────────────────────────
    public void insert(PrevueCollection p) throws SQLException {
        String sql = """
            INSERT INTO prevue_collection (type_collection, quantite, statut, waste_collection_id, collection_date, unit, collector_id)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTypeCollection());
            ps.setDouble(2, p.getQuantite());
            ps.setString(3, p.getStatut());
            if (p.getWasteCollectionId() > 0) {
                ps.setInt(4, p.getWasteCollectionId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setTimestamp(5, p.getCollectionDate() != null ? Timestamp.valueOf(p.getCollectionDate()) : null);
            ps.setString(6, p.getUnit());
            ps.setInt(7, p.getCollectorId());
            ps.executeUpdate();
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public void update(PrevueCollection p) throws SQLException {
        String sql = """
            UPDATE prevue_collection
               SET type_collection=?, quantite=?, statut=?, waste_collection_id=?, collection_date=?, unit=?, collector_id=?
             WHERE id=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTypeCollection());
            ps.setDouble(2, p.getQuantite());
            ps.setString(3, p.getStatut());
            if (p.getWasteCollectionId() > 0) {
                ps.setInt(4, p.getWasteCollectionId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setTimestamp(5, p.getCollectionDate() != null ? Timestamp.valueOf(p.getCollectionDate()) : null);
            ps.setString(6, p.getUnit());
            ps.setInt(7, p.getCollectorId());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM prevue_collection WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── MAPPER ───────────────────────────────────────────────────────────────
    private PrevueCollection map(ResultSet rs) throws SQLException {
        PrevueCollection p = new PrevueCollection();
        p.setId(rs.getInt("id"));
        p.setCollectorId(rs.getInt("collector_id"));
        p.setTypeCollection(rs.getString("type_collection"));
        p.setQuantite(rs.getDouble("quantite"));
        p.setStatut(rs.getString("statut"));
        int wId = rs.getInt("waste_collection_id");
        if (!rs.wasNull()) p.setWasteCollectionId(wId);
        
        Timestamp ts = rs.getTimestamp("collection_date");
        if (ts != null) p.setCollectionDate(ts.toLocalDateTime());
        p.setUnit(rs.getString("unit"));
        
        return p;
    }
}
