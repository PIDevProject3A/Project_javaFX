package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.entities.Reponse;
import org.example.entities.Topic;
import org.example.utils.AppConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import com.esprit.utils.SceneNavigator;

public class DetailReponseController {

    private static final SimpleDateFormat DF =
            new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    @FXML
    private Label labAvatar;
    @FXML
    private Label labTopic;
    @FXML
    private Label labAuthor;
    @FXML
    private Label labCreated;
    @FXML
    private Label labUpdated;
    @FXML
    private TextArea areaContent;
    private int topicId;
    private String topicTitle = "";

    public void setReponse(Reponse reponse, int topicId, String topicTitle) {
        this.topicId = topicId;
        this.topicTitle = topicTitle != null ? topicTitle : "";
        if (reponse == null) {
            return;
        }
        String titreSujet = topicTitle != null && !topicTitle.isBlank() ? topicTitle.trim() : "No title";
        labTopic.setText(titreSujet);
        labAuthor.setText("By " + AppConstants.FORUM_USER_DISPLAY_NAME + " · in this topic");

        String contenu = reponse.getContent() != null ? reponse.getContent() : "";
        labAvatar.setText(initiale(contenu.isBlank() ? titreSujet : contenu));

        labCreated.setText(reponse.getCreated_at() != null
                ? "Posted · " + format(reponse.getCreated_at())
                : "—");
        labCreated.getStyleClass().setAll("sm-badge", "sm-badge-neutral");

        if (reponse.getUpdated_at() != null) {
            labUpdated.setText("Edited · " + format(reponse.getUpdated_at()));
            labUpdated.getStyleClass().setAll("sm-badge", "sm-badge-neutral");
        } else {
            labUpdated.setText("Not edited yet");
            labUpdated.getStyleClass().setAll("sm-badge", "sm-badge-muted");
        }

        areaContent.setText(contenu);
    }

    private static String initiale(String texte) {
        if (texte == null || texte.isBlank()) {
            return "?";
        }
        return texte.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String format(Date d) {
        if (d == null) {
            return "—";
        }
        synchronized (DF) {
            return DF.format(d);
        }
    }

    @FXML
    void close(ActionEvent event) {
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }
    }
}
