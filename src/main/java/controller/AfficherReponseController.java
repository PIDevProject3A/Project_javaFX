package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
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
import java.util.Locale;
import java.util.Objects;
public class AfficherReponseController {

    private static final SimpleDateFormat DF = new SimpleDateFormat("MMM d, yyyy  ·  HH:mm", Locale.ENGLISH);

    @FXML
    private TableView<Reponse> tableReponses;
    @FXML
    private Label labTitle;
    @FXML
    private Label labSubtitle;

    private int topicId;
    private String topicTitle = "";
    private final ReponseServices reponseServices = new ReponseServices();
    private final NotificationService notificationService = NotificationService.getInstance();
    private boolean columnsReady;

    public void initForTopic(Topic topic) {
        this.topicId = topic.getId();
        this.topicTitle = topic.getTitle() != null ? topic.getTitle() : "";
        labTitle.setText("Thread");
        labSubtitle.setText("Topic · " + (this.topicTitle.isEmpty() ? "(no title)" : this.topicTitle));
        if (!columnsReady) {
            buildColumns();
            columnsReady = true;
        }
        refresh();
    }


    private void buildColumns() {
        TableColumn<Reponse, String> fromCol = new TableColumn<>("From");
        fromCol.setPrefWidth(150);
        fromCol.setMinWidth(130);
        fromCol.setCellValueFactory(c -> new SimpleStringProperty(""));
        fromCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Label av = new Label(initialFromUser());
                    av.getStyleClass().add("avatar-chip");
                    av.setAlignment(Pos.CENTER);
                    Label name = new Label(AppConstants.FORUM_USER_DISPLAY_NAME);
                    name.getStyleClass().add("member-name");
                    Label hint = new Label("Forum member");
                    hint.getStyleClass().add("member-hint");
                    VBox text = new VBox(2, name, hint);
                    HBox row = new HBox(12, av, text);
                    row.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(row);
                }
            }
        });

        TableColumn<Reponse, String> msgCol = new TableColumn<>("Message");
        msgCol.setPrefWidth(260);
        msgCol.setCellValueFactory(c -> {
            String t = preview(c.getValue());
            return new SimpleStringProperty(t);
        });
        msgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label l = new Label(text);
                    l.getStyleClass().add("col-message");
                    l.setWrapText(true);
                    l.setMaxWidth(480);
                    setGraphic(l);
                }
            }
        });

        TableColumn<Reponse, String> postedCol = new TableColumn<>("Posted");
        postedCol.setPrefWidth(120);
        postedCol.setCellValueFactory(c ->
                new SimpleStringProperty(formatDate(c.getValue().getCreated_at())));
        postedCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty ? null : t);
                if (!empty) {
                    getStyleClass().removeAll("col-date", "col-date-muted");
                    getStyleClass().add("col-date");
                }
            }
        });

        TableColumn<Reponse, String> editedCol = new TableColumn<>("Last edit");
        editedCol.setPrefWidth(130);
        editedCol.setCellValueFactory(c -> {
            Date u = c.getValue().getUpdated_at();
            return new SimpleStringProperty(u == null ? "—  Not edited yet" : formatDate(u));
        });
        editedCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty ? null : t);
                if (!empty) {
                    getStyleClass().removeAll("col-date", "col-date-muted");
                    if (t != null && t.startsWith("—")) {
                        getStyleClass().add("col-date-muted");
                    } else {
                        getStyleClass().add("col-date");
                    }
                }
            }
        });

        TableColumn<Reponse, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(420);
        actionsCol.setMinWidth(400);
        actionsCol.setMaxWidth(520);
        actionsCol.setResizable(false);
        actionsCol.setSortable(false);
        actionsCol.setCellFactory(ac -> new TableCell<>() {
            private final Button btnLike = new Button();
            private final Button btnDislike = new Button();
            private final Button btnReply = new Button("Reply");
            private final Button btnDetails = new Button("View");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox box = new HBox(8, btnLike, btnDislike, btnReply, btnDetails, btnEdit, btnDelete);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnLike.getStyleClass().add("btn-reaction-like");
                btnDislike.getStyleClass().add("btn-reaction-dislike");
                btnReply.getStyleClass().add("btn-reaction-reply");
                btnDetails.getStyleClass().add("btn-feed-action");
                btnEdit.getStyleClass().add("btn-feed-action");
                btnDelete.getStyleClass().add("btn-feed-action");
                btnLike.setMinWidth(62);
                btnDislike.setMinWidth(62);
                btnReply.setMinWidth(66);
                btnDetails.setMinWidth(56);
                btnEdit.setMinWidth(52);
                btnDelete.setMinWidth(62);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Reponse r = getTableRow().getItem();
                    btnLike.setText("👍 " + r.getLikeCount());
                    btnDislike.setText("👎 " + r.getDislikeCount());
                    btnLike.setOnAction(e -> reactToReply(r, true));
                    btnDislike.setOnAction(e -> reactToReply(r, false));
                    btnReply.setOnAction(e -> openAddReplyWithPrefill(r));
                    btnDetails.setOnAction(e -> openDetail(r));
                    btnEdit.setOnAction(e -> openEdit(r));
                    btnDelete.setOnAction(e -> openDelete(r));
                    setGraphic(box);
                }
            }
        });

        tableReponses.getColumns().setAll(fromCol, msgCol, postedCol, editedCol, actionsCol);
        tableReponses.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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
        return (Stage) tableReponses.getScene().getWindow();
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
                tableReponses.refresh();
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
    //appel like and dislike

    private boolean sendReplyReactionToRestApi(int replyId, boolean like) throws IOException {
        String endpoint = like ? "like" : "dislike";
        URL url = new URL("http://localhost:" + notificationService.getApiPort() + "/api/replies/" + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
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
            tableReponses.getItems().setAll(reponseServices.afficherParTopic(topicId));
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

    private void showError(IOException ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("Could not open this window");
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }
    public void addNotificationForNewReply(String replyContent) {
        notificationService.publishReply("Membre", topicTitle, topicId);
    }
}
