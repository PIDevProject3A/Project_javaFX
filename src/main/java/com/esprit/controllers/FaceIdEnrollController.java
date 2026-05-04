package com.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.esprit.entities.User;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.esprit.services.CompreFaceFaceIdService;
import com.esprit.services.UserService;
import com.esprit.utils.SceneNavigator;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

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

    private Path capturedImagePath;

    @FXML
    public void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
    }

    @FXML
    private void handlePickImage() {
        try {
            Path captured = openCameraPreviewDialog();
            if (captured != null) {
                // Nettoyer l'ancienne capture s'il y en avait une
                if (capturedImagePath != null) {
                    try { Files.deleteIfExists(capturedImagePath); } catch (IOException ignored) {}
                }
                capturedImagePath = captured;
                selectedImageLabel.setText("📸 Photo capturee avec succes");
                selectedImageLabel.setStyle("-fx-text-fill: #2E7D32;");
            }
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleEnrollFaceId() {
        if (!faceIdService.isConfigured()) {
            showError("CompreFace n'est pas configure.");
            return;
        }

        if (capturedImagePath == null || !Files.exists(capturedImagePath)) {
            showError("Veuillez capturer une photo avant l'enrollement.");
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

        CompreFaceFaceIdService.EnrollResult result = faceIdService.enrollFace(faceSubject, capturedImagePath);
        if (!result.success()) {
            showError(result.message());
            return;
        }

        // Nettoyage du fichier temporaire après enrôlement réussi
        try { Files.deleteIfExists(capturedImagePath); } catch (IOException ignored) {}
        capturedImagePath = null;

        showSuccess("Face ID enregistre. Vous pouvez vous connecter sans email.");
    }

    @FXML
    private void goBackToLogin() {
        // Nettoyage si on quitte sans enrôler
        if (capturedImagePath != null) {
            try { Files.deleteIfExists(capturedImagePath); } catch (IOException ignored) {}
        }
        SceneNavigator.navigate(emailField, "/Login.fxml", this::showError);
    }

    /**
     * Ouvre une fenêtre modale avec le preview webcam en direct.
     * L'utilisateur peut se positionner puis cliquer "Capturer".
     * Retourne null si l'utilisateur annule.
     */
    private Path openCameraPreviewDialog() {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune camera detectee sur ce PC.");
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        final Path[] capturedPath = {null};

        // --- Construction de la fenêtre de preview ---
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(emailField.getScene().getWindow());
        dialog.setTitle("Face ID - Capture visage");
        dialog.setResizable(false);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(46, 125, 50, 0.25), 16, 0.2, 0, 4);");

        Label instructionLabel = new Label("Positionnez votre visage face a la camera");
        instructionLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2E7D32;");

        Button captureBtn = new Button("\uD83D\uDCF8  Capturer");
        captureBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: #e4f3df; -fx-text-fill: #245126; -fx-font-size: 13px; " +
                "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 22; -fx-cursor: hand;");

        HBox buttonBox = new HBox(14, captureBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 14; -fx-padding: 6;");

        VBox root = new VBox(16, instructionLabel, imageContainer, buttonBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fcf2, #F1F8E9);");

        Scene scene = new Scene(root);
        dialog.setScene(scene);

        // --- Thread en arrière-plan pour le flux webcam ---
        final boolean[] running = {true};

        Thread webcamThread = new Thread(() -> {
            while (running[0]) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> imageView.setImage(fxImage));
                    }
                    Thread.sleep(33); // ~30 fps
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    break;
                }
            }
        });
        webcamThread.setDaemon(true);
        webcamThread.setName("webcam-enroll-preview");
        webcamThread.start();

        // --- Actions boutons ---
        captureBtn.setOnAction(e -> {
            running[0] = false;
            BufferedImage snapshot = webcam.getImage();
            if (snapshot != null) {
                try {
                    Path tempImage = Files.createTempFile("face-enroll-", ".jpg");
                    ImageIO.write(snapshot, "JPG", tempImage.toFile());
                    capturedPath[0] = tempImage;
                } catch (IOException ex) {
                    // sera traité comme annulation
                }
            }
            webcam.close();
            dialog.close();
        });

        cancelBtn.setOnAction(e -> {
            running[0] = false;
            webcam.close();
            dialog.close();
        });

        dialog.setOnCloseRequest(e -> {
            running[0] = false;
            if (webcam.isOpen()) {
                webcam.close();
            }
        });

        dialog.showAndWait();

        return capturedPath[0];
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

