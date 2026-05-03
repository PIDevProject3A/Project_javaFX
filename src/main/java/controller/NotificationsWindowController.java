package controller;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.entities.Notification;
import org.example.entities.Topic;
import org.example.services.ForumServices;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class NotificationsWindowController implements Initializable {

    @FXML private ListView<Notification> notificationsListView;
    @FXML private Label emptyLabel;

    private ForumServices forumServices = new ForumServices();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Lier la liste des notifications
        notificationsListView.setItems(AfficherTopic.notificationsGlobal);

        // Personnaliser l'affichage des cellules
        notificationsListView.setCellFactory(lv -> new ListCell<Notification>() {
            @Override
            protected void updateItem(Notification notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Créer un conteneur pour la notification
                    HBox container = new HBox(10);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setStyle("-fx-padding: 12px; -fx-background-radius: 8px; -fx-cursor: hand;");

                    // Effet hover
                    container.setOnMouseEntered(e -> container.setStyle(container.getStyle() + "-fx-background-color: #f1f5f9;"));
                    container.setOnMouseExited(e -> container.setStyle("-fx-padding: 12px; -fx-background-radius: 8px; -fx-cursor: hand;"));

                    // Icône
                    Label iconLabel = new Label(getEmojiForType(notif));
                    iconLabel.setStyle("-fx-font-size: 24px;");

                    // Contenu
                    VBox textBox = new VBox(4);
                    Label titleLabel = new Label(notif.getMessage());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
                    Label timeLabel = new Label(getRelativeTime(notif));
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
                    textBox.getChildren().addAll(titleLabel, timeLabel);

                    container.getChildren().addAll(iconLabel, textBox);

                    // Action au clic : ouvrir le topic
                    container.setOnMouseClicked(event -> {
                        openTopic(notif);
                    });

                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // Mettre à jour l'affichage du message "Aucune notification"
        updateEmptyMessage();

        // Écouter les changements
        AfficherTopic.notificationsGlobal.addListener((ListChangeListener<Notification>) c -> {
            updateEmptyMessage();
        });
    }

    private void updateEmptyMessage() {
        boolean empty = AfficherTopic.notificationsGlobal.isEmpty();
        emptyLabel.setVisible(empty);
        notificationsListView.setVisible(!empty);
    }

    private String getEmojiForType(Notification notif) {
        if (notif.getType() != null) {
            switch (notif.getType()) {
                case LIKE: return "👍";
                case DISLIKE: return "👎";
                case REPLY: return "💬";
                case NEW_TOPIC: return "📢";
                default: return "🔔";
            }
        }
        return "🔔";
    }

    private String getRelativeTime(Notification notif) {
        long diff = System.currentTimeMillis() - notif.getCreatedAt().getTime();
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return "Il y a " + minutes + " min";
        if (hours < 24) return "Il y a " + hours + " h";
        return "Il y a " + days + " j";
    }

    private void openTopic(Notification notif) {
        try {
            // Récupérer le topic par son ID
            int topicId = notif.getTopicId();  // ← CORRIGÉ : notif au lieu de notify

            if (topicId <= 0) {
                System.out.println("ID de topic invalide");
                return;
            }

            Topic topic = forumServices.getTopicById(topicId);

            if (topic != null) {
                // Marquer la notification comme lue
                notif.setRead(true);

                // Fermer la fenêtre des notifications
                Stage stage = (Stage) notificationsListView.getScene().getWindow();
                stage.close();

                // Ouvrir la fenêtre du topic
                openTopicWindow(topic);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openTopicWindow(Topic topic) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherReponse.fxml"));
            Parent root = loader.load();
            AfficherReponseController ctrl = loader.getController();
            ctrl.initForTopic(topic);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Topic: " + topic.getTitle());
            stage.setMinWidth(920);
            stage.setMinHeight(620);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    void closeWindow() {
        Stage stage = (Stage) notificationsListView.getScene().getWindow();
        stage.close();
    }
}