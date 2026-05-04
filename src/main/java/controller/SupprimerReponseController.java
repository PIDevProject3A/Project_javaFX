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
import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.services.ReponseServices;

import java.sql.SQLException;
import java.util.Objects;

public class SupprimerReponseController {

    @FXML
    private Label messageLabel;

    private Reponse reponse;
    private int topicId;
    private String topicTitle = "";
    private final ReponseServices reponseServices = new ReponseServices();

    public void setReponse(Reponse reponse, int topicId, String topicTitle) {
        this.reponse = reponse;
        this.topicId = topicId;
        this.topicTitle = topicTitle != null ? topicTitle : "";
        if (reponse != null) {
            String preview = reponse.getContent() != null
                    ? reponse.getContent().replace('\n', ' ').trim()
                    : "";
            if (preview.length() > 80) {
                preview = preview.substring(0, 77) + "...";
            }
            messageLabel.setText("Delete this reply?\n\"" + preview + "\"");
        }
    }

    @FXML
    void confirmDelete(ActionEvent event) {
        if (reponse == null) {
            return;
        }
        try {
            reponseServices.supprimer(reponse.getId());
            navigateToReplies(event);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Deleted");
            ok.setContentText("The reply was removed.");
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
        navigateToReplies(event);
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void navigateToReplies(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/AfficherReponse.fxml")));
            Parent root = loader.load();
            AfficherReponseController ctrl = loader.getController();
            Topic topic = new Topic();
            topic.setId(topicId);
            topic.setTitle(topicTitle);
            ctrl.initForTopic(topic);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Replies - " + (topicTitle == null || topicTitle.isBlank() ? "topic" : topicTitle));
        } catch (Exception ex) {
            closeStage(event);
        }
    }
}
