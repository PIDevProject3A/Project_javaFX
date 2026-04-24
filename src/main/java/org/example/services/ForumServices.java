package org.example.services;

import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForumServices implements Icrud<Topic> {
    Connection con;
    private final ReponseServices reponseServices = new ReponseServices();
    private Boolean topicHasCategoryColumn;
    private Boolean topicHasLikeColumn;
    private Boolean topicHasDislikeColumn;

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

    private void ensureConnection() throws SQLException {
        if (con == null) {
            throw new SQLException("No database connection available.");
        }
    }

    private static String sqlDateTime(Date d) {
        if (d == null) {
            return "NULL";
        }
        return "'" + new Timestamp(d.getTime()) + "'";
    }

    @Override
    public void ajouter(Topic topic) throws SQLException {
        ensureConnection();
        String created = topic.getCreated_at() != null ? sqlDateTime(topic.getCreated_at()) : "NOW()";
        String updated = topic.getUpdated_at() != null ? sqlDateTime(topic.getUpdated_at()) : "NULL";
        String sql;
        if (hasCategoryColumn()) {
            sql = "INSERT INTO topic (title, content, status, category, created_at, updated_at) VALUES ('"
                    + topic.getTitle() + "', '"
                    + topic.getContent() + "', '"
                    + topic.getStatus().getDbValue() + "', '"
                    + topic.getCategory().getDbValue() + "', "
                    + created + ", "
                    + updated + ")";
        } else {
            sql = "INSERT INTO topic (title, content, status, created_at, updated_at) VALUES ('"
                    + topic.getTitle() + "', '"
                    + topic.getContent() + "', '"
                    + topic.getStatus().getDbValue() + "', "
                    + created + ", "
                    + updated + ")";
        }

        try (Statement statement = con.createStatement()) {
            statement.executeUpdate(sql);
        }
        System.out.println("Topic cree avec succee ");
    }


    @Override
    public void supprimer(int id) throws SQLException {
        ensureConnection();
        reponseServices.supprimerParTopic(id);
        String sql = "DELETE FROM topic WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Topic supprimer avec succee ");
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
                topics.add(topic);
            }
        }
        return topics;
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
