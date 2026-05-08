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
import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import com.esprit.utils.SceneNavigator;

public class DetailTopicController {

    private static final SimpleDateFormat DF =
            new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

    @FXML
    private Label labAvatar;
    @FXML
    private Label labTitle;
    @FXML
    private Label labStatus;
    @FXML
    private Label labCategory;
    @FXML
    private Label labCreated;
    @FXML
    private Label labUpdated;
    @FXML
    private TextArea areaContent;

    public void setTopic(Topic topic) {
        if (topic == null) {
            return;
        }
        String titre = topic.getTitle() != null ? topic.getTitle().trim() : "";
        labTitle.setText(titre.isEmpty() ? "No title" : titre);
        labAvatar.setText(initiale(titre));

        appliquerStyleStatut(topic.getStatus());
        labStatus.setText(simpleStatusLabel(topic.getStatus()));
        TopicCategory category = topic.getCategory() != null ? topic.getCategory() : TopicCategory.FEEDBACK;
        labCategory.setText("Category · " + category.getDisplayLabel());
        labCategory.getStyleClass().setAll("sm-badge", "sm-badge-neutral");

        labCreated.setText(topic.getCreated_at() != null
                ? "Posted · " + format(topic.getCreated_at())
                : "—");
        labCreated.getStyleClass().setAll("sm-badge", "sm-badge-neutral");

        if (topic.getUpdated_at() != null) {
            labUpdated.setText("Edited · " + format(topic.getUpdated_at()));
            labUpdated.getStyleClass().setAll("sm-badge", "sm-badge-neutral");
        } else {
            labUpdated.setText("Not edited yet");
            labUpdated.getStyleClass().setAll("sm-badge", "sm-badge-muted");
        }

        areaContent.setText(topic.getContent() != null ? topic.getContent() : "");
    }

    private static String initiale(String titre) {
        if (titre == null || titre.isBlank()) {
            return "?";
        }
        return titre.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String simpleStatusLabel(TopicStatus s) {
        if (s == null) {
            return "Unknown";
        }
        return switch (s) {
            case PENDING -> "Waiting review";
            case ACCEPTED -> "Approved";
            case REFUSED -> "Declined";
        };
    }

    private void appliquerStyleStatut(TopicStatus s) {
        labStatus.getStyleClass().setAll("sm-badge");
        if (s == null) {
            labStatus.getStyleClass().add("sm-badge-neutral");
            return;
        }
        switch (s) {
            case PENDING -> labStatus.getStyleClass().add("sm-badge-pending");
            case ACCEPTED -> labStatus.getStyleClass().add("sm-badge-accepted");
            case REFUSED -> labStatus.getStyleClass().add("sm-badge-refused");
        }
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
        SceneNavigator.navigate((Node) event.getSource(), "/AfficherTopic.fxml", null);
    }
}
