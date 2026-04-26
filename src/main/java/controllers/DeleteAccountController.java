package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;

public class DeleteAccountController {
    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void handleDeleteAccount() {
        String currentEmail = UserSession.getCurrentUserEmail();
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        boolean deleted = userService.deleteAccount(currentRole, currentEmail);
        if (!deleted) {
            if (currentRole == User.AdminType.ADMIN_ACCOUNT) {
                showError("Cannot delete the last admin. Create another admin first.");
            } else {
                showError("Unable to delete account.");
            }
            return;
        }

        UserSession.clear();
        switchScene("/Login.fxml");
    }

    @FXML
    private void goBack() {
        switchScene("/Welcome.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(messageLabel, fxml, this::showError);
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add("status-error");
        messageLabel.setText(message);
    }
}

