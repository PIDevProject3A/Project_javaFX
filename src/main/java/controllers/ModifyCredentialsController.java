package controllers;

import entities.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;

import java.util.List;

public class ModifyCredentialsController {
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 8;

    @FXML
    private TableView<User> accountsTable;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TextField newFirstNameField;

    @FXML
    private TextField newLastNameField;

    @FXML
    private TextField newEmailField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private TextField newPasswordVisibleField;

    @FXML
    private TextField storedPasswordField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label newFirstNameHintLabel;

    @FXML
    private Label newLastNameHintLabel;

    @FXML
    private Label newPasswordHintLabel;

    @FXML
    private Button saveChangesButton;

    @FXML
    private Button deleteSelectedButton;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private User selectedUser;

    @FXML
    private void initialize() {
        setupAccountsTable();
        setupPasswordVisibilityToggle();
        setupTableSelectionListener();
        setupLiveValidation();

        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            setEditingEnabled(false);
            setMessage("Only admin can modify credentials.", false);
            updateValidationState();
            return;
        }

        setEditingEnabled(true);
        loadEditableUsers();
        updateValidationState();
    }

    @FXML
    private void handleUpdateCredentials() {
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Only admin can modify credentials.", false);
            return;
        }

        if (selectedUser == null) {
            setMessage("Please select an account from the list first.", false);
            return;
        }

        if (!isFormValid()) {
            setMessage("Please fix invalid fields before submitting.", false);
            return;
        }

        String currentSessionEmail = UserSession.getCurrentUserEmail();
        if (currentSessionEmail == null) {
            setMessage("No active session. Please login again.", false);
            return;
        }

        String result = userService.updateCredentialsByAdmin(
                currentRole,
                selectedUser.getEmail(),
                selectedUser.getAdminType(),
                newFirstNameField.getText(),
                newLastNameField.getText(),
                newEmailField.getText(),
                newPasswordField.getText()
        );
        if ("SUCCESS".equals(result)) {
            String updatedEmail = newEmailField.getText() == null ? "" : newEmailField.getText().trim().toLowerCase();
            if (selectedUser.getEmail().equalsIgnoreCase(currentSessionEmail)) {
                UserSession.setCurrentUserEmail(updatedEmail);
            }

            loadEditableUsers();
            selectUserByEmail(updatedEmail);
            setMessage("Profile updated successfully.", true);
            return;
        }

        setMessage(result, false);
    }

    @FXML
    private void handleDeleteSelectedAccount() {
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Only admin can delete accounts.", false);
            return;
        }

        if (selectedUser == null) {
            setMessage("Please select an account to delete.", false);
            return;
        }

        String selectedEmail = selectedUser.getEmail();
        String currentSessionEmail = UserSession.getCurrentUserEmail();
        boolean deleted = userService.deleteAccountByAdmin(selectedEmail);
        if (!deleted) {
            setMessage("Unable to delete account. The last admin cannot be removed.", false);
            return;
        }

        if (selectedEmail != null && selectedEmail.equalsIgnoreCase(currentSessionEmail)) {
            UserSession.clear();
            SceneNavigator.navigate(newEmailField, "/Login.fxml", message -> setMessage(message, false));
            return;
        }

        selectedUser = null;
        clearEditorFields();
        loadEditableUsers();
        setMessage("Account deleted successfully.", true);
    }

    @FXML
    private void goBack() {
        switchScene("/Welcome.fxml");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(newEmailField, fxml, message -> setMessage(message, false));
    }

    private void setupAccountsTable() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()
        ));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdminType().name()));
    }

    private void setupPasswordVisibilityToggle() {
        newPasswordVisibleField.textProperty().bindBidirectional(newPasswordField.textProperty());
        newPasswordVisibleField.managedProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordVisibleField.visibleProperty().bind(showPasswordCheckBox.selectedProperty());
        newPasswordField.managedProperty().bind(showPasswordCheckBox.selectedProperty().not());
        newPasswordField.visibleProperty().bind(showPasswordCheckBox.selectedProperty().not());
    }

    private void setupTableSelectionListener() {
        accountsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectUser(newValue);
            }
        });
    }

    private void loadEditableUsers() {
        List<User> users = userService.getUsersEditableByCurrentUser(UserSession.getCurrentUserRole());
        accountsTable.setItems(FXCollections.observableArrayList(users));

        if (!users.isEmpty()) {
            if (selectedUser != null) {
                selectUserByEmail(selectedUser.getEmail());
                return;
            }

            selectUser(users.get(0));
            return;
        }

        selectedUser = null;
        clearEditorFields();
    }

    private void selectUser(User user) {
        selectedUser = user;
        if (selectedUser == null) {
            clearEditorFields();
            return;
        }

        if (accountsTable.getSelectionModel().getSelectedItem() != selectedUser) {
            accountsTable.getSelectionModel().select(selectedUser);
        }

        newFirstNameField.setText(selectedUser.getFirstName());
        newLastNameField.setText(selectedUser.getLastName());
        newEmailField.setText(selectedUser.getEmail());
        storedPasswordField.setText(resolveStoredPasswordForAdmin(selectedUser));
        newPasswordField.clear();
        showPasswordCheckBox.setSelected(false);
        updateValidationState();
    }

    private void selectUserByEmail(String email) {
        for (User user : accountsTable.getItems()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                selectUser(user);
                return;
            }
        }

        if (!accountsTable.getItems().isEmpty()) {
            selectUser(accountsTable.getItems().get(0));
        }
    }

    private void setEditingEnabled(boolean enabled) {
        accountsTable.setDisable(!enabled);
        newFirstNameField.setDisable(!enabled);
        newLastNameField.setDisable(!enabled);
        newEmailField.setDisable(!enabled);
        storedPasswordField.setDisable(!enabled);
        newPasswordField.setDisable(!enabled);
        newPasswordVisibleField.setDisable(!enabled);
        showPasswordCheckBox.setDisable(!enabled);
        saveChangesButton.setVisible(enabled);
        saveChangesButton.setManaged(enabled);
        deleteSelectedButton.setVisible(enabled);
        deleteSelectedButton.setManaged(enabled);
        deleteSelectedButton.setDisable(!enabled);
        saveChangesButton.setDisable(!enabled || !isFormValid());
    }

    private void setupLiveValidation() {
        newFirstNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        newLastNameField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
        newPasswordField.textProperty().addListener((observable, oldValue, newValue) -> updateValidationState());
    }

    private void updateValidationState() {
        boolean firstNameValid = hasMinLength(newFirstNameField.getText(), MIN_NAME_LENGTH);
        boolean lastNameValid = hasMinLength(newLastNameField.getText(), MIN_NAME_LENGTH);
        boolean passwordValid = isBlank(newPasswordField.getText()) || hasMinLength(newPasswordField.getText(), MIN_PASSWORD_LENGTH);

        updateHintLabel(newFirstNameHintLabel, firstNameValid, "First name must be at least 3 characters.");
        updateHintLabel(newLastNameHintLabel, lastNameValid, "Last name must be at least 3 characters.");
        updateHintLabel(newPasswordHintLabel, passwordValid, "Password must be at least 8 characters.");

        User.AdminType currentRole = UserSession.getCurrentUserRole();
        boolean adminCanEdit = currentRole == User.AdminType.ADMIN_ACCOUNT;
        saveChangesButton.setDisable(!adminCanEdit || !isFormValid());
        deleteSelectedButton.setDisable(!adminCanEdit || selectedUser == null);
    }

    private boolean isFormValid() {
        return hasMinLength(newFirstNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(newLastNameField.getText(), MIN_NAME_LENGTH)
                && hasMinLength(newEmailField.getText(), 5)
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
            return;
        }
        hintLabel.setText("Required - " + requirementText);
        hintLabel.getStyleClass().add("validation-hint-error");
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }

    private void clearEditorFields() {
        newFirstNameField.clear();
        newLastNameField.clear();
        newEmailField.clear();
        storedPasswordField.clear();
        newPasswordField.clear();
        showPasswordCheckBox.setSelected(false);
        updateValidationState();
    }

    private String resolveStoredPasswordForAdmin(User user) {
        if (user == null) {
            return "";
        }

        String storedPassword = user.getPasswordHash();
        if (isBlank(storedPassword)) {
            return "";
        }

        if (isBcryptHash(storedPassword)) {
            return "[encrypted bcrypt hash - cannot decrypt]";
        }

        return storedPassword;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}

