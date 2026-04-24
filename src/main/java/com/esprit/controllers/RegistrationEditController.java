package com.esprit.controllers;

import com.esprit.entities.Registration;
import com.esprit.Services.RegistrationService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class RegistrationEditController {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label readOnlyAmountLabel;
    @FXML
    private Label readOnlyPaymentLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private DatePicker registrationDatePicker;
    @FXML
    private TextField registrationTimeField;
    @FXML
    private Button saveBtn;
    @FXML
    private Button cancelBtn;

    private final RegistrationService registrationService = new RegistrationService();
    private Registration registration;

    public void setRegistration(Registration registration) {
        this.registration = registration;
        if (registration == null || eventTitleLabel == null) {
            return;
        }
        eventTitleLabel.setText("Événement : " + (registration.getEventName() != null ? registration.getEventName() : "—"));
        readOnlyAmountLabel.setText(String.format(Locale.FRANCE, "Montant : %.2f TND", registration.getAmount()));
        readOnlyPaymentLabel.setText("Paiement : " + labelPayment(registration.getPaymentMethod()));

        firstNameField.setText(registration.getFirstName() != null ? registration.getFirstName() : "");
        lastNameField.setText(registration.getLastName() != null ? registration.getLastName() : "");
        if (registration.getRegistrationDate() != null) {
            LocalDateTime dt = registration.getRegistrationDate();
            registrationDatePicker.setValue(dt.toLocalDate());
            registrationTimeField.setText(dt.truncatedTo(ChronoUnit.MINUTES).toLocalTime().format(HM));
        } else {
            registrationDatePicker.setValue(LocalDate.now());
            registrationTimeField.setText(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(HM));
        }
    }

    @FXML
    public void initialize() {
        // rien d'obligatoire
    }

    @FXML
    private void handleSave() {
        if (registration == null) {
            return;
        }
        String fn = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String ln = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        if (fn.isEmpty() || ln.isEmpty()) {
            showError("Le prénom et le nom sont obligatoires.");
            return;
        }
        LocalDateTime regDt;
        try {
            regDt = parseRegistrationDateTime();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        try {
            registration.setFirstName(fn);
            registration.setLastName(ln);
            registration.setRegistrationDate(regDt);
            registration.setStatus("REGISTERED");
            registrationService.modifier(registration);
            showInfo("Inscription mise à jour (bledna).");
            close();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private LocalDateTime parseRegistrationDateTime() {
        if (registrationDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Choisissez la date d'inscription.");
        }
        LocalTime time = parseTimeFlexible(registrationTimeField.getText());
        return LocalDateTime.of(registrationDatePicker.getValue(), time);
    }

    private static LocalTime parseTimeFlexible(String raw) {
        String t = raw != null ? raw.trim() : "";
        if (t.isEmpty()) {
            return LocalTime.NOON;
        }
        String[] p = t.split(":");
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                throw new IllegalArgumentException("Heure invalide (0–23 pour les heures, 0–59 pour les minutes).");
            }
            return LocalTime.of(h, m);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Heure invalide. Utilisez le format HH:mm (ex. 14:30).");
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage s = (Stage) saveBtn.getScene().getWindow();
        s.close();
    }

    private static String labelPayment(String code) {
        if (code == null) {
            return "—";
        }
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CASH" -> "Espèces";
            case "CARD" -> "Carte bancaire";
            default -> code;
        };
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("bledna");
        a.setContentText(m);
        a.showAndWait();
    }

    private void showInfo(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("bledna");
        a.setContentText(m);
        a.showAndWait();
    }
}
