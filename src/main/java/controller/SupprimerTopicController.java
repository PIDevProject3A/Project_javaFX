package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entities.Topic;
import org.example.services.ForumServices;

import java.sql.SQLException;
import java.util.Objects;

public class SupprimerTopicController {

    @FXML
    private Label messageLabel;

    private Topic topic;
    private Runnable onDeleted;
    private final ForumServices forumServices = new ForumServices();

    public void setTopic(Topic topic) {
        this.topic = topic;
        if (topic != null) {
            String title = topic.getTitle() != null ? topic.getTitle() : "(no title)";
            messageLabel.setText("Delete this topic for good?\n\"" + title + "\"");
        }
    }

    public void setOnDeleted(Runnable onDeleted) {
        this.onDeleted = onDeleted;
    }

    @FXML
    void confirmDelete(ActionEvent event) {
        if (topic == null) {
            return;
        }
        try {
            forumServices.supprimer(topic.getId());
            if (onDeleted != null) {
                onDeleted.run();
            }
            navigateToList(event);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Deleted");
            ok.setHeaderText(null);
            ok.setContentText("The topic was removed.");
            ok.showAndWait();
        } catch (SQLException e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Error");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    void cancel(ActionEvent event) {
        navigateToList(event);
    }

    private void closeWindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void navigateToList(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/AfficherTopic.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Community topics");
        } catch (Exception e) {
            closeWindow(event);
        }
    }
}
