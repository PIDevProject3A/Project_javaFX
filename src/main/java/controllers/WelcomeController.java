package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import utils.SceneNavigator;
import utils.UserSession;

public class WelcomeController {
    @FXML
    private Label messageLabel;

    @FXML
    private Button manageAccountsButton;

    @FXML
    private Button manageAccountsShortcutButton;

    @FXML
    private Button modifyCredentialsNavButton;

    @FXML
    private Button modifyCredentialsButton;

    @FXML
    private Button manageFaceIdButton;

    @FXML
    private Button manageFaceIdShortcutButton;

    @FXML
    public void initialize() {
        User.AdminType role = UserSession.getCurrentUserRole();
        String roleLabel = role == null ? "UNKNOWN" : role.name();
        messageLabel.setText("Welcome to Bladna application\nRole: " + roleLabel);

        boolean isAdmin = role == User.AdminType.ADMIN_ACCOUNT;
        manageAccountsButton.setVisible(isAdmin);
        manageAccountsButton.setManaged(isAdmin);
        manageAccountsShortcutButton.setVisible(isAdmin);
        manageAccountsShortcutButton.setManaged(isAdmin);
        modifyCredentialsNavButton.setVisible(isAdmin);
        modifyCredentialsNavButton.setManaged(isAdmin);
        modifyCredentialsButton.setVisible(isAdmin);
        modifyCredentialsButton.setManaged(isAdmin);
        manageFaceIdButton.setVisible(isAdmin);
        manageFaceIdButton.setManaged(isAdmin);
        manageFaceIdShortcutButton.setVisible(isAdmin);
        manageFaceIdShortcutButton.setManaged(isAdmin);
    }

    @FXML
    private void goToModifyCredentials() {
        switchScene("/ModifyCredentials.fxml");
    }

    @FXML
    private void goToDeleteAccount() {
        switchScene("/DeleteAccount.fxml");
    }

    @FXML
    private void goToManageAccounts() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            messageLabel.setText("Access denied: admin role required.");
            return;
        }
        switchScene("/AdminAccounts.fxml");
    }

    @FXML
    private void goToManageFaceId() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            messageLabel.setText("Access denied: admin role required.");
            return;
        }
        switchScene("/FaceIdAdminManagement.fxml");
    }

    @FXML
    private void logout() {
        UserSession.clear();
        switchScene("/Login.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(messageLabel, fxml, messageLabel::setText);
    }
}

