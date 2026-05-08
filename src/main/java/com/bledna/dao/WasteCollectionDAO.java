package com.bledna.dao;

import com.bledna.db.DatabaseConnection;
import com.bledna.model.WasteCollection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class WasteCollectionDAO {

    // ── READ ALL ─────────────────────────────────────────────────────────────
    public List<WasteCollection> getAll() throws SQLException {
        List<WasteCollection> list = new ArrayList<>();
        String sql = "SELECT * FROM waste_collection ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<WasteCollection> getByCollector(int collectorId) throws SQLException {
        List<WasteCollection> list = new ArrayList<>();
        String sql = "SELECT * FROM waste_collection WHERE collector_id = ? ORDER BY created_at DESC";
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
    public void insert(WasteCollection w) throws SQLException {
        String sql = """
            INSERT INTO waste_collection
              (collector_id, location_name, waste_type, quantity,
               collection_date, gps_location, status, description,
               created_at, updated_at, unit, image_path)
            VALUES (?,?,?,?,?,?,?,?,NOW(),NOW(),?,?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, w.getCollectorId());
            ps.setString(2, w.getLocationName());
            ps.setString(3, w.getWasteType() != null ? w.getWasteType().toLowerCase() : null);
            ps.setDouble(4, w.getQuantity());
            ps.setTimestamp(5, w.getCollectionDate() != null
                    ? Timestamp.valueOf(w.getCollectionDate()) : null);
            ps.setString(6, w.getGpsLocation());
            ps.setString(7, w.getStatus());
            ps.setString(8, w.getDescription());
            ps.setString(9, w.getUnit());
            ps.setString(10, w.getImagePath());
            ps.executeUpdate();
        }
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public void update(WasteCollection w) throws SQLException {
        String sql = """
            UPDATE waste_collection
               SET location_name=?, waste_type=?, quantity=?,
                   collection_date=?, gps_location=?, status=?,
                    description=?, updated_at=NOW(), unit=?, image_path=?
             WHERE id=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, w.getLocationName());
            ps.setString(2, w.getWasteType() != null ? w.getWasteType().toLowerCase() : null);
            ps.setDouble(3, w.getQuantity());
            ps.setTimestamp(4, w.getCollectionDate() != null
                    ? Timestamp.valueOf(w.getCollectionDate()) : null);
            ps.setString(5, w.getGpsLocation());
            ps.setString(6, w.getStatus());
            ps.setString(7, w.getDescription());
            ps.setString(8, w.getUnit());
            ps.setString(9, w.getImagePath());
            ps.setInt(10, w.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    // FK on prevue_collection is ON DELETE SET NULL — DB handles it automatically
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM waste_collection WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── MAPPER ───────────────────────────────────────────────────────────────
    private WasteCollection map(ResultSet rs) throws SQLException {
        WasteCollection w = new WasteCollection();
        w.setId(rs.getInt("id"));
        w.setCollectorId(rs.getInt("collector_id"));
        w.setLocationName(rs.getString("location_name"));
        w.setWasteType(rs.getString("waste_type"));
        w.setQuantity(rs.getDouble("quantity"));

        Timestamp ts = rs.getTimestamp("collection_date");
        if (ts != null) w.setCollectionDate(ts.toLocalDateTime());

        w.setGpsLocation(rs.getString("gps_location"));
        w.setStatus(rs.getString("status"));
        w.setDescription(rs.getString("description"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) w.setCreatedAt(cat.toLocalDateTime());

        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) w.setUpdatedAt(uat.toLocalDateTime());

        w.setUnit(rs.getString("unit"));
        w.setImagePath(rs.getString("image_path"));
        return w;
    }
}
