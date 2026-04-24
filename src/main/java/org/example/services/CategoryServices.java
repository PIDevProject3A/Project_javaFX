package org.example.services;

import org.example.entities.Category;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryServices implements Icrud<Category> {
    private final Connection con;

    public CategoryServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    private void ensureConnection() throws SQLException {
        if (con == null) {
            throw new SQLException("No database connection available.");
        }
    }

    @Override
    public void ajouter(Category category) throws SQLException {
        ensureConnection();
        String sql = "INSERT INTO category (name, description) VALUES ('"
                + category.getName() + "', '"
                + category.getDescription() + "')";
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        }
        System.out.println("Category cree avec succee ");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        ensureConnection();
        String sql = "DELETE FROM category WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        System.out.println("Category supprimee avec succee ");
    }

    @Override
    public List<Category> afficher() throws SQLException {
        ensureConnection();
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM category";
        try (Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public void modifier(Category category) throws SQLException {
        ensureConnection();
        String sql = "UPDATE category SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getId());
            ps.executeUpdate();
        }
        System.out.println("Category modifiee avec succee ");
    }
}
