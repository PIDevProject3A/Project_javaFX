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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.entities.Topic;
import org.example.entities.TopicCategory;
import org.example.entities.TopicStatus;
import org.example.services.ForumServices;
import org.example.services.NotificationService;
import org.example.utils.ModerationApiClient;
import org.example.utils.TopicStatusComboHelper;
import org.example.utils.ValidationSaisie;
import com.esprit.utils.SceneNavigator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class AjouterTopic implements Initializable {
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

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
    @FXML
    private Label imagePathLabel;
    @FXML
    private ImageView imagePreview;

    private final ForumServices frS = new ForumServices();
    private final NotificationService notificationService = NotificationService.getInstance();
    private String selectedImagePath;

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

    @FXML
    void chooseImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select topic image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }
        if (selected.length() > MAX_IMAGE_SIZE_BYTES) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Image too large");
            alert.setHeaderText("Maximum size is 5 MB");
            alert.setContentText("Please choose an image smaller than 5 MB.");
            alert.showAndWait();
            return;
        }
        try {
            Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "topics");
            Files.createDirectories(uploadDir);
            String fileName = System.currentTimeMillis() + "_" + selected.getName().replaceAll("\\s+", "_");
            Path target = uploadDir.resolve(fileName);
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            selectedImagePath = target.toString();
            imagePathLabel.setText(selected.getName());
            Image previewImage = new Image(target.toUri().toString(), true);
            imagePreview.setImage(previewImage);
            imagePreview.setVisible(true);
            imagePreview.setManaged(true);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Upload error");
            alert.setContentText("Could not save image: " + ex.getMessage());
            alert.showAndWait();
        }
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
        SceneNavigator.navigate((Node) event.getSource(), "/AfficherTopic.fxml", null);
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
        //appel de methode bad words
        try {
            List<String> blockedWords = ModerationApiClient.checkBadWords(Title.getText() + " " + contentArea.getText());
            if (!blockedWords.isEmpty()) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Blocked content");
                warn.setHeaderText("Your topic contains inappropriate words");
                warn.setContentText("Please remove: " + String.join(", ", blockedWords));
                warn.showAndWait();
                return;
            }

            Topic newTopic = new Topic(
                    Title.getText().trim(),
                    contentArea.getText().trim(),
                    statusVal,
                    categoryVal,
                    new Date(),
                    null);
            newTopic.setImagePath(selectedImagePath);

            // Maintenant ajouter() retourne l'ID
            int newTopicId = frS.ajouter(newTopic);

            notificationService.publishTopicCreated("Membre", Title.getText().trim(), newTopicId);

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
