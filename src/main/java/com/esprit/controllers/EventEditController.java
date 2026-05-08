package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.services.EventService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

@SuppressWarnings("unused")
public class EventEditController {
    @FXML
    private TextField nameField, locationField, priceField, maxPlacesField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private ComboBox<String> typeCombo;  // ===== CHANGED: ComboBox instead of TextField =====
    @FXML
    private Button saveBtn, cancelBtn;

    private EventService eventService;
    private Event eventToEdit;
    private static Event staticEventToEdit;

    public static void setStaticEventToEdit(Event event) {
        staticEventToEdit = event;
    }

    public void setEventToEdit(Event event) {
        this.eventToEdit = event;
        loadEventData();
    }

    @FXML
    public void initialize() {
        eventService = new EventService();

        // ===== INITIALIZE COMBOBOX =====
        typeCombo.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        typeCombo.getSelectionModel().selectFirst();

        if (staticEventToEdit != null) {
            setEventToEdit(staticEventToEdit);
            staticEventToEdit = null; // Clear it
        }
    }

    private void loadEventData() {
        if (eventToEdit != null) {
            nameField.setText(eventToEdit.getName());
            descriptionField.setText(eventToEdit.getDescription());
            locationField.setText(eventToEdit.getLocation());
            priceField.setText(String.valueOf(eventToEdit.getPrice()));

            // ===== SELECT VALUE IN COMBOBOX =====
            String type = eventToEdit.getEventType();
            if (type != null) {
                if (typeCombo.getItems().contains(type)) {
                    typeCombo.getSelectionModel().select(type);
                } else {
                    typeCombo.getSelectionModel().selectFirst();
                }
            }

            maxPlacesField.setText(String.valueOf(eventToEdit.getMaxPlaces()));
        }
    }

    @FXML
    private void handleSave() {
        if (validateFields()) {
            try {
                eventToEdit.setName(nameField.getText());
                eventToEdit.setDescription(descriptionField.getText());
                eventToEdit.setLocation(locationField.getText());
                eventToEdit.setPrice(Double.parseDouble(priceField.getText()));

                // ===== GET VALUE FROM COMBOBOX =====
                eventToEdit.setEventType(typeCombo.getSelectionModel().getSelectedItem());

                eventToEdit.setMaxPlaces(Integer.parseInt(maxPlacesField.getText()));

                eventService.modifier(eventToEdit);
                showInfo("✅ Event updated successfully!");
                closeWindow();
            } catch (SQLException e) {
                showError("Error during update: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        com.esprit.utils.SceneNavigator.navigate(saveBtn, "/com/esprit/EventAdmin.fxml", err -> {
            System.err.println("Navigation error: " + err);
        });
    }

    private boolean validateFields() {
        if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty() ||
                locationField.getText().isEmpty() || priceField.getText().isEmpty() ||
                typeCombo.getSelectionModel().getSelectedItem() == null || maxPlacesField.getText().isEmpty()) {
            showError("All fields are required!");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
            Integer.parseInt(maxPlacesField.getText());
        } catch (NumberFormatException e) {
            showError("Price and Max Places must be numbers!");
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

