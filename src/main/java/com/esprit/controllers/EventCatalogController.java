package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.services.EventService;
import com.esprit.services.RegistrationService;
import com.esprit.utils.AppSession;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.StyleHelper;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javafx.util.Duration;

/**
 * Vue « public » : inscription via page dédiée (prénom / nom + paiement si payant).
 */
public class EventCatalogController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ListView<Event> eventList;
    @FXML
    private Label tomorrowReminderLabel;

    private final EventService eventService = new EventService();
    private final RegistrationService registrationService = new RegistrationService();
    private Timeline reminderPulse;
    private java.util.Set<Integer> userRegisteredEvents = new java.util.HashSet<>();

    @FXML
    public void initialize() {
        configureListCells();
        loadTomorrowReminder();
        loadEvents();
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) eventList.getScene().getWindow();
            javafx.scene.Scene scene = new Scene(root, 1100, 680);
            StyleHelper.apply(scene);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    @FXML
    private void handleHome() throws IOException {
        if (com.esprit.utils.UserSession.getCurrentUserRole() == com.esprit.entities.User.AdminType.ADMIN_ACCOUNT) {
            switchScene("/Dashboard.fxml");
        } else {
            switchScene("/UserDashboard.fxml");
        }
    }

    @FXML
    private void handleOpenRegistrations() throws IOException {
        switchScene("/com/esprit/RegistrationList.fxml");
    }

    @FXML
    private void handleLogout() throws IOException {
        com.esprit.utils.UserSession.clear();
        com.esprit.utils.AppSession.clearRegistrant();
        switchScene("/Login.fxml");
    }

    private void configureListCells() {
        eventList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label meta = new Label();
            private final VBox textCol = new VBox(6, title, meta);
            private final Button viewBtn = new Button("👁 Voir");
            private final Button regBtn = new Button("✓ S'inscrire");
            private final HBox actions = new HBox(8, viewBtn, regBtn);
            private final Region spacer = new Region();
            private final HBox row = new HBox(12, textCol, spacer, actions);
            private final VBox card = new VBox(row);

            {
                card.getStyleClass().add("bledna-card");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                textCol.setPrefWidth(700);
                title.getStyleClass().add("reg-event-title");
                meta.getStyleClass().add("reg-meta-line");
                meta.setWrapText(true);
                meta.setMaxWidth(700);
                viewBtn.getStyleClass().add("btn-action-view");
                actions.getStyleClass().add("action-buttons");
                actions.setAlignment(Pos.CENTER_RIGHT);
                actions.setPadding(new Insets(4, 0, 0, 0));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(0));
                viewBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        showEventDetails(item);
                    }
                });
                regBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        openRegisterForm(item);
                    }
                });
            }

            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText(item.getName());
                    LocalDateTime d = item.getEventDate();
                    int max = item.getMaxPlaces();
                    String places = max > 0
                            ? (item.getCurrentParticipants() + " / " + max)
                            : (item.getCurrentParticipants() + " / ∞");
                    meta.setText(String.join("  ·  ",
                            d == null ? "—" : d.format(DT),
                            item.getLocation() != null ? item.getLocation() : "—",
                            String.format(Locale.FRANCE, "%.2f TND", item.getPrice()),
                            "Places : " + places,
                            typeLabel(item.getEventType())));
                    boolean full = isFull(item);
                    boolean paid = isPaid(item);
                    boolean registered = userRegisteredEvents.contains(item.getId());
                    
                    regBtn.getStyleClass().removeAll("btn-register", "btn-register-alt", "btn-register-muted");
                    if (registered) {
                        regBtn.setText("✅ Déjà inscrit(e)");
                        regBtn.setDisable(true);
                        regBtn.getStyleClass().add("btn-register-muted");
                    } else if (full) {
                        regBtn.setText("⛔ Complet");
                        regBtn.setDisable(true);
                        regBtn.getStyleClass().add("btn-register-muted");
                    } else {
                        regBtn.setDisable(false);
                        regBtn.getStyleClass().add(paid ? "btn-register" : "btn-register-alt");
                        regBtn.setText(paid ? "💳 Payer / S'inscrire" : "✓ S'inscrire");
                    }
                    setText(null);
                    setGraphic(card);
                }
            }
        });
    }

    private static String typeLabel(String t) {
        if (t == null) {
            return "—";
        }
        return switch (t.trim().toUpperCase(Locale.ROOT)) {
            case "FREE" -> "Gratuit";
            case "PAID" -> "Payant";
            default -> t;
        };
    }

    private static boolean isPaid(Event e) {
        return e.getEventType() != null && e.getEventType().equalsIgnoreCase("PAID");
    }

    private static boolean isFull(Event e) {
        int max = e.getMaxPlaces();
        if (max <= 0) {
            return false;
        }
        return e.getCurrentParticipants() >= max;
    }

    private void loadEvents() {
        try {
            int currentUserId = AppSession.getCurrentUserId();
            userRegisteredEvents = registrationService.listerFiltre(currentUserId, null, null, null, null)
                                                      .stream()
                                                      .map(com.esprit.entities.Registration::getEventId)
                                                      .collect(Collectors.toSet());

            List<Event> events = eventService.afficher();
            eventList.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void loadTomorrowReminder() {
        if (tomorrowReminderLabel == null) {
            return;
        }
        try {
            List<String> reminders = registrationService.remindersForTomorrow(AppSession.getCurrentUserId());
            if (reminders.isEmpty()) {
                stopReminderAnimation();
                tomorrowReminderLabel.setManaged(false);
                tomorrowReminderLabel.setVisible(false);
                tomorrowReminderLabel.setText("");
                return;
            }
            String message = reminders.size() == 1
                    ? "Rappel : votre événement aura lieu demain : " + reminders.get(0)
                    : "Rappel : vos événements de demain : " + reminders.stream().collect(Collectors.joining("  |  "));
            tomorrowReminderLabel.setText("🔔 " + message);
            tomorrowReminderLabel.setManaged(true);
            tomorrowReminderLabel.setVisible(true);
            tomorrowReminderLabel.setStyle("-fx-background-color:#ffebee; -fx-text-fill:#b71c1c; -fx-font-weight:bold; -fx-padding:10 12; -fx-background-radius:8; -fx-border-color:#ef9a9a; -fx-border-radius:8;");
            startReminderAnimation();
        } catch (SQLException e) {
            stopReminderAnimation();
            tomorrowReminderLabel.setManaged(false);
            tomorrowReminderLabel.setVisible(false);
        }
    }

    private void startReminderAnimation() {
        stopReminderAnimation();
        reminderPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(tomorrowReminderLabel.opacityProperty(), 1.0),
                        new KeyValue(tomorrowReminderLabel.scaleXProperty(), 1.0),
                        new KeyValue(tomorrowReminderLabel.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(650),
                        new KeyValue(tomorrowReminderLabel.opacityProperty(), 0.55),
                        new KeyValue(tomorrowReminderLabel.scaleXProperty(), 1.015),
                        new KeyValue(tomorrowReminderLabel.scaleYProperty(), 1.015)),
                new KeyFrame(Duration.millis(1300),
                        new KeyValue(tomorrowReminderLabel.opacityProperty(), 1.0),
                        new KeyValue(tomorrowReminderLabel.scaleXProperty(), 1.0),
                        new KeyValue(tomorrowReminderLabel.scaleYProperty(), 1.0))
        );
        reminderPulse.setCycleCount(Timeline.INDEFINITE);
        reminderPulse.play();
    }

    private void stopReminderAnimation() {
        if (reminderPulse != null) {
            reminderPulse.stop();
            reminderPulse = null;
        }
    }

    private void openRegisterForm(Event event) {
        if (isFull(event)) {
            showError("Cet événement est complet.");
            return;
        }
        try {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.hide();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/RegisterEventForm.fxml"));
            Stage st = new Stage();
            st.setTitle("bledna — inscription : " + event.getName());
            Scene sc = new Scene(loader.load(), 560, formHeight(event));
            StyleHelper.apply(sc);
            st.setScene(sc);
            if (owner != null) {
                st.initOwner(owner);
            }
            RegisterEventFormController c = loader.getController();
            c.setEvent(event);
            st.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showError("Impossible d'ouvrir le formulaire : " + e.getMessage());
        } finally {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.show();
            }
        }
    }

    private static int formHeight(Event event) {
        boolean paid = event.getEventType() != null && event.getEventType().equalsIgnoreCase("PAID");
        return paid ? 700 : 600;
    }

    private void showEventDetails(Event event) {
        LocalDateTime d = event.getEventDate();
        String body = String.join("\n",
                "Nom : " + event.getName(),
                "Description : " + (event.getDescription() != null ? event.getDescription() : "—"),
                "Date : " + (d == null ? "—" : d.format(DT)),
                "Lieu : " + (event.getLocation() != null ? event.getLocation() : "—"),
                String.format(Locale.FRANCE, "Prix : %.2f TND", event.getPrice()),
                "Type : " + typeLabel(event.getEventType()),
                "Places : " + event.getCurrentParticipants() + " / " + (event.getMaxPlaces() > 0 ? event.getMaxPlaces() : "∞")
        );
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(event.getName());
        a.setContentText(body);
        a.showAndWait();
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(m);
        a.showAndWait();
    }
}

