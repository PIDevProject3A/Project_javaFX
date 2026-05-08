package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.services.EventService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class EventController {
    @FXML
    private ListView<Event> eventList;
    @FXML
    private TextField nameField;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField typeField;
    @FXML
    private Button addBtn, updateBtn, deleteBtn, refreshBtn, clearBtn;

    private EventService eventService;
    private Event selectedEvent = null;

    @FXML
    public void initialize() {
        eventService = new EventService();
        configureListCells();
        loadEvents();

        eventList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedEvent = newVal;
            if (newVal != null) {
                nameField.setText(newVal.getName());
                descriptionField.setText(newVal.getDescription());
                locationField.setText(newVal.getLocation());
                priceField.setText(String.valueOf(newVal.getPrice()));
                typeField.setText(newVal.getEventType());
            }
        });
    }

    private void configureListCells() {
        eventList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String desc = item.getDescription() != null ? item.getDescription() : "—";
                    if (desc.length() > 60) {
                        desc = desc.substring(0, 57) + "…";
                    }
                    setText(String.format(Locale.FRANCE,
                            "%s  |  %s  |  %s  |  %.2f  |  %s",
                            item.getName() != null ? item.getName() : "—",
                            desc,
                            item.getLocation() != null ? item.getLocation() : "—",
                            item.getPrice(),
                            item.getEventType() != null ? item.getEventType() : "—"));
                    setGraphic(null);
                }
            }
        });
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.afficher();
            ObservableList<Event> observableList = FXCollections.observableArrayList(events);
            eventList.setItems(observableList);
        } catch (SQLException e) {
            showError("Error loading events: " + e.getMessage());
        }
    }

    @FXML
    private void handleAdd() {
        if (validateFields()) {
            try {
                Event event = new Event();
                event.setName(nameField.getText());
                event.setDescription(descriptionField.getText());
                event.setLocation(locationField.getText());
                event.setPrice(Double.parseDouble(priceField.getText()));
                event.setEventType(typeField.getText());
                event.setEventDate(LocalDateTime.now());
                event.setPaymentType("CASH");
                event.setMaxPlaces(100);

                eventService.ajouter(event);
                showInfo("✅ Event added successfully!");
                handleClear();
                loadEvents();
            } catch (SQLException e) {
                showError("Error adding event: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedEvent == null) {
            showError("Please select an event to edit!");
            return;
        }
        if (validateFields()) {
            try {
                selectedEvent.setName(nameField.getText());
                selectedEvent.setDescription(descriptionField.getText());
                selectedEvent.setLocation(locationField.getText());
                selectedEvent.setPrice(Double.parseDouble(priceField.getText()));
                selectedEvent.setEventType(typeField.getText());

                eventService.modifier(selectedEvent);
                showInfo("✅ Event updated successfully!");
                handleClear();
                loadEvents();
                selectedEvent = null;
            } catch (SQLException e) {
                showError("Error during update: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedEvent == null) {
            showError("Please select an event to delete!");
            return;
        }
        try {
            eventService.supprimer(selectedEvent.getId());
            showInfo("✅ Event deleted successfully!");
            handleClear();
            loadEvents();
            selectedEvent = null;
        } catch (SQLException e) {
            showError("Error during deletion: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
        showInfo("✅ List refreshed!");
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        descriptionField.clear();
        locationField.clear();
        priceField.clear();
        typeField.clear();
        selectedEvent = null;
        eventList.getSelectionModel().clearSelection();
    }

    private boolean validateFields() {
        if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty() ||
            locationField.getText().isEmpty() || priceField.getText().isEmpty() ||
            typeField.getText().isEmpty()) {
            showError("All fields are required!");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
        } catch (NumberFormatException e) {
            showError("Price must be a number!");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

