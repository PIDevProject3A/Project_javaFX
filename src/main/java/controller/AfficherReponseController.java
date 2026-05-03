package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.services.NotificationService;
import org.example.services.ReponseServices;
import org.example.utils.AppConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AfficherReponseController {

    private static final SimpleDateFormat DF = new SimpleDateFormat("MMM d, yyyy  ·  HH:mm", Locale.ENGLISH);

    @FXML
    private VBox commentsFeed;
    @FXML
    private Label labTitle;
    @FXML
    private Label labSubtitle;

    private int topicId;
    private String topicTitle = "";
    private final ReponseServices reponseServices = new ReponseServices();
    private final NotificationService notificationService = NotificationService.getInstance();

    public void initForTopic(Topic topic) {
        this.topicId = topic.getId();
        this.topicTitle = topic.getTitle() != null ? topic.getTitle() : "";
        labTitle.setText("Conversation");
        labSubtitle.setText("Topic · " + (this.topicTitle.isEmpty() ? "(no title)" : this.topicTitle));
        refresh();
    }

    private VBox buildCommentCard(Reponse r) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-reponse-feed-v2");

        // --- EN-TÊTE DU COMMENTAIRE ---
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(initialFromUser());
        avatar.getStyleClass().add("avatar-chip");

        VBox authorInfo = new VBox(2);
        Label nameLabel = new Label(AppConstants.FORUM_USER_DISPLAY_NAME);
        nameLabel.getStyleClass().add("member-name");
        
        HBox dateBox = new HBox(6);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label(formatDate(r.getCreated_at()));
        dateLabel.getStyleClass().add("col-date");
        
        dateBox.getChildren().add(dateLabel);
        
        if (r.getUpdated_at() != null) {
            Label editedBadge = new Label(" (modifié)");
            editedBadge.getStyleClass().add("col-date-muted");
            dateBox.getChildren().add(editedBadge);
        }

        authorInfo.getChildren().addAll(nameLabel, dateBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Options du commentaire
        MenuButton btnOptions = new MenuButton("⚙ Options");
        btnOptions.getStyleClass().add("topic-menu-button");

        MenuItem viewItem = new MenuItem("👁 Voir en détail");
        viewItem.setOnAction(e -> openDetail(r));

        MenuItem editItem = new MenuItem("✏ Modifier");
        editItem.setOnAction(e -> openEdit(r));

        MenuItem deleteItem = new MenuItem("🗑 Supprimer");
        deleteItem.getStyleClass().add("menu-item-delete");
        deleteItem.setOnAction(e -> openDelete(r));

        btnOptions.getItems().addAll(viewItem, editItem, new SeparatorMenuItem(), deleteItem);

        header.getChildren().addAll(avatar, authorInfo, spacer, btnOptions);

        // --- TEXTE DU COMMENTAIRE ---
        Label content = new Label(r.getContent() != null ? r.getContent() : "");
        content.getStyleClass().add("col-message");
        content.setWrapText(true);

        // --- ACTIONS SOCIALES ---
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new javafx.geometry.Insets(8, 0, 0, 0));

        Button btnLike = new Button("👍 " + r.getLikeCount());
        btnLike.getStyleClass().add("btn-reaction-like");
        btnLike.setOnAction(e -> reactToReply(r, true));

        Button btnDislike = new Button("👎 " + r.getDislikeCount());
        btnDislike.getStyleClass().add("btn-reaction-dislike");
        btnDislike.setOnAction(e -> reactToReply(r, false));

        Button btnReply = new Button("💬 Répondre");
        btnReply.getStyleClass().add("btn-reaction-reply");
        btnReply.setOnAction(e -> openAddReplyWithPrefill(r));

        actions.getChildren().addAll(btnLike, btnDislike, btnReply);

        // Hover animations
        try {
            org.example.utils.UIAnimator.addHoverScaleEffect(btnLike);
            org.example.utils.UIAnimator.addHoverScaleEffect(btnDislike);
            org.example.utils.UIAnimator.addHoverScaleEffect(btnReply);
        } catch(Exception ignored){}

        card.getChildren().addAll(header, content, actions);
        return card;
    }

    private static String initialFromUser() {
        String n = AppConstants.FORUM_USER_DISPLAY_NAME;
        if (n == null || n.isBlank()) {
            return "?";
        }
        char ch = Character.toUpperCase(n.trim().charAt(0));
        return Character.isLetterOrDigit(ch) ? String.valueOf(ch) : "?";
    }

    private static String preview(Reponse r) {
        String c = r.getContent();
        if (c == null) {
            return "(empty)";
        }
        String t = c.replace('\n', ' ').trim();
        return t.length() <= 200 ? t : t.substring(0, 197) + "...";
    }

    private static String formatDate(Date d) {
        if (d == null) {
            return "—";
        }
        synchronized (DF) {
            return DF.format(d);
        }
    }

    private Stage ownerStage() {
        return (Stage) commentsFeed.getScene().getWindow();
    }

    private void setSceneOnCurrentStage(Parent root, String title) {
        Stage stage = ownerStage();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.setMinWidth(920);
        stage.setMinHeight(620);
    }

    private void openDetail(Reponse r) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/DetailReponse.fxml")));
            Parent root = loader.load();
            DetailReponseController ctrl = loader.getController();
            ctrl.setReponse(r, topicId, topicTitle);
            setSceneOnCurrentStage(root, "Reply details");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void openEdit(Reponse r) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/ModifierReponse.fxml")));
            Parent root = loader.load();
            ModifierReponseController ctrl = loader.getController();
            ctrl.setReponse(cloneReponse(r), topicId, topicTitle);
            setSceneOnCurrentStage(root, "Edit reply");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private static Reponse cloneReponse(Reponse src) {
        Reponse c = new Reponse();
        c.setId(src.getId());
        c.setContent(src.getContent());
        c.setTopic_id(src.getTopic_id());
        c.setCreated_at(src.getCreated_at());
        c.setUpdated_at(src.getUpdated_at());
        return c;
    }

    private void openDelete(Reponse r) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/SupprimerReponse.fxml")));
            Parent root = loader.load();
            SupprimerReponseController ctrl = loader.getController();
            ctrl.setReponse(r, topicId, topicTitle);
            setSceneOnCurrentStage(root, "Delete reply");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    @FXML
    void openAddReply() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/AjouterReponse.fxml")));
            Parent root = loader.load();
            AjouterReponseController ctrl = loader.getController();
            ctrl.initContext(topicId, topicTitle, this);
            setSceneOnCurrentStage(root, "New reply");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void openAddReplyWithPrefill(Reponse targetReply) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/AjouterReponse.fxml")));
            Parent root = loader.load();
            AjouterReponseController ctrl = loader.getController();
            String snippet = preview(targetReply);
            ctrl.initContext(topicId, topicTitle, this, "@reply-" + targetReply.getId() + " " + snippet + System.lineSeparator());
            setSceneOnCurrentStage(root, "Reply to comment");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void reactToReply(Reponse reply, boolean like) {
        try {
            if (sendReplyReactionToRestApi(reply.getId(), like)) {
                if (like) {
                    reply.setLikeCount(reply.getLikeCount() + 1);
                } else {
                    reply.setDislikeCount(reply.getDislikeCount() + 1);
                }
                refresh();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("API error");
                a.setHeaderText("Could not register reaction");
                a.setContentText("Reply like/dislike failed via REST API.");
                a.showAndWait();
            }
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("API error");
            a.setHeaderText("REST API not reachable");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
    }

    private boolean sendReplyReactionToRestApi(int replyId, boolean like) throws IOException {
        String endpoint = like ? "like" : "dislike";
        URL url = new URL("http://localhost:" + notificationService.getApiPort() + "/api/replies/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("X-API-KEY", AppConstants.REST_API_SECRET_KEY);
        String payload = "{\"replyId\":" + replyId + "}";
        byte[] body = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body);
        }
        int status = connection.getResponseCode();
        connection.disconnect();
        return status >= 200 && status < 300;
    }

    @FXML
    void refresh() {
        try {
            List<Reponse> reponses = reponseServices.afficherParTopic(topicId);
            commentsFeed.getChildren().clear();
            if (reponses.isEmpty()) {
                Label emptyLabel = new Label("Aucun message. Soyez le premier à répondre !");
                emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-padding: 20px;");
                commentsFeed.getChildren().add(emptyLabel);
            } else {
                for (int i = 0; i < reponses.size(); i++) {
                    VBox card = buildCommentCard(reponses.get(i));
                    commentsFeed.getChildren().add(card);
                    try {
                        org.example.utils.UIAnimator.animateEntrance(card, i * 100);
                    } catch(Exception ignored){}
                }
            }
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Error");
            a.setHeaderText("Could not load replies");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    void goBackToTopics() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/AfficherTopic.fxml")));
            Stage stage = ownerStage();
            stage.setScene(new Scene(root));
            stage.setTitle("Community topics");
        } catch (IOException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("An error occurred");
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }

    public void addNotificationForNewReply(String replyContent) {
        notificationService.publishReply("Membre", topicTitle, topicId);
    }
}
