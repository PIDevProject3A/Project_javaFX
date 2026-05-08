package com.esprit.controllers;

import com.esprit.entities.AppUser;
import com.esprit.entities.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.esprit.services.UserService;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;
import com.esprit.services.EmailService;
import com.esprit.utils.MyDataBase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Optional;

/**
 * Unified row for displaying both admin Users and AppUsers in a single table.
 */
class AccountRow {
    private final User adminUser;
    private final AppUser appUser;

    AccountRow(User adminUser) {
        this.adminUser = adminUser;
        this.appUser = null;
    }

    AccountRow(AppUser appUser) {
        this.adminUser = null;
        this.appUser = appUser;
    }

    boolean isAdmin() {
        return adminUser != null;
    }

    String getFirstName() {
        return isAdmin() ? adminUser.getFirstName() : appUser.getFirstName();
    }

    String getLastName() {
        return isAdmin() ? adminUser.getLastName() : appUser.getLastName();
    }

    String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    String getEmail() {
        return isAdmin() ? adminUser.getEmail() : appUser.getEmail();
    }

    String getRole() {
        if (isAdmin()) {
            switch (adminUser.getAdminType()) {
                case ADMIN_ACCOUNT: return "Admin";
                case EVENT_MANAGER: return "Event Manager";
                case FINANCE_MANAGER: return "Finance Manager";
                default: return adminUser.getAdminType().name();
            }
        } else {
            return appUser.getUserType().name();
        }
    }

    String getAccountType() {
        return isAdmin() ? "Admin" : "User";
    }

    User getAdminUser() {
        return adminUser;
    }

    AppUser getAppUser() {
        return appUser;
    }
}

public class AdminDashboardController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<AccountRow> accountsTable;

    @FXML
    private TableColumn<AccountRow, String> nameColumn;

    @FXML
    private TableColumn<AccountRow, String> emailColumn;

    @FXML
    private TableColumn<AccountRow, String> roleColumn;

    @FXML
    private TableColumn<AccountRow, String> typeColumn;

    @FXML
    private Button editButton;

    @FXML
    private Button emailButton;

    @FXML
    private Button faceIdButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label messageLabel;

    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();
    private ObservableList<AccountRow> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());

        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            setMessage("Access denied: admin role required.", false);
            return;
        }

        setupTableColumns();
        loadData();
        setupSearchFilter();
        setupSelectionListener();
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole()));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountType()));
    }

    private void loadData() {
        masterData.clear();

        // Load admin users (exclude current admin)
        List<User> adminUsers = userService.getOtherUsersEditableByCurrentUser(
                UserSession.getCurrentUserRole(), UserSession.getCurrentUserEmail());
        for (User u : adminUsers) {
            masterData.add(new AccountRow(u));
        }

        // Load app users
        List<AppUser> appUsers = userService.getAllAppUsers();
        for (AppUser au : appUsers) {
            masterData.add(new AccountRow(au));
        }
    }

    private void setupSearchFilter() {
        FilteredList<AccountRow> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(row -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (row.getFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (row.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (row.getRole().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (row.getAccountType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        SortedList<AccountRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(accountsTable.comparatorProperty());
        accountsTable.setItems(sortedData);
    }

    private void setupSelectionListener() {
        accountsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean isSelected = (newValue != null);
            editButton.setDisable(!isSelected);
            emailButton.setDisable(!isSelected);
            faceIdButton.setDisable(!isSelected);
            deleteButton.setDisable(!isSelected);
        });
    }

    @FXML
    private void refreshTable() {
        loadData();
        setMessage("User list refreshed.", true);
    }

    @FXML
    private void goToCreateAccount() {
        switchScene("/AdminAccounts.fxml");
    }

    @FXML
    private void goToSettings() {
        switchScene("/AdminSettings.fxml");
    }

    @FXML
    private void goToEventRegistrations() {
        switchScene("/com/esprit/EventCatalog.fxml");
    }

    @FXML
    private void logout() {
        // Update logout time in database
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            MyDataBase.getInstance().updateLogoutTime(logId);
        }

        String email = UserSession.getCurrentUserEmail();
        if (email != null) {
            User user = userService.findByEmail(email);
            if (user != null) {
                notifyAdmins(user, "Logout");
            }
        }
        UserSession.clear();
        switchScene("/Login.fxml");
    }

    @FXML
    private void goToOverview() {
        switchScene("/Dashboard.fxml");
    }

    private void notifyAdmins(User user, String actionType) {
        String subject = actionType + " - User activity";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
                "A user has performed a logout.\n\n" +
                "Name: %s %s\n" +
                "Email: %s\n" +
                "Date and time: %s\n" +
                "Action type: %s",
                user.getFirstName(), user.getLastName(), user.getEmail(), now, actionType
        );

        // Run in background
        new Thread(() -> emailService.sendEmailToAdmins(subject, message)).start();
    }

    @FXML
    private void handleEditUser() {
        AccountRow selectedRow = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        if (selectedRow.isAdmin()) {
            UserSession.setUserToEdit(selectedRow.getAdminUser());
            UserSession.setAppUserToEdit(null);
        } else {
            UserSession.setUserToEdit(null);
            UserSession.setAppUserToEdit(selectedRow.getAppUser());
        }
        switchScene("/UpdateUser.fxml");
    }

    @FXML
    private void handleEmailUser() {
        AccountRow selectedRow = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        if (selectedRow.isAdmin()) {
            UserSession.setUserToEdit(selectedRow.getAdminUser());
            UserSession.setAppUserToEdit(null);
        } else {
            // Create a temporary User object for the email controller compatibility
            UserSession.setUserToEdit(null);
            UserSession.setAppUserToEdit(selectedRow.getAppUser());
        }
        switchScene("/Email.fxml");
    }

    @FXML
    private void handleFaceId() {
        AccountRow selectedRow = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        if (selectedRow.isAdmin()) {
            UserSession.setUserToEdit(selectedRow.getAdminUser());
            UserSession.setAppUserToEdit(null);
        } else {
            UserSession.setUserToEdit(null);
            UserSession.setAppUserToEdit(selectedRow.getAppUser());
        }
        switchScene("/FaceIdAdminManagement.fxml");
    }

    @FXML
    private void handleDeleteUser() {
        AccountRow selectedRow = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Delete user " + selectedRow.getEmail() + "?");
        alert.setContentText("Are you sure you want to delete this account? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted;
            if (selectedRow.isAdmin()) {
                deleted = userService.deleteAccountByAdmin(selectedRow.getEmail());
            } else {
                deleted = userService.deleteAppUserByAdmin(selectedRow.getEmail());
            }

            if (deleted) {
                setMessage("Account deleted successfully.", true);
                loadData();
            } else {
                setMessage("Failed to delete account.", false);
            }
        }
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(searchField, fxml, message -> setMessage(message, false));
    }

    private void setMessage(String message, boolean success) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(success ? "status-success" : "status-error");
        messageLabel.setText(message);
    }
}

