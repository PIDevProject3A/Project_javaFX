package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import services.CompreFaceFaceIdService;
import services.UserService;
import utils.SceneNavigator;

import java.io.File;

public class FaceIdEnrollController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label selectedImageLabel;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private final CompreFaceFaceIdService faceIdService = new CompreFaceFaceIdService();

    private File selectedImageFile;

    @FXML
    private void handlePickImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de visage");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp"
        ));
        File file = chooser.showOpenDialog(emailField.getScene() == null ? null : emailField.getScene().getWindow());
        if (file == null) {
            return;
        }
        selectedImageFile = file;
        selectedImageLabel.setText(file.getName());
    }

    @FXML
    private void handleEnrollFaceId() {
        if (!faceIdService.isConfigured()) {
            showError("CompreFace n'est pas configure.");
            return;
        }

        if (selectedImageFile == null) {
            showError("Veuillez choisir une image avant l'enrollement.");
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        User user = userService.login(email, password);
        if (user == null) {
            showError("Email ou mot de passe invalide.");
            return;
        }

        String faceSubject = userService.getOrCreateFaceSubject(user.getEmail());
        if (faceSubject == null || faceSubject.isBlank()) {
            showError("Impossible de preparer le profil Face ID.");
            return;
        }

        CompreFaceFaceIdService.EnrollResult result = faceIdService.enrollFace(faceSubject, selectedImageFile.toPath());
        if (!result.success()) {
            showError(result.message());
            return;
        }

        showSuccess("Face ID enregistre. Vous pouvez vous connecter sans email.");
    }

    @FXML
    private void goBackToLogin() {
        SceneNavigator.navigate(emailField, "/Login.fxml", this::showError);
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add("status-error");
        messageLabel.setText(message);
    }

    private void showSuccess(String message) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add("status-success");
        messageLabel.setText(message);
    }
}

