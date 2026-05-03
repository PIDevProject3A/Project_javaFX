package controllers;

import entities.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.UserService;
import utils.SceneNavigator;
import utils.UserSession;
import services.EmailService;
import utils.MyDataBase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Optional;

public class AdminDashboardController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<User> accountsTable;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;


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
    private ObservableList<User> masterData = FXCollections.observableArrayList();

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
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getFirstName() + " " + cellData.getValue().getLastName()
        ));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAdminType().name()));
    }

    private void loadData() {
        List<User> users = userService.getOtherUsersEditableByCurrentUser(UserSession.getCurrentUserRole(), UserSession.getCurrentUserEmail());
        masterData.setAll(users);
    }

    private void setupSearchFilter() {
        FilteredList<User> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
                
                if (fullName.contains(lowerCaseFilter)) {
                    return true; 
                } else if (user.getEmail().toLowerCase().contains(lowerCaseFilter)) {
                    return true; 
                } else if (user.getAdminType().name().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false; 
            });
        });

        SortedList<User> sortedData = new SortedList<>(filteredData);
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
                notifyAdmins(user, "Deconnexion");
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
        String subject = actionType + " utilisateur";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
                "Un utilisateur s'est deconnecte.\n\n" +
                "Nom : %s %s\n" +
                "Email : %s\n" +
                "Date et heure : %s\n" +
                "Type d'action : %s",
                user.getFirstName(), user.getLastName(), user.getEmail(), now, actionType
        );

        // Run in background
        new Thread(() -> emailService.sendEmailToAdmins(subject, message)).start();
    }

    @FXML
    private void handleEditUser() {
        User selectedUser = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        
        UserSession.setUserToEdit(selectedUser);
        switchScene("/UpdateUser.fxml");
    }

    @FXML
    private void handleEmailUser() {
        User selectedUser = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        
        utils.UserSession.setUserToEdit(selectedUser);
        switchScene("/Email.fxml");
    }

    @FXML
    private void handleFaceId() {
        User selectedUser = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;
        
        utils.UserSession.setUserToEdit(selectedUser);
        switchScene("/FaceIdAdminManagement.fxml");
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = accountsTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Delete user " + selectedUser.getEmail() + "?");
        alert.setContentText("Are you sure you want to delete this account? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = userService.deleteAccountByAdmin(selectedUser.getEmail());
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
