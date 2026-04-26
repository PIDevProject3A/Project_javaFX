package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;

import java.io.File;
import java.util.List;

public class FaceIdAdminManagementController {
    @FXML
    private ComboBox<User> userBox;

    @FXML
    private Label faceIdStatusLabel;

    @FXML
    private Label selectedImageLabel;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            setEditingEnabled(false);
            return;
        }

        List<User> users = userService.getUsersEditableByCurrentUser(UserSession.getCurrentUserRole());
        userBox.setItems(FXCollections.observableArrayList(users));
        userBox.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> refreshFaceStatus(newUser));

        if (!users.isEmpty()) {
            userBox.getSelectionModel().selectFirst();
        } else {
            faceIdStatusLabel.setText("Aucun utilisateur disponible.");
            setEditingEnabled(false);
        }
    }

    @FXML
    private void handlePickImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de visage");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp"
        ));
        File picked = chooser.showOpenDialog(userBox.getScene() == null ? null : userBox.getScene().getWindow());
        if (picked == null) {
            return;
        }
        selectedImageFile = picked;
        selectedImageLabel.setText(picked.getName());
    }

    @FXML
    private void handleAddFaceId() {
        handleUpsertFaceId();
    }

    @FXML
    private void handleModifyFaceId() {
        handleUpsertFaceId();
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
        SceneNavigator.navigate(userBox, "/Welcome.fxml", message -> setMessage(message, false));
    }

    private void handleUpsertFaceId() {
        User targetUser = userBox.getValue();
        if (targetUser == null) {
            setMessage("Veuillez selectionner un utilisateur.", false);
            return;
        }
        if (selectedImageFile == null) {
            setMessage("Veuillez choisir une image avant l'operation.", false);
            return;
        }

        String result = userService.addOrUpdateFaceIdByAdmin(
                UserSession.getCurrentUserRole(),
                targetUser.getEmail(),
                selectedImageFile.toPath()
        );
        boolean success = result.startsWith("Face ID ajoute") || result.startsWith("Face ID mis a jour");
        setMessage(result, success);
        refreshFaceStatus(targetUser);
    }

    private void refreshFaceStatus(User user) {
        if (user == null) {
            faceIdStatusLabel.setText("Aucun utilisateur selectionne.");
            return;
        }

        boolean enabled = userService.hasFaceId(user.getEmail());
        String status = enabled ? "ACTIVE" : "NON CONFIGURE";
        faceIdStatusLabel.setText("Face ID: " + status + " | " + user.getEmail() + " (" + user.getAdminType() + ")");
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

