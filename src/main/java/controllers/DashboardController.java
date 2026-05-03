package controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.DashboardService;
import utils.SceneNavigator;
import utils.UserSession;
import utils.MyDataBase;
import entities.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label activeUsersLabel;
    @FXML
    private Label todayLoginsLabel;
    @FXML
    private Label emailsSentLabel;

    @FXML
    private HBox statsCardsContainer;
    @FXML
    private GridPane chartsContainer;
    @FXML
    private VBox nonAdminContainer;
    @FXML
    private Button manageAccountsButton;
    @FXML
    private Button settingsButton;

    @FXML
    private LineChart<String, Number> loginEvolutionChart;
    @FXML
    private PieChart roleDistributionChart;
    @FXML
    private BarChart<String, Number> dailyActivityChart;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        User.AdminType role = UserSession.getCurrentUserRole();
        boolean isAdmin = (role == User.AdminType.ADMIN_ACCOUNT);

        // Sidebar visibility
        manageAccountsButton.setVisible(isAdmin);
        manageAccountsButton.setManaged(isAdmin);
        // User said "don't show settings for other roles than admin"
        settingsButton.setVisible(isAdmin);
        settingsButton.setManaged(isAdmin);

        // Main content visibility
        statsCardsContainer.setVisible(isAdmin);
        statsCardsContainer.setManaged(isAdmin);
        chartsContainer.setVisible(isAdmin);
        chartsContainer.setManaged(isAdmin);
        nonAdminContainer.setVisible(!isAdmin);
        nonAdminContainer.setManaged(!isAdmin);

        if (isAdmin) {
            welcomeLabel.setText("Real-time monitoring of application activity and user engagement.");
            loadStatistics();
            setupLineChart();
            setupPieChart();
            setupBarChart();
        } else {
            welcomeLabel.setText("Welcome to your personal dashboard.");
        }
    }

    private void loadStatistics() {
        totalUsersLabel.setText(String.valueOf(dashboardService.getTotalUsers()));
        activeUsersLabel.setText(String.valueOf(dashboardService.getActiveUsers()));
        todayLoginsLabel.setText(String.valueOf(dashboardService.getTodayLogins()));
        emailsSentLabel.setText(String.valueOf(dashboardService.getEmailCount()));
    }

    private void setupLineChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Connexions");
        
        Map<LocalDate, Integer> data = dashboardService.getLoginActivityEvolution();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        
        data.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.format(formatter), count));
        });

        loginEvolutionChart.getData().add(series);
    }

    private void setupPieChart() {
        Map<String, Integer> data = dashboardService.getRoleDistribution();
        data.forEach((role, count) -> {
            roleDistributionChart.getData().add(new PieChart.Data(role, count));
        });
    }

    private void setupBarChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Activite Totale");

        Map<LocalDate, Integer> data = dashboardService.getDailyActivity();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        data.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.format(formatter), count));
        });

        dailyActivityChart.getData().add(series);
    }

    @FXML
    private void goToManageAccounts() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            return;
        }
        switchScene(manageAccountsButton, "/AdminAccounts.fxml");
    }

    @FXML
    private void goToSettings() {
        switchScene(settingsButton, "/AdminSettings.fxml");
    }

    @FXML
    private void goToDeleteAccount() {
        switchScene(nonAdminContainer, "/DeleteAccount.fxml");
    }

    @FXML
    private void goToDonation() {
        switchScene(welcomeLabel, "/donation.fxml");
    }

    @FXML
    private void goToGps() {
        switchScene(welcomeLabel, "/gps.fxml");
    }

    @FXML
    private void goToTransaction() {
        switchScene(welcomeLabel, "/transaction.fxml");
    }

    @FXML
    private void logout() {
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            MyDataBase.getInstance().updateLogoutTime(logId);
        }
        UserSession.clear();
        switchScene(totalUsersLabel, "/Login.fxml");
    }

    private void switchScene(javafx.scene.Node source, String fxml) {
        SceneNavigator.navigate(source, fxml, msg -> System.err.println("[Dashboard Error] " + msg));
    }
}
