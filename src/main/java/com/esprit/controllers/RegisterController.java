package com.esprit.controllers;

import com.esprit.entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.esprit.services.UserService;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;

public class RegisterController {
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label firstNameHintLabel;

    @FXML
    private Label lastNameHintLabel;

    @FXML
    private Label passwordHintLabel;

    @FXML
    private ComboBox<User.AdminType> adminTypeBox;

    @FXML
    private Button registerButton;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
        adminTypeBox.setItems(FXCollections.observableArrayList(User.AdminType.values()));
        adminTypeBox.setValue(User.AdminType.EVENT_MANAGER);
        setMessage("Public registration is disabled. Login with an existing account.", false);
        setupLiveValidation();
        updateValidationState();
    }

    @FXML
    private void handleRegister() {
        String currentEmail = UserSession.getCurrentUserEmail();
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentEmail == null || currentRole == null) {
            setMessage("Login as admin to create accounts.", false);
            return;
        }

        if (!isFormValid()) {
            setMessage("Please fix invalid fields before submitting.", false);
            return;
        }

        String result = userService.createAccountByAdmin(
                currentRole,
                firstNameField.getText(),
                lastNameField.getText(),
                emailField.getText(),
                passwordField.getText(),
                adminTypeBox.getValue()
        );

        if ("SUCCESS".equals(result)) {
            setMessage("Account created successfully.", true);
            clearFields();
            return;
        }

        setMessage(result, false);
    }

    @FXML
    private void goToLogin() {
        switchScene("/Login.fxml");
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        adminTypeBox.setValue(User.AdminType.EVENT_MANAGER);
        updateValidationState();
    }

    private void setupLiveValidation() {
        firstNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        lastNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
    }

    private void updateValidationState() {
        boolean firstNameValid = hasMinLength(firstNameField.getText(), MIN_NAME_LENGTH);
        boolean lastNameValid = hasMinLength(lastNameField.getText(), MIN_NAME_LENGTH);
        boolean passwordValid = hasMinLength(passwordField.getText(), MIN_PASSWORD_LENGTH);

        updateHintLabel(firstNameHintLabel, firstNameValid, "First name must be at least 3 characters.");
        updateHintLabel(lastNameHintLabel, lastNameValid, "Last name must be at least 3 characters.");
        updateHintLabel(passwordHintLabel, passwordValid, "Password must be at least 8 characters.");

        registerButton.setDisable(!isFormValid());
    }

    private boolean isFormValid() {
        return hasMinLength(firstNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(lastNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(passwordField.getText(), MIN_PASSWORD_LENGTH);
    }

    private boolean hasMinLength(String value, int minLength) {
        return value != null && value.trim().length() >= minLength;
    }

    private void updateHintLabel(Label hintLabel, boolean isValid, String requirementText) {
        hintLabel.getStyleClass().removeAll("validation-hint-ok", "validation-hint-error");
        if (isValid) {
            hintLabel.setText("OK - " + requirementText);
            hintLabel.getStyleClass().add("validation-hint-ok");
            return;
        }
        hintLabel.setText("Required - " + requirementText);
        hintLabel.getStyleClass().add("validation-hint-error");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(firstNameField, fxml, message -> setMessage(message, false));
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }
}


