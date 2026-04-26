package controllers;

import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;

public class AdminAccountsController {
    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<User.AdminType> roleBox;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setEditingEnabled(false);
            setMessage("Access denied: admin role required.", false);
            return;
        }

        roleBox.setItems(FXCollections.observableArrayList(User.AdminType.values()));
        roleBox.setValue(User.AdminType.EVENT_MANAGER);
    }

    @FXML
    private void handleCreateAccount() {
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            return;
        }

        String result = userService.createAccountByAdmin(
                currentRole,
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                roleBox.getValue()
        );

        if ("SUCCESS".equals(result)) {
            setMessage("Account created successfully.", true);
            clearFields();
            return;
        }

        setMessage(result, false);
    }

    @FXML
    private void goBack() {
        switchScene("/Welcome.fxml");
    }

    @FXML
    private void goToFaceIdManagement() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            return;
        }
        switchScene("/FaceIdAdminManagement.fxml");
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        roleBox.setValue(User.AdminType.EVENT_MANAGER);
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(firstNameField, fxml, message -> setMessage(message, false));
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }

    private void setEditingEnabled(boolean enabled) {
        firstNameField.setDisable(!enabled);
        lastNameField.setDisable(!enabled);
        emailField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        roleBox.setDisable(!enabled);
    }
}

