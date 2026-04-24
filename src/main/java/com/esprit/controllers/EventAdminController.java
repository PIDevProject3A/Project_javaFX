package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import com.esprit.Services.RegistrationService;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.StyleHelper;
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
import java.util.Map;
import java.util.Optional;

public class EventAdminController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ListView<Event> eventList;
    @FXML
    private Button addBtn;

    private final EventService eventService = new EventService();
    private final RegistrationService registrationService = new RegistrationService();

    @FXML
    public void initialize() {
        configureListCells();
        loadEvents();
    }

    @FXML
    private void handleHome() throws IOException {
        NavigationManager.navigateTo("Home.fxml");
    }

    private void configureListCells() {
        eventList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label meta = new Label();
            private final Label desc = new Label();
            private final Label registrants = new Label();
            private final VBox textCol = new VBox(4, title, meta, desc, registrants);
            private final Button viewBtn = new Button("👁 Voir");
            private final Button editBtn = new Button("✏ Modifier");
            private final Button delBtn = new Button("🗑 Supprimer");
            private final HBox actions = new HBox(8, viewBtn, editBtn, delBtn);
            private final Region spacer = new Region();
            private final HBox row = new HBox(12, textCol, spacer, actions);
            private final VBox card = new VBox(row);

            {
                card.getStyleClass().add("bledna-card");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                textCol.setPrefWidth(720);
                title.getStyleClass().add("reg-event-title");
                meta.getStyleClass().add("reg-meta-line");
                desc.getStyleClass().add("reg-meta-line");
                desc.setWrapText(true);
                desc.setMaxWidth(720);
                registrants.getStyleClass().add("reg-meta-line");
                registrants.setWrapText(true);
                registrants.setMaxWidth(720);
                viewBtn.getStyleClass().add("btn-action-view");
                editBtn.getStyleClass().add("btn-action-edit");
                delBtn.getStyleClass().add("btn-action-delete");
                actions.getStyleClass().add("action-buttons");
                actions.setAlignment(Pos.TOP_RIGHT);
                actions.setPadding(new Insets(4, 0, 0, 0));
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(0));
                viewBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        showDetails(item);
                    }
                });
                editBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        openEdit(item);
                    }
                });
                delBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        confirmDelete(item);
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
                    title.setText(item.getName() != null ? item.getName() : "—");
                    LocalDateTime d = item.getEventDate();
                    int max = item.getMaxPlaces();
                    String places = max > 0
                            ? (item.getCurrentParticipants() + " / " + max)
                            : (item.getCurrentParticipants() + " / ∞");
                    meta.setText(String.join("  ·  ",
                            d == null ? "—" : d.format(DT),
                            item.getLocation() != null ? item.getLocation() : "—",
                            String.format(Locale.FRANCE, "%.2f TND", item.getPrice()),
                            item.getEventType() != null ? item.getEventType() : "—",
                            "Places : " + places));
                    desc.setText(shortDesc(item.getDescription()));
                    String reg = item.getRegistrantsSummary() != null ? item.getRegistrantsSummary() : "—";
                    registrants.setText("👥 Inscrits : " + reg);
                    setText(null);
                    setGraphic(card);
                }
            }
        });
    }

    private static String shortDesc(String d) {
        if (d == null || d.isBlank()) {
            return "—";
        }
        String t = d.trim().replaceAll("\\s+", " ");
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.afficher();
            Map<Integer, String> summaries = registrationService.loadRegistrantSummariesByEvent();
            for (Event e : events) {
                e.setRegistrantsSummary(summaries.getOrDefault(e.getId(), "—"));
            }
            eventList.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void showDetails(Event event) {
        LocalDateTime d = event.getEventDate();
        String body = String.join("\n",
                "Nom : " + event.getName(),
                "Description : " + (event.getDescription() != null ? event.getDescription() : "—"),
                "Date : " + (d == null ? "—" : d.format(DT)),
                "Lieu : " + (event.getLocation() != null ? event.getLocation() : "—"),
                String.format(Locale.FRANCE, "Prix : %.2f TND", event.getPrice()),
                "Type : " + (event.getEventType() != null ? event.getEventType() : "—"),
                "Paiement (événement) : " + (event.getPaymentType() != null ? event.getPaymentType() : "—"),
                "Places : " + event.getCurrentParticipants() + " / " + (event.getMaxPlaces() > 0 ? event.getMaxPlaces() : "∞"),
                "Statut : " + (event.getStatus() != null ? event.getStatus() : "—"),
                "Inscrits : " + (event.getRegistrantsSummary() != null ? event.getRegistrantsSummary() : "—")
        );
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(event.getName());
        a.setContentText(body);
        a.showAndWait();
    }

    private void openEdit(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/EventEdit.fxml"));
            Stage st = new Stage();
            st.setTitle("bledna — modifier l'événement");
            Scene sc = new Scene(loader.load(), 820, 620);
            StyleHelper.apply(sc);
            st.setScene(sc);
            loader.<EventEditController>getController().setEventToEdit(event);
            st.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void confirmDelete(Event event) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setContentText("Supprimer « " + event.getName() + " » ?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                eventService.supprimer(event.getId());
                loadEvents();
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/EventAdd.fxml"));
            Stage st = new Stage();
            st.setTitle("bledna — nouvel événement");
            Scene sc = new Scene(loader.load(), 820, 620);
            StyleHelper.apply(sc);
            st.setScene(sc);
            st.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(m);
        a.showAndWait();
    }
}
