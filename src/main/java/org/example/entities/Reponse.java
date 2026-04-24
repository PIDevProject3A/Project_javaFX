package org.example.entities;

import java.util.Date;

public class Reponse {
    private int id;
    private String Content;
    private int Topic_id;
    private Date created_at;
    private Date updated_at;

    public Reponse() {}

    public Reponse(String content, int topic_id, Date created_at, Date updated_at) {
        Content = content;
        Topic_id = topic_id;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return Content;
    }

    public int getTopic_id() {
        return Topic_id;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setContent(String content) {
        Content = content;
    }

    public void setTopic_id(int topic_id) {
        Topic_id = topic_id;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }

    @Override
    public String toString() {
        return "Reponse{" +
                "id=" + id +
                ", Content='" + Content + '\'' +
                ", Topic_id=" + Topic_id +
                ", created_at=" + created_at +
                ", updated_at=" + updated_at +
                '}';
    }
}
