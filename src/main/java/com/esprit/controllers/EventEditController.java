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
    private ComboBox<String> typeCombo;  // ===== CHANGÉ: ComboBox au lieu de TextField =====
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

        // ===== INITIALISER LA COMBOBOX =====
        typeCombo.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        typeCombo.getSelectionModel().selectFirst();
    }

    private void loadEventData() {
        if (eventToEdit != null) {
            nameField.setText(eventToEdit.getName());
            descriptionField.setText(eventToEdit.getDescription());
            locationField.setText(eventToEdit.getLocation());
            priceField.setText(String.valueOf(eventToEdit.getPrice()));

            // ===== SÉLECTIONNER LA VALEUR DANS LA COMBOBOX =====
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

                // ===== RÉCUPÉRER LA VALEUR DE LA COMBOBOX =====
                eventToEdit.setEventType(typeCombo.getSelectionModel().getSelectedItem());

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
                typeCombo.getSelectionModel().getSelectedItem() == null || maxPlacesField.getText().isEmpty()) {
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

