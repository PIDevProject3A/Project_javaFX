package com.esprit.controllers;

import com.esprit.entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.esprit.services.UserService;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;

import java.util.Optional;

public class AdminSettingsController {
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TextField newPasswordVisibleField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label firstNameHintLabel;

    @FXML
    private Label lastNameHintLabel;

    @FXML
    private Label newPasswordHintLabel;

    @FXML
    private Button saveChangesButton;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    private void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            setEditingEnabled(false);
            return;
        }

        loadUserData();
        setupPasswordVisibilityToggle();
        setupLiveValidation();
    }

    private void loadUserData() {
        String currentEmail = UserSession.getCurrentUserEmail();
        if (currentEmail == null) return;
        
        currentUser = userService.findByEmail(currentEmail);
        if (currentUser == null) {
            setMessage("Error: Unable to load user data.", false);
            return;
        }

        firstNameField.setText(currentUser.getFirstName());
        lastNameField.setText(currentUser.getLastName());
        emailField.setText(currentUser.getEmail());
        newPasswordField.clear();
        showPasswordCheckBox.setSelected(false);
        updateValidationState();
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) return;
        
        if (!isFormValid()) {
            setMessage("Please fix invalid fields before submitting.", false);
            return;
        }

        String currentSessionEmail = UserSession.getCurrentUserEmail();
        String result = userService.updateCredentialsByAdmin(
                UserSession.getCurrentUserRole(),
                currentUser.getEmail(),
                currentUser.getAdminType(),
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                newPasswordField.getText()
        );
        
        if ("SUCCESS".equals(result)) {
            String updatedEmail = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            UserSession.setCurrentUserEmail(updatedEmail);
            setMessage("Profile updated successfully.", true);
            loadUserData(); // Reload to reflect any other changes
        } else {
            setMessage(result, false);
        }
    }

    @FXML
    private void handleFaceId() {
        if (currentUser != null) {
            UserSession.setUserToEdit(currentUser); // Pass self to face ID management if needed
            switchScene("/FaceIdAdminManagement.fxml");
        }
    }

    @FXML
    private void handleDeleteAccount() {
        if (currentUser == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Delete your account (" + currentUser.getEmail() + ")?");
        alert.setContentText("Are you sure you want to delete your own account? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = userService.deleteAccountByAdmin(currentUser.getEmail());
            if (deleted) {
                UserSession.clear();
                switchScene("/Login.fxml");
            } else {
                setMessage("Unable to delete account. The last admin cannot be removed.", false);
            }
        }
    }

    @FXML
    private void goToOverview() {
        switchScene("/Dashboard.fxml");
    }

    @FXML
    private void goToManageAccounts() {
        switchScene("/AdminDashboard.fxml");
    }

    @FXML
    private void refresh() {
        loadUserData();
        setMessage("Settings refreshed.", true);
    }

    @FXML
    private void logout() {
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            com.esprit.utils.MyDataBase.getInstance().updateLogoutTime(logId);
        }
        UserSession.clear();
        switchScene("/Login.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(emailField, fxml, message -> setMessage(message, false));
    }

    private void setupPasswordVisibilityToggle() {
        newPasswordVisibleField.textProperty().bindBidirectional(newPasswordField.textProperty());
        newPasswordVisibleField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordVisibleField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        newPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());
    }

    private void setEditingEnabled(boolean enabled) {
        firstNameField.setDisable(!enabled);
        lastNameField.setDisable(!enabled);
        emailField.setDisable(!enabled);
        newPasswordField.setDisable(!enabled);
        newPasswordVisibleField.setDisable(!enabled);
        showPasswordCheckBox.setDisable(!enabled);
        saveChangesButton.setDisable(!enabled);
    }

    private void setupLiveValidation() {
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        emailField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
    }

    private void updateValidationState() {
        boolean firstNameValid = hasMinLength(firstNameField.getText(), MIN_NAME_LENGTH);
        boolean lastNameValid = hasMinLength(lastNameField.getText(), MIN_NAME_LENGTH);
        boolean passwordValid = isBlank(newPasswordField.getText()) || hasMinLength(newPasswordField.getText(), MIN_PASSWORD_LENGTH);
        boolean emailValid = hasMinLength(emailField.getText(), 5) && emailField.getText().contains("@");

        updateHintLabel(firstNameHintLabel, firstNameValid, "First name must be at least 3 characters.");
        updateHintLabel(lastNameHintLabel, lastNameValid, "Last name must be at least 3 characters.");
        updateHintLabel(newPasswordHintLabel, passwordValid, "Password must be at least 8 characters.");

        saveChangesButton.setDisable(!isFormValid());
    }

    private boolean isFormValid() {
        return hasMinLength(firstNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(lastNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(emailField.getText(), 5) && emailField.getText().contains("@")
                && (isBlank(newPasswordField.getText()) || hasMinLength(newPasswordField.getText(), MIN_PASSWORD_LENGTH));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasMinLength(String value, int minLength) {
        return value != null && value.trim().length() >= minLength;
    }

    private void updateHintLabel(Label hintLabel, boolean isValid, String requirementText) {
        hintLabel.getStyleClass().removeAll("validation-hint-ok", "validation-hint-error");
        if (isValid) {
            hintLabel.setText("OK - " + requirementText);
            hintLabel.getStyleClass().add("validation-hint-ok");
        } else {
            hintLabel.setText("Required - " + requirementText);
            hintLabel.getStyleClass().add("validation-hint-error");
        }
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }
}

