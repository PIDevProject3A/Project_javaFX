package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;

@SuppressWarnings("unused")
public class EventAddController {
    private static final String MSG_RED = "-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-font-weight: bold;";

    @FXML
    private TextField nameField, locationField, priceField, maxPlacesField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private DatePicker eventDatePicker;  // ✅ NOUVEAU
    @FXML
    private Label inputMessageLabel;
    @FXML
    private Button createBtn, cancelBtn;

    private EventService eventService;

    @FXML
    public void initialize() {
        eventService = new EventService();

        // ===== INITIALISER LA COMBOBOX =====
        typeCombo.setItems(FXCollections.observableArrayList("FREE", "PAID"));
        typeCombo.getSelectionModel().selectFirst();

        // ===== INITIALISER LE DATEPICKER À LA DATE D'AUJOURD'HUI =====
        eventDatePicker.setValue(LocalDate.now());

        // 🔸 AJOUTER LE LISTENER POUR DÉSACTIVER/ACTIVER LE PRIX
        typeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("FREE".equals(newVal)) {
                priceField.setDisable(true);
                priceField.setText("0");
            } else {
                priceField.setDisable(false);
                priceField.setText("");
            }
        });

        nameField.setTooltip(new Tooltip("Exemple : Beach Cleanup"));
        descriptionField.setTooltip(new Tooltip("Décrivez brièvement l'événement."));
        locationField.setTooltip(new Tooltip("Exemple : La Marsa, Tunis..."));
        priceField.setTooltip(new Tooltip("Nombre décimal : 0, 10, 49.99..."));
        typeCombo.setTooltip(new Tooltip("Choisir : FREE ou PAID"));
        maxPlacesField.setTooltip(new Tooltip("Nombre entier positif : 10, 100..."));
        eventDatePicker.setTooltip(new Tooltip("Sélectionnez la date de l'événement"));
    }

    @FXML
    private void handleCreate() {
        if (validateFields()) {
            try {
                String priceRaw = priceField.getText().trim().replace(',', '.');
                String typeRaw = typeCombo.getSelectionModel().getSelectedItem();
                String maxRaw = maxPlacesField.getText().trim();

                Event event = new Event();
                event.setName(nameField.getText().trim());
                event.setDescription(descriptionField.getText().trim());
                event.setLocation(locationField.getText().trim());
                event.setPrice(Double.parseDouble(priceRaw));
                event.setEventType(typeRaw);
                event.setMaxPlaces(Integer.parseInt(maxRaw));

                // ✅ RÉCUPÉRER LA DATE DU DATEPICKER
                LocalDate selectedDate = eventDatePicker.getValue();
                if (selectedDate != null) {
                    event.setEventDate(selectedDate.atStartOfDay()); // 00:00:00
                } else {
                    event.setEventDate(LocalDateTime.now());
                }

                event.setPaymentType("CASH");
                event.setStatus("OPEN");

                eventService.ajouter(event);
                showInfo("✅ Événement créé avec succès !");
                closeWindow();
            } catch (SQLException e) {
                showError("Erreur lors de la création: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) createBtn.getScene().getWindow();
        stage.close();
    }

    private boolean validateFields() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String desc = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String location = locationField.getText() != null ? locationField.getText().trim() : "";
        String typeRaw = typeCombo.getSelectionModel().getSelectedItem();
        String maxRaw = maxPlacesField.getText() != null ? maxPlacesField.getText().trim() : "";
        String priceRaw = priceField.getText() != null ? priceField.getText().trim().replace(',', '.') : "";
        LocalDate date = eventDatePicker.getValue();

        if (name.isEmpty() || desc.isEmpty() || location.isEmpty()
                || typeRaw == null || typeRaw.isEmpty() || maxRaw.isEmpty() || date == null) {
            setInputMessage("Tous les champs sont obligatoires.");
            return false;
        }

        // Validation du prix : optionnel si FREE, obligatoire si PAID
        if ("PAID".equals(typeRaw) && priceRaw.isEmpty()) {
            setInputMessage("Le prix est obligatoire pour un événement payant.");
            return false;
        }

        try {
            double price = priceRaw.isEmpty() ? 0 : Double.parseDouble(priceRaw);
            int maxPlaces = Integer.parseInt(maxRaw);

            if (price < 0) {
                setInputMessage("Le prix ne peut pas être négatif.");
                return false;
            }
            if (maxPlaces <= 0) {
                setInputMessage("Max de places doit être supérieur à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            setInputMessage("Prix et Max de places doivent être des nombres valides.");
            return false;
        }
        setInputMessage("Saisie valide. Vous pouvez créer l'événement.");
        return true;
    }

    private void setInputMessage(String message) {
        if (inputMessageLabel != null) {
            inputMessageLabel.setText(message);
            inputMessageLabel.setStyle(MSG_RED);
        }
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
