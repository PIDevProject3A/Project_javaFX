package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.ResourceBundle;

public class AjouterTopic implements Initializable {

    @FXML
    private TextArea contentArea;
    @FXML
    private ComboBox<TopicStatus> statusCombo;
    @FXML
    private ComboBox<TopicCategory> categoryCombo;
    @FXML
    private TextField Title;
    @FXML
    private Label errTitle;
    @FXML
    private Label errContent;
    @FXML
    private Label errStatus;
    @FXML
    private Label errCategory;

    private final ForumServices frS = new ForumServices();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TopicStatusComboHelper.setup(statusCombo);
        statusCombo.getItems().setAll(TopicStatus.values());
        statusCombo.setValue(TopicStatus.PENDING);
        statusCombo.getSelectionModel().select(TopicStatus.PENDING);
        categoryCombo.getItems().setAll(TopicCategory.values());
        categoryCombo.setValue(TopicCategory.FEEDBACK);

        Platform.runLater(() -> {
            if (statusCombo.getValue() == null) {
                statusCombo.setValue(TopicStatus.PENDING);
            }
        });

        Title.textProperty().addListener((o, a, b) -> setFieldError(errTitle, null));
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

    @FXML
    void goToList(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/AfficherTopic.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Community topics");
    }

    @FXML
    void Save(ActionEvent event) {
        clearFieldErrors();
        String e1 = ValidationSaisie.validerTitreTopic(Title.getText());
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
            frS.ajouter(new Topic(
                    Title.getText().trim(),
                    contentArea.getText().trim(),
                    statusVal,
                    categoryVal,
                    new Date(),
                    null));
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Saved");
            ok.setHeaderText(null);
            ok.setContentText("Your topic was added.");
            ok.showAndWait();
            goToList(event);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : "Could not save topic.");
            alert.showAndWait();
        }
    }
}
