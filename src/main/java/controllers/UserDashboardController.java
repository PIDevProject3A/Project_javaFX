package controllers;

import entities.AppUser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import utils.MyDataBase;
import utils.SceneNavigator;
import utils.UserSession;

public class UserDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label emailLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label roleBadgeLabel;

    @FXML
    public void initialize() {
        String email = UserSession.getCurrentUserEmail();
        AppUser.UserType userType = UserSession.getCurrentAppUserType();

        if (email == null || userType == null) {
            welcomeLabel.setText("Session expired. Please login again.");
            return;
        }

        AppUser appUser = MyDataBase.getInstance().findAppUserByEmail(email);
        if (appUser != null) {
            welcomeLabel.setText("Welcome, " + appUser.getFirstName() + " " + appUser.getLastName() + "!");
            nameLabel.setText(appUser.getFirstName() + " " + appUser.getLastName());
            emailLabel.setText(appUser.getEmail());
            roleLabel.setText(appUser.getUserType().name());
            roleBadgeLabel.setText(appUser.getUserType().name());
            roleBadgeLabel.getStyleClass().add("role-badge-" + appUser.getUserType().name().toLowerCase());
        } else {
            welcomeLabel.setText("Welcome!");
            nameLabel.setText(email);
            emailLabel.setText(email);
            roleLabel.setText(userType.name());
            roleBadgeLabel.setText(userType.name());
        }
    }

    @FXML
    private void handleDeleteAccount() {
        String email = UserSession.getCurrentUserEmail();
        if (email == null) return;

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure you want to delete your account?");
        alert.setContentText("This action cannot be undone. All your data will be permanently removed.");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                boolean deleted = MyDataBase.getInstance().deleteAppUserByEmail(email);
                if (deleted) {
                    UserSession.clear();
                    switchScene("/Login.fxml");
                }
            }
        });
    }

    @FXML
    private void logout() {
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            MyDataBase.getInstance().updateLogoutTime(logId);
        }
        UserSession.clear();
        switchScene("/Login.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(welcomeLabel, fxml, msg -> {});
    }
}
