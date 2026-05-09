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

import com.esprit.entities.EcoTransaction;
import com.esprit.utils.MyDataBase;

public class TransactionDao {
    private static final String SELECT_ALL_SQL = """
            SELECT id, amount, transaction_type, transaction_date,
                   source_user_id, target_user_id, payment_status
            FROM transactions
            ORDER BY transaction_date DESC, id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO transactions (
                amount, transaction_type, transaction_date,
                source_user_id, target_user_id, payment_status
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE transactions
            SET amount = ?,
                transaction_type = ?,
                transaction_date = ?,
                source_user_id = ?,
                target_user_id = ?,
                payment_status = ?
            WHERE id = ?
            """;

    private static final String DELETE_SQL = "DELETE FROM transactions WHERE id = ?";

    public List<EcoTransaction> findAll() throws SQLException {
        List<EcoTransaction> result = new ArrayList<>();

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }

        return result;
    }

    public EcoTransaction insert(EcoTransaction transaction) throws SQLException {
        TransactionValidator.validateAndNormalizeForInsert(transaction);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            fillStatement(statement, transaction);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setId(keys.getInt(1));
                }
            }

            return transaction;
        }
    }

    public void update(EcoTransaction transaction) throws SQLException {
        TransactionValidator.validateAndNormalizeForUpdate(transaction);

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            fillStatement(statement, transaction);
            statement.setInt(7, transaction.getId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("Transaction ID is invalid.");
        }

        try (Connection connection = MyDataBase.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private EcoTransaction mapRow(ResultSet rs) throws SQLException {
        EcoTransaction t = new EcoTransaction(
            rs.getInt("id"),
            null, // reference_code (no longer in DB)
            rs.getString("transaction_type"),
            null, // source_type
            null, // purpose
            rs.getDouble("amount"),
            null, // impact_unit
            null, // impact_quantity
            rs.getDate("transaction_date").toLocalDate(),
            null, // status (replaced by payment_status)
            null  // notes
        );
        t.setSourceUserId(toNullableInteger(rs, "source_user_id"));
        t.setTargetUserId(toNullableInteger(rs, "target_user_id"));
        t.setPaymentStatus(rs.getString("payment_status"));
        return t;
    }

    private Integer toNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void fillStatement(PreparedStatement statement, EcoTransaction t) throws SQLException {
        statement.setDouble(1, t.getAmount());
        statement.setString(2, t.getTransactionType());
        statement.setDate(3, Date.valueOf(t.getTransactionDate()));
        
        if (t.getSourceUserId() == null) {
            statement.setNull(4, Types.INTEGER);
        } else {
            statement.setInt(4, t.getSourceUserId());
        }

        if (t.getTargetUserId() == null) {
            statement.setNull(5, Types.INTEGER);
        } else {
            statement.setInt(5, t.getTargetUserId());
        }

        statement.setString(6, t.getPaymentStatus() != null ? t.getPaymentStatus() : "pending");
    }
}
