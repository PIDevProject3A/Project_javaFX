package services;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import entities.Donation;
import com.esprit.utils.MyDataBase;

public class DonationDao {
    private static final String SELECT_ALL_SQL = """
            SELECT id, donor_name, donation_type, amount, payment_method, donation_date, status, notes, tree_count
            FROM donations
            ORDER BY donation_date DESC, id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO donations (
                donor_name, donation_type, amount, payment_method, donation_date, status, notes, tree_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE donations
            SET donor_name = ?,
                donation_type = ?,
                amount = ?,
                payment_method = ?,
                donation_date = ?,
                status = ?,
                notes = ?,
                tree_count = ?
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
            statement.setInt(9, donation.getId());
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
        return new Donation(
            rs.getInt("id"),
            rs.getString("donor_name"),
            rs.getString("donation_type"),
            rs.getDouble("amount"),
            rs.getString("payment_method"),
            rs.getDate("donation_date").toLocalDate(),
            rs.getString("status"),
            rs.getString("notes"),
            toNullableInteger(rs, "tree_count")
        );
    }

    private Integer toNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void fillStatement(PreparedStatement statement, Donation donation) throws SQLException {
        statement.setString(1, donation.getDonorName());
        statement.setString(2, donation.getDonationType());
        statement.setDouble(3, donation.getAmount());
        statement.setString(4, donation.getPaymentMethod());
        statement.setDate(5, Date.valueOf(donation.getDonationDate()));
        statement.setString(6, donation.getStatus());
        statement.setString(7, donation.getNotes());

        if (donation.getTreeCount() == null) {
            statement.setNull(8, Types.INTEGER);
        } else {
            statement.setInt(8, donation.getTreeCount());
        }
    }
}
