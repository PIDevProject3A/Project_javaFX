package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.services.EventService;
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

        nameField.setTooltip(new Tooltip("Example: Beach Cleanup"));
        descriptionField.setTooltip(new Tooltip("Briefly describe the event."));
        locationField.setTooltip(new Tooltip("Example: Tunis, etc."));
        priceField.setTooltip(new Tooltip("Decimal number: 0, 10, 49.99..."));
        typeCombo.setTooltip(new Tooltip("Choose: FREE or PAID"));
        maxPlacesField.setTooltip(new Tooltip("Positive integer: 10, 100..."));
        eventDatePicker.setTooltip(new Tooltip("Select the event date"));
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
                showInfo("✅ Event created successfully!");
                closeWindow();
            } catch (SQLException e) {
                showError("Error during creation: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        com.esprit.utils.SceneNavigator.navigate(createBtn, "/com/esprit/EventAdmin.fxml", err -> {
            System.err.println("Navigation error: " + err);
        });
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
            setInputMessage("All fields are required.");
            return false;
        }

        // Validation du prix : optionnel si FREE, obligatoire si PAID
        if ("PAID".equals(typeRaw) && priceRaw.isEmpty()) {
            setInputMessage("Price is required for paid events.");
            return false;
        }

        try {
            double price = priceRaw.isEmpty() ? 0 : Double.parseDouble(priceRaw);
            int maxPlaces = Integer.parseInt(maxRaw);

            if (price < 0) {
                setInputMessage("Price cannot be negative.");
                return false;
            }
            if (maxPlaces <= 0) {
                setInputMessage("Max places must be greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            setInputMessage("Price and Max Places must be valid numbers.");
            return false;
        }
        setInputMessage("Valid input. You can create the event.");
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

