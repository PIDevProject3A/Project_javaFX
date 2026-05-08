package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import com.esprit.services.DashboardService;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;
import com.esprit.utils.MyDataBase;
import com.esprit.entities.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
    private Button eventRegistrationButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button donationButton, gpsButton, transactionButton;

    @FXML
    private LineChart<String, Number> loginEvolutionChart;
    @FXML
    private PieChart roleDistributionChart;
    @FXML
    private BarChart<String, Number> dailyActivityChart, eventActivityChart;
    @FXML
    private VBox eventManagerDashboard;

    @FXML
    private StackPane mainContentStack;

    @FXML
    private VBox mainDashboardContent;
    
    @FXML
    private VBox forumContainer;
    
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button communityNavButton;
    @FXML
    private Button logoutButton;

    @FXML
    private Label sidebarTitle, sidebarSubtitle, contentTitle;
    @FXML
    private Label totalUsersTitle, activeUsersTitle, todayLoginsTitle, emailsSentTitle;
    @FXML
    private Label loginChartTitle, rolesChartTitle, activityChartTitle, eventActivityTitle;
    @FXML
    private Label totalEventsTitle, totalEventsLabel, totalRegistrationsTitle, totalRegistrationsLabel;
    @FXML
    private Button btnEn, btnFr, themeToggleButton;
    @FXML
    private Label themeIcon;

    private boolean isDark = false;
    private ResourceBundle bundle;
    private Locale currentLocale = Locale.ENGLISH;

    private boolean isForumLoaded = false;

    private final DashboardService dashboardService = new DashboardService();
    private String currentFxmlPath = null;
    private Button currentActiveBtn = null;

    @FXML
    public void initialize() {
        // Initialize locale from session
        String sessionLocale = UserSession.getCurrentLocale();
        currentLocale = new Locale(sessionLocale);
        bundle = ResourceBundle.getBundle("messages", currentLocale);
        
        // Sync Dark Mode from Session
        isDark = UserSession.isDarkMode();
        
        User.AdminType role = UserSession.getCurrentUserRole();
        boolean isAdmin = (role == User.AdminType.ADMIN_ACCOUNT);

        // UI Setup...
        // Sidebar visibility
        boolean isEventManager = (role == User.AdminType.EVENT_MANAGER);
        
        manageAccountsButton.setVisible(isAdmin);
        manageAccountsButton.setManaged(isAdmin);
        
        if (eventRegistrationButton != null) {
            eventRegistrationButton.setVisible(isAdmin || isEventManager);
            eventRegistrationButton.setManaged(isAdmin || isEventManager);
        }
        
        settingsButton.setVisible(isAdmin);
        settingsButton.setManaged(isAdmin);

        // Show for everyone (Admin & Event Manager)
        dashboardNavButton.setVisible(true);
        dashboardNavButton.setManaged(true);
        
        donationButton.setVisible(isAdmin);
        donationButton.setManaged(isAdmin);
        gpsButton.setVisible(isAdmin);
        gpsButton.setManaged(isAdmin);
        transactionButton.setVisible(isAdmin);
        transactionButton.setManaged(isAdmin);

        // Main content visibility
        statsCardsContainer.setVisible(isAdmin);
        statsCardsContainer.setManaged(isAdmin);
        chartsContainer.setVisible(isAdmin);
        chartsContainer.setManaged(isAdmin);
        
        eventManagerDashboard.setVisible(isEventManager);
        eventManagerDashboard.setManaged(isEventManager);

        nonAdminContainer.setVisible(!isAdmin && !isEventManager);
        nonAdminContainer.setManaged(!isAdmin && !isEventManager);

        if (isAdmin) {
            loadStatistics();
            setupCharts();
        } else if (isEventManager) {
            loadEventStatistics();
            setupEventCharts();
        }

        updateTexts();
        updateLangButtonStyles();
        
        // Wait for scene to apply initial theme if already dark
        javafx.application.Platform.runLater(() -> {
            if (isDark && welcomeLabel.getScene() != null) {
                welcomeLabel.getScene().getRoot().getStyleClass().add("dark-mode");
                themeIcon.setText("\uD83C\uDF1E");
            }
        });

        if (isAdmin) {
            loadStatistics();
            setupLineChart();
            setupPieChart();
            setupBarChart();
            currentActiveBtn = dashboardNavButton;
        } else if (isEventManager) {
            // Default view for Event Manager is now the specialized dashboard
            javafx.application.Platform.runLater(() -> {
                showDashboardView();
                currentActiveBtn = dashboardNavButton;
                updateNavButtons(dashboardNavButton);
            });
        }
    }

    private void updateLangButtonStyles() {
        if (currentLocale.getLanguage().equals("fr")) {
            btnFr.getStyleClass().add("lang-button-active");
            btnEn.getStyleClass().remove("lang-button-active");
        } else {
            btnEn.getStyleClass().add("lang-button-active");
            btnFr.getStyleClass().remove("lang-button-active");
        }
    }

    private void updateTexts() {
        // Sidebar
        sidebarTitle.setText(bundle.getString("sidebar.title"));
        sidebarSubtitle.setText(bundle.getString("sidebar.subtitle"));
        dashboardNavButton.setText(bundle.getString("nav.dashboard"));
        communityNavButton.setText(bundle.getString("nav.community"));
        manageAccountsButton.setText(bundle.getString("nav.accounts"));
        if (eventRegistrationButton != null) eventRegistrationButton.setText(bundle.getString("nav.events"));
        settingsButton.setText(bundle.getString("nav.settings"));
        if (donationButton != null) donationButton.setText(bundle.getString("nav.donation"));
        if (gpsButton != null) gpsButton.setText(bundle.getString("nav.gps"));
        if (transactionButton != null) transactionButton.setText(bundle.getString("nav.transaction"));
        logoutButton.setText(bundle.getString("nav.logout"));

        // Main Content
        contentTitle.setText(bundle.getString("content.title"));
        welcomeLabel.setText(bundle.getString("content.subtitle"));
        
        // Stats
        totalUsersTitle.setText(bundle.getString("stats.total_users"));
        activeUsersTitle.setText(bundle.getString("stats.active_users"));
        todayLoginsTitle.setText(bundle.getString("stats.today_logins"));
        emailsSentTitle.setText(bundle.getString("stats.emails_sent"));

        // Charts
        loginChartTitle.setText(bundle.getString("chart.evolution"));
        rolesChartTitle.setText(bundle.getString("chart.roles"));
        activityChartTitle.setText(bundle.getString("chart.activity"));

        // Event Manager Dashboard
        if (totalEventsTitle != null) totalEventsTitle.setText(bundle.getString("stats.total_events"));
        if (totalRegistrationsTitle != null) totalRegistrationsTitle.setText(bundle.getString("stats.total_registrations"));
        if (eventActivityTitle != null) eventActivityTitle.setText(bundle.getString("chart.event_activity"));
    }

    @FXML
    private void setEnglish() {
        UserSession.setCurrentLocale("en");
        currentLocale = Locale.ENGLISH;
        bundle = ResourceBundle.getBundle("messages", currentLocale);
        updateLangButtonStyles();
        updateTexts();
        if (currentFxmlPath != null) {
            loadIntoContent(currentFxmlPath);
        }
    }

    @FXML
    private void setFrench() {
        UserSession.setCurrentLocale("fr");
        currentLocale = Locale.FRENCH;
        bundle = ResourceBundle.getBundle("messages", currentLocale);
        updateLangButtonStyles();
        updateTexts();
        if (currentFxmlPath != null) {
            loadIntoContent(currentFxmlPath);
        }
    }

    @FXML
    private void toggleTheme() {
        isDark = !isDark;
        UserSession.setDarkMode(isDark);
        Node root = welcomeLabel.getScene().getRoot();
        if (isDark) {
            root.getStyleClass().add("dark-mode");
            themeIcon.setText("\uD83C\uDF1E"); // Sun icon
        } else {
            root.getStyleClass().remove("dark-mode");
            themeIcon.setText("\uD83C\uDF19"); // Moon icon
        }
    }

    private void loadStatistics() {
        totalUsersLabel.setText(String.valueOf(dashboardService.getTotalUsers()));
        activeUsersLabel.setText(String.valueOf(dashboardService.getActiveUsers()));
        todayLoginsLabel.setText(String.valueOf(dashboardService.getTodayLogins()));
        emailsSentLabel.setText(String.valueOf(dashboardService.getEmailCount()));
    }

    private void loadEventStatistics() {
        totalEventsLabel.setText(String.valueOf(dashboardService.getTotalEvents()));
        totalRegistrationsLabel.setText(String.valueOf(dashboardService.getTotalRegistrations()));
    }

    private void setupCharts() {
        setupLineChart();
        setupPieChart();
        setupBarChart();
    }

    private void setupEventCharts() {
        eventActivityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Event Activity");

        Map<LocalDate, Integer> data = dashboardService.getDailyActivity();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        data.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.format(formatter), count));
        });

        eventActivityChart.getData().add(series);
    }

    private void setupLineChart() {
        loginEvolutionChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Logins");
        
        Map<LocalDate, Integer> data = dashboardService.getLoginActivityEvolution();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        
        data.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.format(formatter), count));
        });

        loginEvolutionChart.getData().add(series);
    }

    private void setupPieChart() {
        roleDistributionChart.getData().clear();
        Map<String, Integer> data = dashboardService.getRoleDistribution();
        data.forEach((role, count) -> {
            roleDistributionChart.getData().add(new PieChart.Data(role, count));
        });
    }

    private void setupBarChart() {
        dailyActivityChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Activity");

        Map<LocalDate, Integer> data = dashboardService.getDailyActivity();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        data.forEach((date, count) -> {
            series.getData().add(new XYChart.Data<>(date.format(formatter), count));
        });

        dailyActivityChart.getData().add(series);
    }

    @FXML
    private void showDashboardView() {
        currentFxmlPath = null;
        mainContentStack.getChildren().clear();
        mainContentStack.getChildren().add(mainDashboardContent);
        mainDashboardContent.setVisible(true);
        mainDashboardContent.setManaged(true);
        
        currentActiveBtn = dashboardNavButton;
        updateNavButtons(dashboardNavButton);
    }

    @FXML
    private void showCommunityView() {
        loadIntoContent("/AfficherTopic.fxml");
        updateNavButtons(communityNavButton);
    }

    @FXML
    private void goToManageAccounts() {
        if (UserSession.getCurrentUserRole() != User.AdminType.ADMIN_ACCOUNT) {
            showAccessDenied();
            return;
        }
        loadIntoContent("/AdminDashboard.fxml");
        updateNavButtons(manageAccountsButton);
    }

    @FXML
    private void goToEventRegistrations() {
        User.AdminType role = UserSession.getCurrentUserRole();
        if (role != User.AdminType.ADMIN_ACCOUNT && role != User.AdminType.EVENT_MANAGER) return;
        
        // For Event Manager, this button goes to administration
        if (role == User.AdminType.EVENT_MANAGER) {
            loadIntoContent("/com/esprit/EventAdmin.fxml");
        } else {
            loadIntoContent("/com/esprit/EventCatalog.fxml");
        }
        updateNavButtons(eventRegistrationButton);
    }

    @FXML
    private void goToSettings() {
        loadIntoContent("/AdminSettings.fxml");
        updateNavButtons(settingsButton);
    }

    @FXML
    private void goToDonation() {
        loadIntoContent("/donation.fxml");
    }

    @FXML
    private void goToGps() {
        loadIntoContent("/gps.fxml");
    }

    @FXML
    private void goToTransaction() {
        loadIntoContent("/transaction.fxml");
    }

    private void updateNavButtons(Button activeBtn) {
        dashboardNavButton.getStyleClass().remove("nav-button-active");
        communityNavButton.getStyleClass().remove("nav-button-active");
        manageAccountsButton.getStyleClass().remove("nav-button-active");
        if (eventRegistrationButton != null) eventRegistrationButton.getStyleClass().remove("nav-button-active");
        settingsButton.getStyleClass().remove("nav-button-active");
        
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    private void showAccessDenied() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setContentText("Only the main administrator can access this section.");
        alert.showAndWait();
    }

    private void loadIntoContent(String fxml) {
        try {
            currentFxmlPath = fxml;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setResources(bundle);
            Node node = loader.load();
            
            // Extract center if it's a shell FXML
            if (node instanceof BorderPane) {
                node = ((BorderPane) node).getCenter();
            }
            
            mainContentStack.getChildren().clear();
            mainContentStack.getChildren().add(node);
            
            // Animation
            node.setOpacity(0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), node);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToDeleteAccount() {
        loadIntoContent("/DeleteAccount.fxml");
    }

    @FXML
    private void logout() {
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            MyDataBase.getInstance().updateLogoutTime(logId);
        }
        UserSession.clear();
        SceneNavigator.navigate(logoutButton, "/Login.fxml", msg -> {});
    }
}

