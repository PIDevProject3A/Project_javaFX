package com.esprit.services;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.esprit.entities.Donation;
import com.esprit.utils.MyDataBase;

public class DonationDao {
    private static final String SELECT_ALL_SQL = """
            SELECT id, user_id, amount, donation_type, donation_date, transaction_status
            FROM donations
            ORDER BY donation_date DESC, id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO donations (
                user_id, amount, donation_type, donation_date, transaction_status
            ) VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE donations
            SET user_id = ?,
                amount = ?,
                donation_type = ?,
                donation_date = ?,
                transaction_status = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM donations WHERE id = ?";

    public List<Donation> findAll() throws SQLException {
        List<Donation> result = new ArrayList<>();

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public Donation insert(Donation donation) throws SQLException {
        DonationValidator.validateAndNormalizeForInsert(donation);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            fillStatement(statement, donation);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    donation.setId(keys.getInt(1));
                }
            }

            return donation;
        }
    }

    public void update(Donation donation) throws SQLException {
        DonationValidator.validateAndNormalizeForUpdate(donation);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            fillStatement(statement, donation);
            statement.setInt(6, donation.getId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Donation ID is invalid.");
        }

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Donation mapRow(ResultSet rs) throws SQLException {
        Donation d = new Donation(
            rs.getInt("id"),
            "User #" + rs.getInt("user_id"), // Placeholder for donor name
            rs.getString("donation_type"),
            rs.getDouble("amount"),
            "N/A", // payment_method (no longer in DB)
            rs.getTimestamp("donation_date").toLocalDateTime().toLocalDate(),
            rs.getString("transaction_status"), // Map status to transaction_status
            null, // notes
            null  // tree_count
        );
        d.setUserId(rs.getInt("user_id"));
        d.setTransactionStatus(rs.getString("transaction_status"));
        return d;
    }

    private Integer toNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void fillStatement(PreparedStatement statement, Donation d) throws SQLException {
        if (d.getUserId() != null) {
            statement.setInt(1, d.getUserId());
        } else {
            statement.setInt(1, 1); // Default to admin user
        }
        statement.setDouble(2, d.getAmount());
        statement.setString(3, d.getDonationType());
        statement.setTimestamp(4, java.sql.Timestamp.valueOf(d.getDonationDate().atStartOfDay()));
        statement.setString(5, d.getTransactionStatus() != null ? d.getTransactionStatus() : "Pending");
    }
}
