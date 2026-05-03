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
    private Button manageAccountsNavButton;

    @FXML
    private Button manageAccountsShortcutButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Button settingsShortcutButton;

    @FXML
    public void initialize() {
        User.AdminType role = UserSession.getCurrentUserRole();
        String roleLabel = role == null ? "UNKNOWN" : role.name();
        messageLabel.setText("Dashboard Overview\nRole: " + roleLabel);

        boolean isAdmin = role == User.AdminType.ADMIN_ACCOUNT;
        manageAccountsNavButton.setVisible(isAdmin);
        manageAccountsNavButton.setManaged(isAdmin);
        manageAccountsShortcutButton.setVisible(isAdmin);
        manageAccountsShortcutButton.setManaged(isAdmin);
        settingsNavButton.setVisible(isAdmin);
        settingsNavButton.setManaged(isAdmin);
        settingsShortcutButton.setVisible(isAdmin);
        settingsShortcutButton.setManaged(isAdmin);
    }

    @FXML
    private void goToManageAccounts() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            messageLabel.setText("Access denied: admin role required.");
            return;
        }
        switchScene("/AdminDashboard.fxml");
    }

    @FXML
    private void goToSettings() {
        switchScene("/AdminSettings.fxml");
    }

    @FXML
    private void logout() {
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            utils.MyDataBase.getInstance().updateLogoutTime(logId);
        }
        UserSession.clear();
        switchScene("/Login.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(messageLabel, fxml, messageLabel::setText);
    }
}
