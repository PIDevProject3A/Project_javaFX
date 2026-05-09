package com.esprit.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.esprit.entities.RecyclingBuyer;
import com.esprit.utils.MyDataBase;

public class RecyclingBuyerDao {
    private static final String SELECT_ALL_SQL = """
            SELECT id, user_id, buyer_type, gps_location, conditions,
                   contact_phone, contact_email, website
            FROM recycling_buyers
            ORDER BY id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO recycling_buyers (
                user_id, buyer_type, gps_location, conditions,
                contact_phone, contact_email, website
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE recycling_buyers
            SET user_id = ?,
                buyer_type = ?,
                gps_location = ?,
                conditions = ?,
                contact_phone = ?,
                contact_email = ?,
                website = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM recycling_buyers WHERE id = ?";

    public List<RecyclingBuyer> findAll() throws SQLException {
        List<RecyclingBuyer> result = new ArrayList<>();

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public RecyclingBuyer insert(RecyclingBuyer buyer) throws SQLException {
        RecyclingBuyerValidator.validateAndNormalizeForInsert(buyer);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            fillStatement(statement, buyer);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    buyer.setId(keys.getInt(1));
                }
            }

            return buyer;
        }
    }

    public void update(RecyclingBuyer buyer) throws SQLException {
        RecyclingBuyerValidator.validateAndNormalizeForUpdate(buyer);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            fillStatement(statement, buyer);
            statement.setInt(8, buyer.getId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Buyer ID is invalid.");
        }

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private RecyclingBuyer mapRow(ResultSet rs) throws SQLException {
        RecyclingBuyer b = new RecyclingBuyer(
            rs.getInt("id"),
            "Buyer #" + rs.getInt("id"), // Default name as it's no longer in DB
            rs.getString("buyer_type"),
            "See GPS", // Address placeholder
            "Unknown", // City placeholder
            0, 0, // Lat/Long placeholders (will be parsed from gps_location)
            rs.getString("contact_phone"),
            "Active", // Status placeholder
            null // Notes
        );
        b.setUserId(rs.getInt("user_id"));
        b.setBuyerType(rs.getString("buyer_type"));
        b.setGpsLocation(rs.getString("gps_location"));
        b.setConditions(rs.getString("conditions"));
        b.setContactEmail(rs.getString("contact_email"));
        b.setWebsite(rs.getString("website"));
        return b;
    }

    private void fillStatement(PreparedStatement statement, RecyclingBuyer b) throws SQLException {
        if (b.getUserId() != null) {
            statement.setInt(1, b.getUserId());
        } else {
            statement.setNull(1, java.sql.Types.INTEGER);
        }
        statement.setString(2, b.getBuyerType());
        statement.setString(3, b.getGpsLocation());
        statement.setString(4, b.getConditions());
        statement.setString(5, b.getContactPhone());
        statement.setString(6, b.getContactEmail());
        statement.setString(7, b.getWebsite());
    }
}
