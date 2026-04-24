package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

@SuppressWarnings("unused")
public class EventEditController {
    @FXML
    private TextField nameField, locationField, priceField, typeField, maxPlacesField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button saveBtn, cancelBtn;

    private EventService eventService;
    private Event eventToEdit;

    public void setEventToEdit(Event event) {
        this.eventToEdit = event;
        loadEventData();
    }

    @FXML
    public void initialize() {
        eventService = new EventService();
    }

    private void loadEventData() {
        if (eventToEdit != null) {
            nameField.setText(eventToEdit.getName());
            descriptionField.setText(eventToEdit.getDescription());
            locationField.setText(eventToEdit.getLocation());
            priceField.setText(String.valueOf(eventToEdit.getPrice()));
            typeField.setText(eventToEdit.getEventType());
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
                eventToEdit.setEventType(typeField.getText());
                eventToEdit.setMaxPlaces(Integer.parseInt(maxPlacesField.getText()));

                eventService.modifier(eventToEdit);
                showInfo("✅ Événement modifié avec succès !");
                closeWindow();
            } catch (SQLException e) {
                showError("Erreur lors de la modification: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    private boolean validateFields() {
        if (nameField.getText().isEmpty() || descriptionField.getText().isEmpty() ||
            locationField.getText().isEmpty() || priceField.getText().isEmpty() ||
            typeField.getText().isEmpty() || maxPlacesField.getText().isEmpty()) {
            showError("Tous les champs sont obligatoires !");
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
            Integer.parseInt(maxPlacesField.getText());
        } catch (NumberFormatException e) {
            showError("Prix et Max places doivent être des nombres !");
            return false;
        }
        return true;
    }


    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

