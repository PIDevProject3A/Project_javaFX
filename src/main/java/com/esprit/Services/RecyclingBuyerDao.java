package services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.RecyclingBuyer;
import com.esprit.utils.MyDataBase;

public class RecyclingBuyerDao {
    private static final String SELECT_ALL_SQL = """
            SELECT id, buyer_name, recycling_type, address, city, latitude, longitude,
                   contact_phone, status, notes
            FROM recycling_buyers
            ORDER BY city ASC, buyer_name ASC, id DESC
            """;

    private static final String INSERT_SQL = """
            INSERT INTO recycling_buyers (
                buyer_name, recycling_type, address, city, latitude, longitude,
                contact_phone, status, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE recycling_buyers
            SET buyer_name = ?,
                recycling_type = ?,
                address = ?,
                city = ?,
                latitude = ?,
                longitude = ?,
                contact_phone = ?,
                status = ?,
                notes = ?
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
            statement.setInt(10, buyer.getId());
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
        return new RecyclingBuyer(
            rs.getInt("id"),
            rs.getString("buyer_name"),
            rs.getString("recycling_type"),
            rs.getString("address"),
            rs.getString("city"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude"),
            rs.getString("contact_phone"),
            rs.getString("status"),
            rs.getString("notes")
        );
    }

    private void fillStatement(PreparedStatement statement, RecyclingBuyer buyer) throws SQLException {
        statement.setString(1, buyer.getBuyerName());
        statement.setString(2, buyer.getRecyclingType());
        statement.setString(3, buyer.getAddress());
        statement.setString(4, buyer.getCity());
        statement.setDouble(5, buyer.getLatitude());
        statement.setDouble(6, buyer.getLongitude());
        statement.setString(7, buyer.getContactPhone());
        statement.setString(8, buyer.getStatus());
        statement.setString(9, buyer.getNotes());
    }
}
