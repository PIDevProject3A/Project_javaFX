package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;

public class UpdateUserController {
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
    private TextField storedPasswordField;

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

    @FXML
    private Label subtitleLabel;

    private final UserService userService = new UserService();
    private User selectedUser;

    @FXML
    private void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
        selectedUser = UserSession.getUserToEdit();
        if (selectedUser == null) {
            setMessage("Error: No user selected.", false);
            setEditingEnabled(false);
            return;
        }

        subtitleLabel.setText("Editing user: " + selectedUser.getEmail());

        setupPasswordVisibilityToggle();
        setupLiveValidation();
        
        loadUserData();
    }

    private void loadUserData() {
        firstNameField.setText(selectedUser.getFirstName());
        lastNameField.setText(selectedUser.getLastName());
        emailField.setText(selectedUser.getEmail());
        storedPasswordField.setText(resolveStoredPasswordForAdmin(selectedUser));
        newPasswordField.clear();
        showPasswordCheckBox.setSelected(false);
        updateValidationState();
    }

    @FXML
    private void handleSave() {
        if (selectedUser == null) return;
        
        if (!isFormValid()) {
            setMessage("Please fix invalid fields before submitting.", false);
            return;
        }

        String result = userService.updateCredentialsByAdmin(
                UserSession.getCurrentUserRole(),
                selectedUser.getEmail(),
                selectedUser.getAdminType(),
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                newPasswordField.getText()
        );
        
        if ("SUCCESS".equals(result)) {
            setMessage("Profile updated successfully.", true);
            // Optionally clear the session user to avoid leaks, but we can also just go back
            // UserSession.setUserToEdit(null);
            goBack();
        } else {
            setMessage(result, false);
        }
    }

    @FXML
    private void goBack() {
        UserSession.setUserToEdit(null);
        switchScene("/AdminDashboard.fxml");
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
        storedPasswordField.setDisable(!enabled);
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

    private String resolveStoredPasswordForAdmin(User user) {
        if (user == null) return "";
        String storedPassword = user.getPasswordHash();
        if (isBlank(storedPassword)) return "";
        if (isBcryptHash(storedPassword)) return "[encrypted bcrypt hash - cannot decrypt]";
        return storedPassword;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
