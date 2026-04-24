package org.example.entities;



import java.util.Date;



public class Topic {

    private int id;

    private String title;

    private String Content;

    private TopicStatus status = TopicStatus.PENDING;
    private TopicCategory category = TopicCategory.FEEDBACK;

    private Date created_at;

    private Date updated_at;
    private int likeCount;
    private int dislikeCount;



    public Topic(String title, String Content, TopicStatus status) {

        this.title = title;

        this.Content = Content;

        this.status = status != null ? status : TopicStatus.PENDING;
        this.category = TopicCategory.FEEDBACK;

    }



    /** Compatibilité : chaîne (tests console {@code Main} ou ancienne base). */

    public Topic(String title, String Content, String status) {

        this(title, Content, TopicStatus.fromDb(status));

    }



    public Topic(String title, String Content, TopicStatus status, Date created_at, Date updated_at) {

        this.title = title;

        this.Content = Content;

        this.status = status != null ? status : TopicStatus.PENDING;
        this.category = TopicCategory.FEEDBACK;

        this.created_at = created_at;

        this.updated_at = updated_at;

    }



    public Topic() {

    }

    public Topic(String title, String Content, TopicStatus status, TopicCategory category, Date created_at, Date updated_at) {
        this.title = title;
        this.Content = Content;
        this.status = status != null ? status : TopicStatus.PENDING;
        this.category = category != null ? category : TopicCategory.FEEDBACK;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }



    public int getId() {

        return id;

    }



    public String getTitle() {

        return title;

    }



    public String getContent() {

        return Content;

    }



    public TopicStatus getStatus() {

        return status;

    }

    public TopicCategory getCategory() {
        return category;
    }



    public Date getCreated_at() {

        return created_at;

    }



    public Date getUpdated_at() {

        return updated_at;

    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }



    public void setId(int id) {

        this.id = id;

    }



    public void setTitle(String title) {

        this.title = title;

    }



    public void setContent(String content) {

        Content = content;

    }



    public void setStatus(TopicStatus status) {

        this.status = status != null ? status : TopicStatus.PENDING;

    }

    public void setCategory(TopicCategory category) {
        this.category = category != null ? category : TopicCategory.FEEDBACK;
    }



    public void setCreated_at(Date created_at) {

        this.created_at = created_at;

    }



    public void setUpdated_at(Date updated_at) {

        this.updated_at = updated_at;

    }

    public void setLikeCount(int likeCount) {
        this.likeCount = Math.max(0, likeCount);
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = Math.max(0, dislikeCount);
    }



    @Override

    public String toString() {

        return "Topic{" +

                "id=" + id +

                ", title='" + title + '\'' +

                ", Content='" + Content + '\'' +

                ", status=" + status +
                ", category=" + category +

                ", created_at=" + created_at +

                ", updated_at=" + updated_at +
                ", likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +

                '}';

    }

}

