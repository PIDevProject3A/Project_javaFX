package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.entities.Topic;
import org.example.entities.Reponse;
import org.example.services.ReponseServices;
import org.example.utils.ModerationApiClient;
import org.example.utils.ValidationSaisie;

import java.net.URL;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AjouterReponseController implements Initializable {

    @FXML
    private Label labTopic;
    @FXML
    private TextArea areaContent;
    @FXML
    private Label errContent;

    private int topicId;
    private String topicTitle = "";
    private AfficherReponseController parentController; // ← AJOUTÉ POUR NOTIFICATIONS
    private final ReponseServices reponseServices = new ReponseServices();

    private static void setFieldError(Label label, String message) {
        if (message == null || message.isBlank()) {
            label.setText("");
            label.setManaged(false);
            label.setVisible(false);
        } else {
            label.setText(message);
            label.setManaged(true);
            label.setVisible(true);
        }
    }

    private void clearFieldErrors() {
        setFieldError(errContent, null);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        areaContent.textProperty().addListener((o, a, b) -> setFieldError(errContent, null));
    }

    // MODIFIER cette méthode pour accepter le parentController
    public void initContext(int topicId, String topicTitle) {
        this.topicId = topicId;
        this.topicTitle = topicTitle != null ? topicTitle : "";
        labTopic.setText("Topic: " + (this.topicTitle.isEmpty() ? "(no title)" : this.topicTitle));
    }

    // NOUVELLE MÉTHODE avec parent pour les notifications
    public void initContext(int topicId, String topicTitle, AfficherReponseController parent) {
        this.topicId = topicId;
        this.topicTitle = topicTitle != null ? topicTitle : "";
        this.parentController = parent;
        labTopic.setText("Topic: " + (this.topicTitle.isEmpty() ? "(no title)" : this.topicTitle));
    }

    public void initContext(int topicId, String topicTitle, AfficherReponseController parent, String prefillText) {
        initContext(topicId, topicTitle, parent);
        if (prefillText != null && !prefillText.isBlank()) {
            areaContent.setText(prefillText);
            areaContent.positionCaret(areaContent.getText().length());
        }
    }

    @FXML
    void save(ActionEvent event) {
        clearFieldErrors();
        String msgContenu = ValidationSaisie.validerContenuReponse(areaContent.getText());
        if (msgContenu != null) {
            setFieldError(errContent, msgContenu);
            return;
        }

        String text = areaContent.getText().trim();
        try {
            List<String> blockedWords = ModerationApiClient.checkBadWords(text);
            if (!blockedWords.isEmpty()) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Blocked content");
                warn.setHeaderText("Your reply contains inappropriate words");
                warn.setContentText("Please remove: " + String.join(", ", blockedWords));
                warn.showAndWait();
                return;
            }

            // Sauvegarder la réponse
            reponseServices.ajouter(new Reponse(text, topicId, new Date(), null));

            // ✅ AJOUTER LA NOTIFICATION SOCIALE
            if (parentController != null) {
                parentController.addNotificationForNewReply(text);
            }

            // Naviguer vers la liste des réponses
            navigateToReplies(event);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Saved");
            ok.setHeaderText(null);
            ok.setContentText("Reply posted.");
            ok.showAndWait();

        } catch (IOException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Moderation API unavailable");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        } catch (SQLException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Database error");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    }

    @FXML
    void cancel(ActionEvent event) {
        navigateToReplies(event);
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }
    }
}