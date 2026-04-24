package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.services.ForumServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AfficherTopic implements Initializable {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.ENGLISH);
    private static final String FILTER_ALL = "All statuses";

    @FXML
    private ScrollPane feedScroll;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Label lblResultCount;
    @FXML
    private VBox topicsFeedBox;

    private final ForumServices forumServices = new ForumServices();
    /** Local copy for search / filter without hitting the database on every keystroke. */
    private final List<Topic> sourceTopics = new ArrayList<>();
    private boolean reactionsWarningShown;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> statuts = FXCollections.observableArrayList(FILTER_ALL);
        for (TopicStatus ts : TopicStatus.values()) {
            statuts.add(ts.getDisplayLabel());
        }
        statusFilter.setItems(statuts);
        statusFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((obs, oldV, newV) -> appliquerFiltres());
        statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> appliquerFiltres());

        chargerListe();
    }

    private static String statutDepuisLibelleCombo(String libelle) {
        if (libelle == null || FILTER_ALL.equals(libelle)) {
            return null;
        }
        TopicStatus ts = TopicStatus.fromDisplayLabel(libelle);
        return ts != null ? ts.getDbValue() : null;
    }

    private Stage ownerStage() {
        return (Stage) feedScroll.getScene().getWindow();
    }

    private void setSceneOnCurrentStage(Parent root, String title) {
        Stage stage = ownerStage();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.setMinWidth(920);
        stage.setMinHeight(620);
    }

    private void reactToTopic(Topic topic, boolean like) {
        try {
            if (like) {
                forumServices.likeTopic(topic.getId());
                topic.setLikeCount(topic.getLikeCount() + 1);
            } else {
                forumServices.dislikeTopic(topic.getId());
                topic.setDislikeCount(topic.getDislikeCount() + 1);
            }
            appliquerFiltres();
            if (!reactionsWarningShown && !forumServices.supportsReactions()) {
                reactionsWarningShown = true;
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Info");
                info.setHeaderText("Database reactions columns are missing");
                info.setContentText("UI counters work, but to persist counts add columns like_count and dislike_count in table topic.");
                info.showAndWait();
            }
        } catch (SQLException ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Error");
            err.setHeaderText("Could not save reaction");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    private void openDetailsWindow(Topic topic) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/DetailTopic.fxml")));
            Parent root = loader.load();
            DetailTopicController ctrl = loader.getController();
            ctrl.setTopic(topic);
            setSceneOnCurrentStage(root, "Topic details");
        } catch (IOException ex) {
            showIoError(ex);
        }
    }

    private void openEditWindow(Topic topic) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/ModifierTopic.fxml")));
            Parent root = loader.load();
            ModifierTopicController ctrl = loader.getController();
            ctrl.setTopic(cloneTopic(topic));
            ctrl.setOnSaved(this::chargerListe);
            setSceneOnCurrentStage(root, "Edit topic");
        } catch (IOException ex) {
            showIoError(ex);
        }
    }

    private static Topic cloneTopic(Topic src) {
        Topic t = new Topic();
        t.setId(src.getId());
        t.setTitle(src.getTitle());
        t.setContent(src.getContent());
        t.setStatus(src.getStatus());
        t.setCategory(src.getCategory() != null ? src.getCategory() : TopicCategory.FEEDBACK);
        t.setCreated_at(src.getCreated_at());
        t.setUpdated_at(src.getUpdated_at());
        return t;
    }

    private void openRepliesWindow(Topic topic) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/AfficherReponse.fxml")));
            Parent root = loader.load();
            AfficherReponseController ctrl = loader.getController();
            ctrl.initForTopic(topic);
            setSceneOnCurrentStage(root, "Replies - " + (topic.getTitle() != null ? topic.getTitle() : "topic"));
        } catch (IOException ex) {
            showIoError(ex);
        }
    }

    private void openDeleteWindow(Topic topic) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/SupprimerTopic.fxml")));
            Parent root = loader.load();
            SupprimerTopicController ctrl = loader.getController();
            ctrl.setTopic(topic);
            ctrl.setOnDeleted(this::chargerListe);
            setSceneOnCurrentStage(root, "Delete topic");
        } catch (IOException ex) {
            showIoError(ex);
        }
    }

    private void showIoError(IOException ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Could not open this window");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String formatDate(Date d) {
        if (d == null) {
            return "—";
        }
        synchronized (DATE_FMT) {
            return DATE_FMT.format(d);
        }
    }

    @FXML
    void goToAdd(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/AjouterTopic.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(560);
        stage.setTitle("New topic");
    }

    @FXML
    void refresh() {
        chargerListe();
    }

    @FXML
    void resetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        appliquerFiltres();
    }

    private void chargerListe() {
        try {
            sourceTopics.clear();
            sourceTopics.addAll(forumServices.afficher());
            appliquerFiltres();
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load topics");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void appliquerFiltres() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String statutFiltre = statutDepuisLibelleCombo(statusFilter.getSelectionModel().getSelectedItem());

        List<Topic> filtered = sourceTopics.stream()
                .filter(t -> matchesSearch(t, q))
                .filter(t -> matchesStatut(t, statutFiltre))
                .collect(Collectors.toList());

        renderFeed(filtered);
        int n = filtered.size();
        int total = sourceTopics.size();
        lblResultCount.setText(n + " shown · " + total + " total");
    }

    private void renderFeed(List<Topic> topics) {
        topicsFeedBox.getChildren().clear();
        for (Topic t : topics) {
            topicsFeedBox.getChildren().add(buildTopicCard(t));
        }
    }

    private VBox buildTopicCard(Topic topic) {
        VBox card = new VBox(12);
        card.getStyleClass().add("topic-card");

        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        Label categoryBadge = new Label(topic.getCategory() != null ? topic.getCategory().getDisplayLabel() : "Feedback");
        categoryBadge.getStyleClass().add("topic-tag-category");
        Label statusBadge = new Label(topic.getStatus() != null ? topic.getStatus().getDisplayLabel() : "Unknown");
        statusBadge.getStyleClass().add("topic-tag-status");
        Label meta = new Label("posted " + formatDate(topic.getCreated_at()));
        meta.getStyleClass().add("topic-meta");
        tags.getChildren().addAll(categoryBadge, statusBadge, meta);

        Label title = new Label(nullToEmpty(topic.getTitle()));
        title.getStyleClass().add("topic-card-title");
        title.setWrapText(true);

        Label content = new Label(truncate(topic.getContent(), 220));
        content.getStyleClass().add("topic-card-content");
        content.setWrapText(true);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnLike = new Button("👍 " + topic.getLikeCount());
        btnLike.getStyleClass().add("btn-reaction-like");
        btnLike.setOnAction(e -> reactToTopic(topic, true));
        Button btnDislike = new Button("👎 " + topic.getDislikeCount());
        btnDislike.getStyleClass().add("btn-reaction-dislike");
        btnDislike.setOnAction(e -> reactToTopic(topic, false));

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        FlowPane crudButtons = new FlowPane(8, 8);
        crudButtons.setAlignment(Pos.CENTER_RIGHT);
        Button btnDetails = new Button("View");
        Button btnEdit = new Button("Edit");
        Button btnDelete = new Button("Delete");
        Button btnReply = new Button("Reply");
        btnDetails.getStyleClass().add("btn-feed-action");
        btnEdit.getStyleClass().add("btn-feed-action");
        btnDelete.getStyleClass().add("btn-feed-action");
        btnReply.getStyleClass().add("btn-feed-action");
        btnDetails.setOnAction(e -> openDetailsWindow(topic));
        btnEdit.setOnAction(e -> openEditWindow(topic));
        btnDelete.setOnAction(e -> openDeleteWindow(topic));
        btnReply.setOnAction(e -> openRepliesWindow(topic));
        crudButtons.getChildren().addAll(btnDetails, btnEdit, btnDelete, btnReply);

        footer.getChildren().addAll(btnLike, btnDislike, spacer, crudButtons);

        card.getChildren().addAll(tags, title, content, footer);
        return card;
    }

    private static boolean matchesSearch(Topic t, String q) {
        if (q.isEmpty()) {
            return true;
        }
        String title = nullToEmpty(t.getTitle()).toLowerCase(Locale.ROOT);
        String content = nullToEmpty(t.getContent()).toLowerCase(Locale.ROOT);
        String category = t.getCategory() != null ? t.getCategory().getDisplayLabel().toLowerCase(Locale.ROOT) : "";
        return title.contains(q) || content.contains(q) || category.contains(q);
    }

    private static boolean matchesStatut(Topic t, String statutDbAttendu) {
        if (statutDbAttendu == null) {
            return true;
        }
        return t.getStatus() != null && statutDbAttendu.equalsIgnoreCase(t.getStatus().getDbValue());
    }
}
