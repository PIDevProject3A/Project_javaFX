package com.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.esprit.entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.esprit.services.UserService;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

public class FaceIdAdminManagementController {
    @FXML
    private ComboBox<User> userBox;

    @FXML
    private Label faceIdStatusLabel;



    @FXML
    private Button addFaceIdBtn;

    @FXML
    private Button deleteFaceIdBtn;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            setEditingEnabled(false);
            return;
        }

        List<User> users = userService.getUsersEditableByCurrentUser(UserSession.getCurrentUserRole());
        userBox.setItems(FXCollections.observableArrayList(users));
        userBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldUser, newUser) -> refreshFaceStatus(newUser));

        User sessionUser = UserSession.getUserToEdit();
        if (sessionUser != null) {
            // Find the user in the list by email
            users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(sessionUser.getEmail()))
                .findFirst()
                .ifPresent(u -> {
                    userBox.getSelectionModel().select(u);
                    userBox.setDisable(true); // Lock selection to the context provided
                });
            UserSession.setUserToEdit(null); // Clear context
        } else if (!users.isEmpty()) {
            userBox.getSelectionModel().selectFirst();
        } else {
            faceIdStatusLabel.setText("Aucun utilisateur disponible.");
            setEditingEnabled(false);
        }
    }

    @FXML
    private void handleAddFaceId() {
        User targetUser = userBox.getValue();
        if (targetUser == null) {
            setMessage("Veuillez selectionner un utilisateur.", false);
            return;
        }

        try {
            Path captured = openCameraPreviewDialog();
            if (captured != null) {
                String result = userService.addOrUpdateFaceIdByAdmin(
                        UserSession.getCurrentUserRole(),
                        targetUser.getEmail(),
                        captured);
                
                boolean success = result.startsWith("Face ID ajoute") || result.startsWith("Face ID mis a jour");
                setMessage(result, success);
                refreshFaceStatus(targetUser);

                // Clean up capture
                try {
                    Files.deleteIfExists(captured);
                } catch (IOException ignored) {}
            }
        } catch (IllegalStateException ex) {
            setMessage(ex.getMessage(), false);
        }
    }

    @FXML
    private void handleDeleteFaceId() {
        User targetUser = userBox.getValue();
        if (targetUser == null) {
            setMessage("Veuillez selectionner un utilisateur.", false);
            return;
        }

        String result = userService.deleteFaceIdByAdmin(UserSession.getCurrentUserRole(), targetUser.getEmail());
        boolean success = result.startsWith("Face ID supprime");
        setMessage(result, success);
        refreshFaceStatus(targetUser);
    }

    @FXML
    private void goBack() {
        SceneNavigator.navigate(userBox, "/Dashboard.fxml", message -> setMessage(message, false));
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

        final Path[] capturedPath = { null };

        // --- Construction de la fenêtre de preview ---
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(userBox.getScene().getWindow());
        dialog.setTitle("Face ID - Capture visage");
        dialog.setResizable(false);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(46, 125, 50, 0.25), 16, 0.2, 0, 4);");

        Label instructionLabel = new Label("Positionnez le visage face a la camera");
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
        final boolean[] running = { true };

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
        webcamThread.setName("webcam-admin-preview");
        webcamThread.start();

        // --- Actions boutons ---
        captureBtn.setOnAction(e -> {
            running[0] = false;
            BufferedImage snapshot = webcam.getImage();
            if (snapshot != null) {
                try {
                    Path tempImage = Files.createTempFile("face-admin-", ".jpg");
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

    private void refreshFaceStatus(User user) {
        if (user == null) {
            faceIdStatusLabel.setText("Aucun utilisateur selectionne.");
            addFaceIdBtn.setVisible(false);
            addFaceIdBtn.setManaged(false);
            deleteFaceIdBtn.setVisible(false);
            deleteFaceIdBtn.setManaged(false);
            return;
        }

        boolean hasFaceId = userService.hasFaceId(user.getEmail());
        String status = hasFaceId ? "ACTIVE" : "NON CONFIGURE";
        faceIdStatusLabel.setText("Face ID: " + status + " | " + user.getEmail() + " (" + user.getAdminType() + ")");

        // Toggle buttons visibility
        addFaceIdBtn.setVisible(!hasFaceId);
        addFaceIdBtn.setManaged(!hasFaceId);
        deleteFaceIdBtn.setVisible(hasFaceId);
        deleteFaceIdBtn.setManaged(hasFaceId);
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }

    private void setEditingEnabled(boolean enabled) {
        userBox.setDisable(!enabled);
    }
}

