package org.example.services;

import org.example.entities.Reponse;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReponseServices implements Icrud<Reponse> {
    /** Nom réel de la table dans MySQL */
    private static final String TABLE = "forum_reponse";

    private final Connection con;

    public ReponseServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    private void ensureConnection() throws SQLException {
        if (con == null) {
            throw new SQLException("No database connection available.");
        }
    }

    /** Pour INSERT en concaténation : date SQL ou NULL */
    private static String sqlDateTime(Date d) {
        if (d == null) {
            return "NULL";
        }
        return "'" + new Timestamp(d.getTime()) + "'";
    }

    private static String escapeContent(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "''");
    }

    @Override
    public void ajouter(Reponse reponse) throws SQLException {
        ensureConnection();
        String created = reponse.getCreated_at() != null ? sqlDateTime(reponse.getCreated_at()) : "NOW()";
        String updated = reponse.getUpdated_at() != null ? sqlDateTime(reponse.getUpdated_at()) : "NULL";
        String sql = "INSERT INTO " + TABLE + " (content, topic_id, created_at, updated_at) VALUES ('"
                + escapeContent(reponse.getContent()) + "', "
                + reponse.getTopic_id() + ", "
                + created + ", "
                + updated + ")";
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        }
        System.out.println("Reponse cree avec succee ");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        ensureConnection();
        String sql = "DELETE FROM " + TABLE + " WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        System.out.println("Reponse supprimee avec succee ");
    }

    /** Supprime toutes les réponses liées à un sujet (avant suppression du topic). */
    public void supprimerParTopic(int topicId) throws SQLException {
        ensureConnection();
        String sql = "DELETE FROM " + TABLE + " WHERE topic_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Reponse> afficher() throws SQLException {
        ensureConnection();
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE;
        try (Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Reponse r = new Reponse();
                r.setId(rs.getInt("id"));
                r.setContent(rs.getString("content"));
                r.setTopic_id(rs.getInt("topic_id"));
                Timestamp ca = rs.getTimestamp("created_at");
                if (ca != null) {
                    r.setCreated_at(new Date(ca.getTime()));
                }
                Timestamp ua = rs.getTimestamp("updated_at");
                if (ua != null) {
                    r.setUpdated_at(new Date(ua.getTime()));
                }
                list.add(r);
            }
        }
        return list;
    }

    public List<Reponse> afficherParTopic(int topicId) throws SQLException {
        ensureConnection();
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE + " WHERE topic_id = ? ORDER BY created_at ASC, id ASC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reponse r = new Reponse();
                    r.setId(rs.getInt("id"));
                    r.setContent(rs.getString("content"));
                    r.setTopic_id(rs.getInt("topic_id"));
                    Timestamp ca = rs.getTimestamp("created_at");
                    if (ca != null) {
                        r.setCreated_at(new Date(ca.getTime()));
                    }
                    Timestamp ua = rs.getTimestamp("updated_at");
                    if (ua != null) {
                        r.setUpdated_at(new Date(ua.getTime()));
                    }
                    list.add(r);
                }
            }
        }
        return list;
    }

    @Override
    public void modifier(Reponse reponse) throws SQLException {
        ensureConnection();
        String sql = "UPDATE " + TABLE + " SET content = ?, topic_id = ?, created_at = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reponse.getContent());
            ps.setInt(2, reponse.getTopic_id());
            if (reponse.getCreated_at() != null) {
                ps.setTimestamp(3, new Timestamp(reponse.getCreated_at().getTime()));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (reponse.getUpdated_at() != null) {
                ps.setTimestamp(4, new Timestamp(reponse.getUpdated_at().getTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(5, reponse.getId());
            ps.executeUpdate();
        }
        System.out.println("Reponse modifiee avec succee ");
    }
}
