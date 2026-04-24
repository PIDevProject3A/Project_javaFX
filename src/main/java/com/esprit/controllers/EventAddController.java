package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;

@SuppressWarnings("unused")
public class EventAddController {
    private static final String MSG_RED = "-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-font-weight: bold;";

    @FXML
    private TextField nameField, locationField, priceField, typeField, maxPlacesField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Label inputMessageLabel;
    @FXML
    private Button createBtn, cancelBtn;

    private EventService eventService;

    @FXML
    public void initialize() {
        eventService = new EventService();
        nameField.setTooltip(new Tooltip("Exemple : Beach Cleanup"));
        descriptionField.setTooltip(new Tooltip("Décrivez brièvement l'événement."));
        locationField.setTooltip(new Tooltip("Exemple : La Marsa, Tunis..."));
        priceField.setTooltip(new Tooltip("Nombre décimal : 0, 10, 49.99..."));
        typeField.setTooltip(new Tooltip("Valeurs autorisées : FREE ou PAID"));
        maxPlacesField.setTooltip(new Tooltip("Nombre entier positif : 10, 100..."));
    }

    @FXML
    private void handleCreate() {
        if (validateFields()) {
            try {
                String priceRaw = priceField.getText().trim().replace(',', '.');
                String typeRaw = typeField.getText().trim().toUpperCase();
                String maxRaw = maxPlacesField.getText().trim();
                Event event = new Event();
                event.setName(nameField.getText().trim());
                event.setDescription(descriptionField.getText().trim());
                event.setLocation(locationField.getText().trim());
                event.setPrice(Double.parseDouble(priceRaw));
                event.setEventType(typeRaw);
                event.setMaxPlaces(Integer.parseInt(maxRaw));
                event.setEventDate(LocalDateTime.now());
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
        String priceRaw = priceField.getText() != null ? priceField.getText().trim().replace(',', '.') : "";
        String typeRaw = typeField.getText() != null ? typeField.getText().trim().toUpperCase() : "";
        String maxRaw = maxPlacesField.getText() != null ? maxPlacesField.getText().trim() : "";

        if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || priceRaw.isEmpty()
                || typeRaw.isEmpty() || maxRaw.isEmpty()) {
            setInputMessage("Tous les champs sont obligatoires.");
            return false;
        }

        if (!typeRaw.equals("FREE") && !typeRaw.equals("PAID")) {
            setInputMessage("Type invalide : utilisez uniquement FREE ou PAID.");
            return false;
        }

        try {
            double price = Double.parseDouble(priceRaw);
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

