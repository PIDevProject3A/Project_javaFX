package controller;

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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.ListChangeListener;
import org.example.entities.Notification;
import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.services.ForumServices;
import org.example.services.NotificationService;

import java.awt.Desktop;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

public class AfficherTopic implements Initializable {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.ENGLISH);
    private static final String FILTER_ALL = "All statuses";
    private static final String SORT_LATEST = "Latest";
    private static final String SORT_MOST_LIKED = "Most liked";
    private static final String SORT_MOST_DISCUSSED = "Most discussed";
    public static final ObservableList<Notification> notificationsGlobal = NotificationService.getInstance().getNotifications();
    private static final Set<Integer> savedTopicIds = new HashSet<>();
    private static final java.util.Map<Integer, String> topicReactions = new java.util.HashMap<>();

    @FXML
    private ScrollPane feedScroll;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> sortFilter;
    @FXML
    private Label lblResultCount;
    @FXML
    private VBox topicsFeedBox;
    @FXML
    private ListView<Notification> notificationList;
    @FXML
    private Label notifBadge;
    @FXML
    private Label lblEngagement;
    @FXML
    private Button btnAllTopics;
    @FXML
    private Button btnSavedOnly;

    private final ForumServices forumServices = new ForumServices();
    private final NotificationService notificationService = NotificationService.getInstance();
    private final List<Topic> sourceTopics = new ArrayList<>();
    private Integer pinnedTopicId;
    private boolean savedOnlyMode;

    private void updateNotifBadge() {
        if (notifBadge != null) {
            int count = notificationsGlobal.size();
            notifBadge.setText(String.valueOf(count));
            notifBadge.setVisible(count > 0);
        }
    }

    @FXML
    void showNotificationsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/styles/NotificationsWindow.fxml")));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Notifications");
            stage.setScene(new Scene(root));
            stage.setMinWidth(480);
            stage.setMinHeight(520);
            stage.show();
        } catch (IOException ex) {
            showIoError(ex);
        }
    }

    @FXML
    void clearNotifications() {
        notificationsGlobal.clear();
        updateNotifBadge();
    }

    @FXML
    void testNotif() {
        String message = "🧪 Test notification à " + new SimpleDateFormat("HH:mm:ss").format(new Date());
        notificationsGlobal.add(0, new Notification(message));
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test");
        alert.setHeaderText("Notification ajoutée");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> statuts = FXCollections.observableArrayList(FILTER_ALL);
        for (TopicStatus ts : TopicStatus.values()) {
            statuts.add(ts.getDisplayLabel());
        }
        ObservableList<String> sorts = FXCollections.observableArrayList(SORT_LATEST, SORT_MOST_LIKED, SORT_MOST_DISCUSSED);

        // notificationList can be absent in the current FXML (button-only notification UI)
        if (notificationList != null) {
            notificationList.setItems(notificationsGlobal);
            notificationList.setCellFactory(lv -> new ListCell<Notification>() {
                @Override
                protected void updateItem(Notification notif, boolean empty) {
                    super.updateItem(notif, empty);
                    if (empty || notif == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(notif.getMessage());
                        setStyle("-fx-padding: 8px; -fx-font-size: 12px; -fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 5px;");
                    }
                }
            });
            notificationsGlobal.addListener((ListChangeListener.Change<? extends Notification> c) -> {
                notificationList.scrollTo(0);
                updateNotifBadge();
            });
        }

        updateNotifBadge();
        statusFilter.setItems(statuts);
        statusFilter.getSelectionModel().selectFirst();
        sortFilter.setItems(sorts);
        sortFilter.getSelectionModel().select(SORT_LATEST);
        updateSavedTabState();

        searchField.textProperty().addListener((obs, oldV, newV) -> appliquerFiltres());
        statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> appliquerFiltres());
        sortFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> appliquerFiltres());

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
        String currentReaction = topicReactions.get(topic.getId());
        String newReaction = like ? "LIKE" : "DISLIKE";

        try {
            if (currentReaction == null) {
                // No reaction yet, so we just add the new one
                if (sendReactionToRestApi(topic.getId(), like, false)) {
                    topicReactions.put(topic.getId(), newReaction);
                    if (like) topic.setLikeCount(topic.getLikeCount() + 1);
                    else topic.setDislikeCount(topic.getDislikeCount() + 1);
                }
            } else if (currentReaction.equals(newReaction)) {
                // Toggle off (Undo)
                if (sendReactionToRestApi(topic.getId(), like, true)) {
                    topicReactions.remove(topic.getId());
                    if (like) topic.setLikeCount(Math.max(0, topic.getLikeCount() - 1));
                    else topic.setDislikeCount(Math.max(0, topic.getDislikeCount() - 1));
                }
            } else {
                // Switch reaction (e.g. from LIKE to DISLIKE)
                boolean undoOld = sendReactionToRestApi(topic.getId(), !like, true);
                boolean addNew = sendReactionToRestApi(topic.getId(), like, false);
                
                if (undoOld && addNew) {
                    topicReactions.put(topic.getId(), newReaction);
                    if (like) {
                        topic.setLikeCount(topic.getLikeCount() + 1);
                        topic.setDislikeCount(Math.max(0, topic.getDislikeCount() - 1));
                    } else {
                        topic.setDislikeCount(topic.getDislikeCount() + 1);
                        topic.setLikeCount(Math.max(0, topic.getLikeCount() - 1));
                    }
                }
            }
            appliquerFiltres();
        } catch (IOException ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("API error");
            err.setHeaderText("REST API not reachable");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    private boolean sendReactionToRestApi(int topicId, boolean like, boolean isUndo) throws IOException {
        String endpoint;
        if (isUndo) {
            endpoint = like ? "unlike" : "undislike";
        } else {
            endpoint = like ? "like" : "dislike";
        }
        URL url = new URL("http://localhost:" + notificationService.getApiPort() + "/api/topics/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("X-API-KEY", org.example.utils.AppConstants.REST_API_SECRET_KEY);
        String payload = "{\"topicId\":" + topicId + ",\"username\":\"Membre\"}";
        byte[] body = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body);
        }
        int status = connection.getResponseCode();
        connection.disconnect();
        if (status >= 200 && status < 300) {
            refreshPinnedTopicFromApi();
        }
        return status >= 200 && status < 300;
    }

    private void refreshPinnedTopicFromApi() throws IOException {
        URL url = new URL("http://localhost:" + notificationService.getApiPort() + "/api/topics/pinned");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        if (status >= 200 && status < 300) {
            try (InputStream is = connection.getInputStream()) {
                String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                pinnedTopicId = extractTopicId(json);
            }
        } else {
            pinnedTopicId = null;
        }
        connection.disconnect();
    }

    private static Integer extractTopicId(String json) {
        Matcher m = Pattern.compile("\"topicId\"\\s*:\\s*(\\d+)").matcher(json == null ? "" : json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
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
        t.setImagePath(src.getImagePath());
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
        sortFilter.getSelectionModel().select(SORT_LATEST);
        savedOnlyMode = false;
        updateSavedTabState();
        appliquerFiltres();
    }

    @FXML
    void showAllTopics() {
        savedOnlyMode = false;
        updateSavedTabState();
        appliquerFiltres();
    }

    @FXML
    void showSavedOnlyTopics() {
        savedOnlyMode = true;
        updateSavedTabState();
        appliquerFiltres();
    }

    private void updateSavedTabState() {
        if (btnAllTopics != null) {
            btnAllTopics.getStyleClass().remove("feed-tab-active");
            if (!savedOnlyMode) {
                btnAllTopics.getStyleClass().add("feed-tab-active");
            }
        }
        if (btnSavedOnly != null) {
            btnSavedOnly.getStyleClass().remove("feed-tab-active");
            if (savedOnlyMode) {
                btnSavedOnly.getStyleClass().add("feed-tab-active");
            }
        }
    }

    private void chargerListe() {
        try {
            sourceTopics.clear();
            sourceTopics.addAll(forumServices.afficher());
            try {
                refreshPinnedTopicFromApi();
            } catch (IOException ex) {
                pinnedTopicId = null;
            }
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
        String sortMode = sortFilter.getSelectionModel().getSelectedItem();

        List<Topic> filtered = sourceTopics.stream()
                .filter(t -> matchesSearch(t, q))
                .filter(t -> matchesStatut(t, statutFiltre))
                .filter(t -> !savedOnlyMode || savedTopicIds.contains(t.getId()))
                .collect(Collectors.toList());

        applySorting(filtered, sortMode);

        renderFeed(filtered);
        int n = filtered.size();
        int total = sourceTopics.size();
        lblResultCount.setText(n + " shown · " + total + " total");
        updateEngagementStats(filtered);
    }

    private static void applySorting(List<Topic> topics, String sortMode) {
        if (SORT_MOST_LIKED.equals(sortMode)) {
            topics.sort((a, b) -> {
                int byLikes = Integer.compare(b.getLikeCount(), a.getLikeCount());
                if (byLikes != 0) {
                    return byLikes;
                }
                return compareDatesDesc(a.getCreated_at(), b.getCreated_at());
            });
            return;
        }
        if (SORT_MOST_DISCUSSED.equals(sortMode)) {
            topics.sort((a, b) -> {
                int byReplies = Integer.compare(b.getReplyCount(), a.getReplyCount());
                if (byReplies != 0) {
                    return byReplies;
                }
                return compareDatesDesc(a.getCreated_at(), b.getCreated_at());
            });
            return;
        }
        topics.sort(Comparator.comparing(Topic::getCreated_at, Comparator.nullsLast(Date::compareTo)).reversed());
    }

    private static int compareDatesDesc(Date a, Date b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return b.compareTo(a);
    }

    private void updateEngagementStats(List<Topic> topics) {
        if (lblEngagement == null) {
            return;
        }
        int likes = 0;
        int comments = 0;
        for (Topic t : topics) {
            likes += Math.max(0, t.getLikeCount());
            comments += Math.max(0, t.getReplyCount());
        }
        lblEngagement.setText(likes + " likes · " + comments + " comments · " + savedTopicIds.size() + " saved");
    }

    private void renderFeed(List<Topic> topics) {
        topicsFeedBox.getChildren().clear();
        String sortMode = sortFilter != null ? sortFilter.getSelectionModel().getSelectedItem() : SORT_LATEST;
        boolean keepPinnedFirst = SORT_LATEST.equals(sortMode) || sortMode == null || sortMode.isBlank();
        Topic pinnedTopic = null;
        if (keepPinnedFirst && pinnedTopicId != null) {
            for (Topic t : topics) {
                if (t.getId() == pinnedTopicId) {
                    pinnedTopic = t;
                    break;
                }
            }
        }
        
        int delay = 0;
        if (pinnedTopic != null) {
            javafx.scene.layout.VBox card = buildTopicCard(pinnedTopic);
            topicsFeedBox.getChildren().add(card);
            org.example.utils.UIAnimator.animateEntrance(card, delay);
            delay += 80;
        }
        for (Topic t : topics) {
            if (pinnedTopic != null && t.getId() == pinnedTopic.getId()) {
                continue;
            }
            javafx.scene.layout.VBox card = buildTopicCard(t);
            topicsFeedBox.getChildren().add(card);
            org.example.utils.UIAnimator.animateEntrance(card, delay);
            delay += 80;
        }
    }

    private VBox buildTopicCard(Topic topic) {
        VBox card = new VBox(12);
        card.getStyleClass().add("topic-card");

        // --- EN-TÊTE (Header) ---
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox tags = new HBox(8);
        tags.setAlignment(Pos.CENTER_LEFT);
        if (pinnedTopicId != null && topic.getId() == pinnedTopicId) {
            Label pinnedBadge = new Label("📌 Pinned");
            pinnedBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold;");
            tags.getChildren().add(pinnedBadge);
        }
        Label categoryBadge = new Label(topic.getCategory() != null ? topic.getCategory().getDisplayLabel() : "Feedback");
        categoryBadge.getStyleClass().add("topic-tag-category");
        if (isTrending(topic)) {
            Label trendingBadge = new Label("🔥 Trending");
            trendingBadge.setStyle("-fx-background-color: #ffedd5; -fx-text-fill: #c2410c; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold;");
            tags.getChildren().add(trendingBadge);
        }
        Label statusBadge = new Label(topic.getStatus() != null ? topic.getStatus().getDisplayLabel() : "Unknown");
        statusBadge.getStyleClass().add("topic-tag-status");
        Label meta = new Label("posted " + formatDate(topic.getCreated_at()));
        meta.getStyleClass().add("topic-meta");
        tags.getChildren().addAll(categoryBadge, statusBadge, meta);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, javafx.scene.layout.Priority.ALWAYS);

        // NOUVEAU: Menu déroulant (Options)
        javafx.scene.control.MenuButton btnOptions = new javafx.scene.control.MenuButton("⚙ Options");
        btnOptions.getStyleClass().add("topic-menu-button");
        
        javafx.scene.control.MenuItem viewItem = new javafx.scene.control.MenuItem("👁 Voir les détails");
        viewItem.setOnAction(e -> openDetailsWindow(topic));
        
        javafx.scene.control.MenuItem editItem = new javafx.scene.control.MenuItem("✏ Modifier");
        editItem.setOnAction(e -> openEditWindow(topic));
        
        javafx.scene.control.MenuItem saveItem = new javafx.scene.control.MenuItem(savedTopicIds.contains(topic.getId()) ? "⭐ Retirer des favoris" : "⭐ Sauvegarder");
        saveItem.setOnAction(e -> {
            toggleSavedTopic(topic.getId());
            appliquerFiltres();
        });
        
        javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("🗑 Supprimer");
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(e -> openDeleteWindow(topic));
        
        btnOptions.getItems().addAll(viewItem, saveItem, editItem, new javafx.scene.control.SeparatorMenuItem(), deleteItem);
        topBar.getChildren().addAll(tags, topSpacer, btnOptions);

        // --- CONTENU (Content) ---
        Label title = new Label(nullToEmpty(topic.getTitle()));
        title.getStyleClass().add("topic-card-title");
        title.setWrapText(true);

        Label content = new Label(truncate(topic.getContent(), 220));
        content.getStyleClass().add("topic-card-content");
        content.setWrapText(true);

        ImageView topicImageView = null;
        if (topic.getImagePath() != null && !topic.getImagePath().isBlank()) {
            try {
                File imageFile = new File(topic.getImagePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString(), true);
                    topicImageView = new ImageView(image);
                    topicImageView.setFitHeight(180);
                    topicImageView.setPreserveRatio(true);
                    topicImageView.setSmooth(true);
                }
            } catch (Exception ignored) {
                topicImageView = null;
            }
        }

        // --- PIED DE PAGE (Footer) ---
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnLike = new Button("👍 " + topic.getLikeCount());
        btnLike.getStyleClass().add("btn-reaction-like");
        btnLike.setOnAction(e -> reactToTopic(topic, true));

        Button btnDislike = new Button("👎 " + topic.getDislikeCount());
        btnDislike.getStyleClass().add("btn-reaction-dislike");
        btnDislike.setOnAction(e -> reactToTopic(topic, false));

        Button btnReply = new Button("💬 " + topic.getReplyCount());
        btnReply.getStyleClass().add("btn-reaction-reply");
        btnReply.setOnAction(e -> openRepliesWindow(topic));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnTranslate = new Button("🌍 Traduire");
        btnTranslate.getStyleClass().add("btn-action-view");
        btnTranslate.setOnAction(e -> {
            btnTranslate.setDisable(true);
            btnTranslate.setText("⏳...");
            new Thread(() -> {
                String translatedTitle = org.example.integrations.translation.TranslationService.translate(topic.getTitle(), "fr");
                String translatedContent = org.example.integrations.translation.TranslationService.translate(topic.getContent(), "fr");
                javafx.application.Platform.runLater(() -> {
                    title.setText(translatedTitle);
                    content.setText(truncate(translatedContent, 220));
                    btnTranslate.setText("✅");
                });
            }).start();
        });

        Button btnShare = new Button("🔗 Partager");
        btnShare.getStyleClass().add("btn-action-view");
        btnShare.setOnAction(e -> shareTopic(topic));

        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.getChildren().addAll(btnTranslate, btnShare);

        footer.getChildren().addAll(btnLike, btnDislike, btnReply, bottomSpacer, actionButtons);

        // Add hover animations
        org.example.utils.UIAnimator.addHoverScaleEffect(btnLike);
        org.example.utils.UIAnimator.addHoverScaleEffect(btnDislike);
        org.example.utils.UIAnimator.addHoverScaleEffect(btnReply);
        org.example.utils.UIAnimator.addHoverScaleEffect(btnTranslate);
        org.example.utils.UIAnimator.addHoverScaleEffect(btnShare);

        card.getChildren().addAll(topBar, title);
        if (topicImageView != null) {
            card.getChildren().add(topicImageView);
        }
        card.getChildren().addAll(content, footer);
        return card;
    }

    private static boolean isTrending(Topic topic) {
        return topic.getLikeCount() >= 20 || topic.getReplyCount() >= 10;
    }

    private static void toggleSavedTopic(int topicId) {
        if (savedTopicIds.contains(topicId)) {
            savedTopicIds.remove(topicId);
        } else {
            savedTopicIds.add(topicId);
        }
    }

    private void shareTopic(Topic topic) {
        String publicLink = "https://zsocial.local/topic/" + topic.getId();
        try {
            String facebookShareUrl = "https://www.facebook.com/sharer/sharer.php?u="
                    + URLEncoder.encode(publicLink, StandardCharsets.UTF_8);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(facebookShareUrl));
            } else {
                throw new IOException("Desktop browser is not supported on this machine.");
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Shared");
            alert.setHeaderText(null);
            alert.setContentText("Facebook share dialog opened for this topic.");
            alert.showAndWait();
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Share failed");
            alert.setHeaderText("Facebook share unavailable");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
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