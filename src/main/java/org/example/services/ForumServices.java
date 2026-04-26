package org.example.services;

import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForumServices implements Icrud<Topic> {
    Connection con;
    private final ReponseServices reponseServices = new ReponseServices();
    private Boolean topicHasCategoryColumn;
    private Boolean topicHasLikeColumn;
    private Boolean topicHasDislikeColumn;
    private Boolean topicHasImageColumn;

    public ForumServices() {
        con = MyDataBase.getInstance().getConnection();
        }

    private boolean hasCategoryColumn() {
        if (topicHasCategoryColumn != null) {
            return topicHasCategoryColumn;
        }
        try {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "topic", "category")) {
                topicHasCategoryColumn = rs.next();
            }
        } catch (SQLException e) {
            topicHasCategoryColumn = false;
        }
        return topicHasCategoryColumn;
    }

    private boolean hasLikeColumn() {
        if (topicHasLikeColumn != null) {
            return topicHasLikeColumn;
        }
        try {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "topic", "like_count")) {
                topicHasLikeColumn = rs.next();
            }
        } catch (SQLException e) {
            topicHasLikeColumn = false;
        }
        return topicHasLikeColumn;
    }

    private boolean hasDislikeColumn() {
        if (topicHasDislikeColumn != null) {
            return topicHasDislikeColumn;
        }
        try {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "topic", "dislike_count")) {
                topicHasDislikeColumn = rs.next();
            }
        } catch (SQLException e) {
            topicHasDislikeColumn = false;
        }
        return topicHasDislikeColumn;
    }

    private boolean hasImageColumn() {
        if (topicHasImageColumn != null) {
            return topicHasImageColumn;
        }
        try {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "topic", "image_path")) {
                topicHasImageColumn = rs.next();
            }
        } catch (SQLException e) {
            topicHasImageColumn = false;
        }
        return topicHasImageColumn;
    }

    private void ensureConnection() throws SQLException {
        if (con == null) {
            throw new SQLException("No database connection available.");
        }
    }

    private int getReplyCountForTopic(int topicId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM forum_reponse WHERE topic_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static String sqlDateTime(Date d) {
        if (d == null) {
            return "NULL";
        }
        return "'" + new Timestamp(d.getTime()) + "'";
    }

    @Override
    public int ajouter(Topic topic) throws SQLException {
        ensureConnection();
        String sql = hasImageColumn()
                ? "INSERT INTO topic (title, content, status, category, created_at, like_count, dislike_count, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO topic (title, content, status, category, created_at, like_count, dislike_count) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, topic.getTitle());
            pstmt.setString(2, topic.getContent());
            pstmt.setString(3, topic.getStatus().getDbValue());
            pstmt.setString(4, topic.getCategory().getDbValue());
            pstmt.setTimestamp(5, new java.sql.Timestamp(topic.getCreated_at().getTime()));
            pstmt.setInt(6, 0);
            pstmt.setInt(7, 0);
            if (hasImageColumn()) {
                pstmt.setString(8, topic.getImagePath());
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating topic failed, no rows affected.");
            }

            // Récupérer l'ID généré
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating topic failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        ensureConnection();
        Topic topicToDelete = getTopicById(id);
        reponseServices.supprimerParTopic(id);
        String sql = "DELETE FROM topic WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Topic supprimer avec succee ");
        }
        if (topicToDelete != null && topicToDelete.getImagePath() != null && !topicToDelete.getImagePath().isBlank()) {
            try {
                Path imagePath = Path.of(topicToDelete.getImagePath());
                Path uploadsRoot = Path.of(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
                Path absoluteImage = imagePath.toAbsolutePath().normalize();
                if (absoluteImage.startsWith(uploadsRoot)) {
                    Files.deleteIfExists(absoluteImage);
                }
            } catch (Exception ignored) {
                // Keep topic deletion successful even if file cleanup fails.
            }
        }
    }

    @Override
    public List<Topic> afficher() throws SQLException {
        ensureConnection();
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT * FROM topic";
        try (Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Topic topic = new Topic();
                topic.setId(rs.getInt("id"));
                topic.setTitle(rs.getString("title"));
                topic.setContent(rs.getString("content"));
                topic.setStatus(TopicStatus.fromDb(rs.getString("status")));
                if (hasCategoryColumn()) {
                    topic.setCategory(TopicCategory.fromDb(rs.getString("category")));
                } else {
                    topic.setCategory(TopicCategory.FEEDBACK);
                }
                Timestamp ca = rs.getTimestamp("created_at");
                if (ca != null) {
                    topic.setCreated_at(new Date(ca.getTime()));
                }
                Timestamp ua = rs.getTimestamp("updated_at");
                if (ua != null) {
                    topic.setUpdated_at(new Date(ua.getTime()));
                }
                topic.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
                topic.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
                topic.setReplyCount(getReplyCountForTopic(topic.getId()));
                topic.setImagePath(hasImageColumn() ? rs.getString("image_path") : null);
                topics.add(topic);
            }
        }
        return topics;
    }

    public Topic getTopicById(int topicId) throws SQLException {
        ensureConnection();
        String sql = "SELECT * FROM topic WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Topic topic = new Topic();
                topic.setId(rs.getInt("id"));
                topic.setTitle(rs.getString("title"));
                topic.setContent(rs.getString("content"));
                topic.setStatus(TopicStatus.fromDb(rs.getString("status")));
                if (hasCategoryColumn()) {
                    topic.setCategory(TopicCategory.fromDb(rs.getString("category")));
                } else {
                    topic.setCategory(TopicCategory.FEEDBACK);
                }
                Timestamp ca = rs.getTimestamp("created_at");
                if (ca != null) {
                    topic.setCreated_at(new Date(ca.getTime()));
                }
                Timestamp ua = rs.getTimestamp("updated_at");
                if (ua != null) {
                    topic.setUpdated_at(new Date(ua.getTime()));
                }
                topic.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
                topic.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
                topic.setReplyCount(getReplyCountForTopic(topic.getId()));
                topic.setImagePath(hasImageColumn() ? rs.getString("image_path") : null);
                return topic;
            }
        }
    }

    public Topic getMostLikedTopic() throws SQLException {
        ensureConnection();
        String sql;
        if (hasLikeColumn()) {
            sql = "SELECT * FROM topic ORDER BY COALESCE(like_count, 0) DESC, id ASC LIMIT 1";
        } else {
            sql = "SELECT * FROM topic ORDER BY id ASC LIMIT 1";
        }
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Topic topic = new Topic();
            topic.setId(rs.getInt("id"));
            topic.setTitle(rs.getString("title"));
            topic.setContent(rs.getString("content"));
            topic.setStatus(TopicStatus.fromDb(rs.getString("status")));
            if (hasCategoryColumn()) {
                topic.setCategory(TopicCategory.fromDb(rs.getString("category")));
            } else {
                topic.setCategory(TopicCategory.FEEDBACK);
            }
            Timestamp ca = rs.getTimestamp("created_at");
            if (ca != null) {
                topic.setCreated_at(new Date(ca.getTime()));
            }
            Timestamp ua = rs.getTimestamp("updated_at");
            if (ua != null) {
                topic.setUpdated_at(new Date(ua.getTime()));
            }
            topic.setLikeCount(hasLikeColumn() ? rs.getInt("like_count") : 0);
            topic.setDislikeCount(hasDislikeColumn() ? rs.getInt("dislike_count") : 0);
            topic.setReplyCount(getReplyCountForTopic(topic.getId()));
            topic.setImagePath(hasImageColumn() ? rs.getString("image_path") : null);
            return topic;
        }
    }

    @Override
    public void modifier(Topic topic) throws SQLException {
        ensureConnection();
        String sql = hasCategoryColumn()
                ? "UPDATE topic SET title = ?, content = ?, status = ?, category = ?, updated_at = ? WHERE id = ?"
                : "UPDATE topic SET title = ?, content = ?, status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, topic.getTitle());
            ps.setString(2, topic.getContent());
            ps.setString(3, topic.getStatus().getDbValue());
            int updatedIdx;
            int idIdx;
            if (hasCategoryColumn()) {
                ps.setString(4, topic.getCategory().getDbValue());
                updatedIdx = 5;
                idIdx = 6;
            } else {
                updatedIdx = 4;
                idIdx = 5;
            }
            if (topic.getUpdated_at() != null) {
                ps.setTimestamp(updatedIdx, new Timestamp(topic.getUpdated_at().getTime()));
            } else {
                ps.setTimestamp(updatedIdx, new Timestamp(System.currentTimeMillis()));
            }
            ps.setInt(idIdx, topic.getId());
            ps.executeUpdate();
        }
    }

    public void likeTopic(int topicId) throws SQLException {
        ensureConnection();
        if (!hasLikeColumn()) {
            return;
        }
        String sql = "UPDATE topic SET like_count = COALESCE(like_count, 0) + 1 WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            ps.executeUpdate();
        }
    }

    public void dislikeTopic(int topicId) throws SQLException {
        ensureConnection();
        if (!hasDislikeColumn()) {
            return;
        }
        String sql = "UPDATE topic SET dislike_count = COALESCE(dislike_count, 0) + 1 WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            ps.executeUpdate();
        }
    }

    public boolean supportsReactions() throws SQLException {
        ensureConnection();
        return hasLikeColumn() && hasDislikeColumn();
    }
}
