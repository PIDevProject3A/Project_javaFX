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
            SELECT id, reference_code, transaction_type, source_type, purpose, amount,
                   impact_unit, impact_quantity, transaction_date, status, notes
            FROM transactions
            ORDER BY transaction_date DESC, id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO transactions (
                reference_code, transaction_type, source_type, purpose, amount,
                impact_unit, impact_quantity, transaction_date, status, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE transactions
            SET reference_code = ?,
                transaction_type = ?,
                source_type = ?,
                purpose = ?,
                amount = ?,
                impact_unit = ?,
                impact_quantity = ?,
                transaction_date = ?,
                status = ?,
                notes = ?
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
            statement.setInt(11, transaction.getId());
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
        return new EcoTransaction(
            rs.getInt("id"),
            rs.getString("reference_code"),
            rs.getString("transaction_type"),
            rs.getString("source_type"),
            rs.getString("purpose"),
            rs.getDouble("amount"),
            rs.getString("impact_unit"),
            toNullableInteger(rs, "impact_quantity"),
            rs.getDate("transaction_date").toLocalDate(),
            rs.getString("status"),
            rs.getString("notes")
        );
    }

    private Integer toNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void fillStatement(PreparedStatement statement, EcoTransaction transaction) throws SQLException {
        statement.setString(1, transaction.getReferenceCode());
        statement.setString(2, transaction.getTransactionType());
        statement.setString(3, transaction.getSourceType());
        statement.setString(4, transaction.getPurpose());
        statement.setDouble(5, transaction.getAmount());
        statement.setString(6, transaction.getImpactUnit());

        if (transaction.getImpactQuantity() == null) {
            statement.setNull(7, Types.INTEGER);
        } else {
            statement.setInt(7, transaction.getImpactQuantity());
        }

        statement.setDate(8, Date.valueOf(transaction.getTransactionDate()));
        statement.setString(9, transaction.getStatus());
        statement.setString(10, transaction.getNotes());
    }
}
