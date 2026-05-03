package controllers;

import entities.AppUser;
import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import services.EmailService;
import services.UserService;
import utils.MyDataBase;
import utils.SceneNavigator;
import utils.UserSession;

public class AdminAccountsController {
    private static final String ROLE_ADMIN = "Admin Account";
    private static final String ROLE_EVENT_MANAGER = "Event Manager";
    private static final String ROLE_FINANCE_MANAGER = "Finance Manager";
    private static final String ROLE_COLLECTOR = "Collector";
    private static final String ROLE_BUYER = "Buyer";
    private static final String ROLE_DONATOR = "Donator";

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleBox;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();

    @FXML
    public void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setEditingEnabled(false);
            setMessage("Access denied: admin role required.", false);
            return;
        }

        roleBox.setItems(FXCollections.observableArrayList(
                ROLE_ADMIN, ROLE_EVENT_MANAGER, ROLE_FINANCE_MANAGER,
                ROLE_COLLECTOR, ROLE_BUYER, ROLE_DONATOR
        ));
        roleBox.setValue(ROLE_COLLECTOR);
    }

    @FXML
    private void handleCreateAccount() {
        User.AdminType currentRole = UserSession.getCurrentUserRole();
        if (currentRole != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            return;
        }

        String selectedRole = roleBox.getValue();
        if (selectedRole == null) {
            setMessage("Please select a role.", false);
            return;
        }

        String plainPassword = passwordField.getText();
        String email = emailField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String result;

        if (isAdminRole(selectedRole)) {
            User.AdminType targetAdminType = mapToAdminType(selectedRole);
            result = userService.createAccountByAdmin(currentRole, firstName, lastName, email, plainPassword, targetAdminType);
        } else {
            AppUser.UserType userType = mapToUserType(selectedRole);
            result = userService.createAppUser(currentRole, firstName, lastName, email, plainPassword, userType);
        }

        if ("SUCCESS".equals(result)) {
            setMessage("Account created successfully. Sending welcome email...", true);

            // Send welcome email in background
            String welcomeEmail = email.trim().toLowerCase();
            String welcomeName = firstName.trim();
            String welcomeRole = selectedRole;
            String welcomePassword = plainPassword;

            new Thread(() -> {
                String subject = "Bienvenue sur BLADNA - Votre compte a ete cree";
                String message = String.format(
                        "Bonjour %s,\n\n" +
                        "Votre compte BLADNA a ete cree avec succes.\n\n" +
                        "Vos identifiants de connexion :\n" +
                        "Email : %s\n" +
                        "Mot de passe : %s\n" +
                        "Role : %s\n\n" +
                        "Veuillez vous connecter et changer votre mot de passe des que possible.\n\n" +
                        "Cordialement,\n" +
                        "L'equipe BLADNA",
                        welcomeName, welcomeEmail, welcomePassword, welcomeRole
                );
                emailService.sendEmail(welcomeEmail, subject, message);
            }).start();

            clearFields();
            return;
        }

        setMessage(result, false);
    }

    @FXML
    private void goBack() {
        switchScene("/AdminDashboard.fxml");
    }

    @FXML
    private void goToDashboard() {
        switchScene("/Dashboard.fxml");
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

    @FXML
    private void goToFaceIdManagement() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            return;
        }
        switchScene("/FaceIdAdminManagement.fxml");
    }

    private boolean isAdminRole(String role) {
        return ROLE_ADMIN.equals(role) || ROLE_EVENT_MANAGER.equals(role) || ROLE_FINANCE_MANAGER.equals(role);
    }

    private User.AdminType mapToAdminType(String role) {
        switch (role) {
            case ROLE_ADMIN: return User.AdminType.ADMIN_ACCOUNT;
            case ROLE_EVENT_MANAGER: return User.AdminType.EVENT_MANAGER;
            case ROLE_FINANCE_MANAGER: return User.AdminType.FINANCE_MANAGER;
            default: return User.AdminType.EVENT_MANAGER;
        }
    }

    private AppUser.UserType mapToUserType(String role) {
        switch (role) {
            case ROLE_COLLECTOR: return AppUser.UserType.Collector;
            case ROLE_BUYER: return AppUser.UserType.Buyer;
            case ROLE_DONATOR: return AppUser.UserType.Donator;
            default: return AppUser.UserType.Collector;
        }
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        roleBox.setValue(ROLE_COLLECTOR);
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
