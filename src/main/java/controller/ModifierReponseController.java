package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.services.ReponseServices;
import org.example.utils.ModerationApiClient;
import org.example.utils.ValidationSaisie;

import java.sql.SQLException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.esprit.utils.SceneNavigator;

public class ModifierReponseController {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    @FXML
    private Label labTopic;
    @FXML
    private TextField topicInfoField;
    @FXML
    private TextField createdField;
    @FXML
    private TextArea areaContent;
    @FXML
    private Label errContent;

    private Reponse reponse;
    private int topicId;
    private String topicTitle = "";
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

    public void initialize() {
        areaContent.textProperty().addListener((o, a, b) -> setFieldError(errContent, null));
    }

    public void setReponse(Reponse reponse, int topicId, String topicTitle) {
        this.reponse = reponse;
        this.topicId = topicId;
        this.topicTitle = topicTitle != null ? topicTitle : "";
        if (reponse == null) {
            return;
        }
        String tt = topicTitle != null && !topicTitle.isBlank() ? topicTitle : "(no title)";
        labTopic.setText("Topic: " + tt);
        topicInfoField.setText(tt);
        areaContent.setText(reponse.getContent() != null ? reponse.getContent() : "");
        if (reponse.getCreated_at() != null) {
            synchronized (TS) {
                createdField.setText(TS.format(reponse.getCreated_at()));
            }
        } else {
            createdField.setText("—");
        }
        setFieldError(errContent, null);
    }

    @FXML
    void save(ActionEvent event) {
        if (reponse == null) {
            return;
        }
        setFieldError(errContent, null);
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

            reponse.setContent(text);
            reponse.setUpdated_at(new Date());
            reponseServices.modifier(reponse);
            navigateToReplies(event);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Saved");
            ok.setContentText("Reply updated.");
            ok.showAndWait();
        } catch (IOException e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setContentText("Moderation API unavailable: " + e.getMessage());
            errorAlert.showAndWait();
        } catch (SQLException e) {
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

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void navigateToReplies(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/AfficherReponse.fxml")));
            Parent root = loader.load();
            AfficherReponseController ctrl = loader.getController();
            org.example.entities.Topic topic = new org.example.entities.Topic();
            topic.setId(topicId);
            topic.setTitle(topicTitle);
            ctrl.initForTopic(topic);
            SceneNavigator.navigateWithRoot((Node) event.getSource(), root, null);
        } catch (Exception ex) {
            closeStage(event);
        }
    }
}
