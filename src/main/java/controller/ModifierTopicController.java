package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.services.ForumServices;
import org.example.utils.TopicStatusComboHelper;
import org.example.utils.ValidationSaisie;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ModifierTopicController {

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private ComboBox<TopicStatus> statusCombo;
    @FXML
    private ComboBox<TopicCategory> categoryCombo;
    @FXML
    private TextField createdField;
    @FXML
    private Label errTitle;
    @FXML
    private Label errContent;
    @FXML
    private Label errStatus;
    @FXML
    private Label errCategory;

    private Topic topic;
    private Runnable onSaved;
    private final ForumServices forumServices = new ForumServices();

    public void initialize() {
        TopicStatusComboHelper.setup(statusCombo);
        statusCombo.getItems().setAll(TopicStatus.values());
        categoryCombo.getItems().setAll(TopicCategory.values());

        titleField.textProperty().addListener((o, a, b) -> setFieldError(errTitle, null));
        contentArea.textProperty().addListener((o, a, b) -> setFieldError(errContent, null));
        statusCombo.valueProperty().addListener((o, a, b) -> setFieldError(errStatus, null));
        categoryCombo.valueProperty().addListener((o, a, b) -> setFieldError(errCategory, null));
    }

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
        setFieldError(errTitle, null);
        setFieldError(errContent, null);
        setFieldError(errStatus, null);
        setFieldError(errCategory, null);
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
        if (topic == null) {
            return;
        }
        titleField.setText(topic.getTitle() != null ? topic.getTitle() : "");
        contentArea.setText(topic.getContent() != null ? topic.getContent() : "");
        TopicStatus st = topic.getStatus() != null ? topic.getStatus() : TopicStatus.PENDING;
        statusCombo.setValue(st);
        statusCombo.getSelectionModel().select(st);
        TopicCategory cat = topic.getCategory() != null ? topic.getCategory() : TopicCategory.FEEDBACK;
        categoryCombo.setValue(cat);
        categoryCombo.getSelectionModel().select(cat);
        Platform.runLater(() -> {
            if (statusCombo.getValue() == null) {
                statusCombo.setValue(st);
            }
            if (categoryCombo.getValue() == null) {
                categoryCombo.setValue(cat);
            }
        });
        if (topic.getCreated_at() != null) {
            synchronized (TS) {
                createdField.setText(TS.format(topic.getCreated_at()));
            }
        } else {
            createdField.setText("—");
        }
        clearFieldErrors();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    void save(ActionEvent event) {
        if (topic == null) {
            return;
        }
        clearFieldErrors();
        String e1 = ValidationSaisie.validerTitreTopic(titleField.getText());
        if (e1 != null) {
            setFieldError(errTitle, e1);
        }
        String e2 = ValidationSaisie.validerContenuTopic(contentArea.getText());
        if (e2 != null) {
            setFieldError(errContent, e2);
        }
        TopicStatus statusVal = statusCombo.getValue();
        if (statusVal == null) {
            statusVal = TopicStatus.PENDING;
            statusCombo.setValue(TopicStatus.PENDING);
        }
        String e3 = ValidationSaisie.validerStatutTopic(statusVal);
        if (e3 != null) {
            setFieldError(errStatus, e3);
        }
        TopicCategory categoryVal = categoryCombo.getValue();
        String e4 = ValidationSaisie.validerCategorieTopic(categoryVal);
        if (e4 != null) {
            setFieldError(errCategory, e4);
        }
        if (e1 != null || e2 != null || e3 != null || e4 != null) {
            return;
        }
        try {
            topic.setTitle(titleField.getText().trim());
            topic.setContent(contentArea.getText().trim());
            topic.setStatus(statusVal);
            topic.setCategory(categoryVal);
            topic.setUpdated_at(new Date());
            forumServices.modifier(topic);
            if (onSaved != null) {
                onSaved.run();
            }
            navigateToList(event);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Saved");
            ok.setHeaderText(null);
            ok.setContentText("Your topic was updated.");
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
