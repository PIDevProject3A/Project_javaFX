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
    private Boolean reponseHasLikeColumn;
    private Boolean reponseHasDislikeColumn;

    public ReponseServices() {
        con = MyDataBase.getInstance().getConnection();
    }

    private void ensureConnection() throws SQLException {
        if (con == null) {
            throw new SQLException("No database connection available.");
        }
    }

    private boolean hasLikeColumn() {
        if (reponseHasLikeColumn != null) {
            return reponseHasLikeColumn;
        }
        try {
            java.sql.DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, TABLE, "like_count")) {
                reponseHasLikeColumn = rs.next();
            }
        } catch (SQLException e) {
            reponseHasLikeColumn = false;
        }
        return reponseHasLikeColumn;
    }

    private boolean hasDislikeColumn() {
        if (reponseHasDislikeColumn != null) {
            return reponseHasDislikeColumn;
        }
        try {
            java.sql.DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, TABLE, "dislike_count")) {
                reponseHasDislikeColumn = rs.next();
            }
        } catch (SQLException e) {
            reponseHasDislikeColumn = false;
        }
        return reponseHasDislikeColumn;
    }

    @Override
    public int ajouter(Reponse reponse) throws SQLException {
        ensureConnection();
        String sql = "INSERT INTO " + TABLE + " (content, topic_id, created_at, updated_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reponse.getContent());
            ps.setInt(2, reponse.getTopic_id());
            if (reponse.getCreated_at() != null) {
                ps.setTimestamp(3, new Timestamp(reponse.getCreated_at().getTime()));
            } else {
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            }
            if (reponse.getUpdated_at() != null) {
                ps.setTimestamp(4, new Timestamp(reponse.getUpdated_at().getTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating reply failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        return -1;
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
                r.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
                r.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
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
                    r.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
                    r.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
                    list.add(r);
                }
            }
        }
        return list;
    }

    public Reponse getById(int reponseId) throws SQLException {
        ensureConnection();
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reponseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
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
                r.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
                r.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
                return r;
            }
        }
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

    public void likeReponse(int reponseId) throws SQLException {
        ensureConnection();
        if (!hasLikeColumn()) {
            return;
        }
        String sql = "UPDATE " + TABLE + " SET like_count = COALESCE(like_count, 0) + 1 WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reponseId);
            ps.executeUpdate();
        }
    }

    public void dislikeReponse(int reponseId) throws SQLException {
        ensureConnection();
        if (!hasDislikeColumn()) {
            return;
        }
        String sql = "UPDATE " + TABLE + " SET dislike_count = COALESCE(dislike_count, 0) + 1 WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, reponseId);
            ps.executeUpdate();
        }
    }

    public boolean supportsReactions() throws SQLException {
        ensureConnection();
        return hasLikeColumn() && hasDislikeColumn();
    }
}
