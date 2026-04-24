package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.entities.Registration;
import com.esprit.Services.RegistrationService;
import com.esprit.utils.AppSession;
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

public class RegisterEventFormController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label eventTitleLabel;
    @FXML
    private Label eventDetailsLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private DatePicker registrationDatePicker;
    @FXML
    private TextField registrationTimeField;
    @FXML
    private Label fixedAmountLabel;
    @FXML
    private Label fixedPaymentLabel;
    @FXML
    private Button submitBtn;
    @FXML
    private Button cancelBtn;

    private final RegistrationService registrationService = new RegistrationService();
    private Event event;
    private boolean paidEvent;

    public void setEvent(Event event) {
        this.event = event;
        if (event == null || eventTitleLabel == null) {
            return;
        }
        paidEvent = event.getEventType() != null && event.getEventType().equalsIgnoreCase("PAID");
        eventTitleLabel.setText(event.getName());
        LocalDateTime d = event.getEventDate();
        String details = String.join("\n",
                "Date événement : " + (d == null ? "—" : d.format(DT)),
                "Lieu : " + (event.getLocation() != null ? event.getLocation() : "—"),
                "Type : " + (paidEvent ? "Payant" : "Gratuit"),
                "Places : " + event.getCurrentParticipants() + " / " + (event.getMaxPlaces() > 0 ? event.getMaxPlaces() : "∞")
        );
        eventDetailsLabel.setText(details);

        if (paidEvent) {
            fixedAmountLabel.setText(String.format(Locale.FRANCE,
                    "Montant à régler : %.2f TND (fixe, défini avec l'événement)", event.getPrice()));
        } else {
            fixedAmountLabel.setText("Événement gratuit — montant : 0,00 TND");
        }
        fixedPaymentLabel.setText("Mode de paiement : " + labelPaymentFr(paymentMethodFromEvent(event))
                + " (imposé par l'organisateur, non modifiable ici)");

        registrationDatePicker.setValue(LocalDate.now());
        registrationTimeField.setText(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(HM));

        submitBtn.setText(paidEvent ? "✓ Payer et confirmer" : "✓ Confirmer l'inscription");
    }

    @FXML
    public void initialize() {
        if (registrationDatePicker != null) {
            registrationDatePicker.setValue(LocalDate.now());
        }
        if (registrationTimeField != null) {
            registrationTimeField.setText(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(HM));
        }
    }

    @FXML
    private void handleSubmit() {
        if (event == null) {
            return;
        }
        String fn = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String ln = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        if (fn.isEmpty() || ln.isEmpty()) {
            showError("Le prénom et le nom sont obligatoires.");
            return;
        }
        int max = event.getMaxPlaces();
        if (max > 0 && event.getCurrentParticipants() >= max) {
            showError("Cet événement est complet.");
            return;
        }
        LocalDateTime registrationDt;
        try {
            registrationDt = parseRegistrationDateTime();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }
        try {
            if (registrationService.existeInscriptionMemePersonne(
                    event.getId(), AppSession.getCurrentUserId(), fn, ln)) {
                showError("Vous êtes déjà inscrit à cet événement avec ce prénom et ce nom.");
                return;
            }
            Registration r = new Registration();
            r.setUserId(AppSession.getCurrentUserId());
            r.setEventId(event.getId());
            r.setFirstName(fn);
            r.setLastName(ln);
            r.setRegistrationDate(registrationDt);
            r.setAmount(paidEvent ? event.getPrice() : 0);
            r.setPaymentMethod(paymentMethodFromEvent(event));
            r.setStatus("REGISTERED");
            registrationService.ajouter(r);
            AppSession.setRegistrant(fn, ln);
            showInfo(paidEvent ? "Inscription enregistrée (bledna)." : "Inscription enregistrée (bledna).");
            close();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private LocalDateTime parseRegistrationDateTime() {
        LocalDate date = registrationDatePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Choisissez la date d'inscription.");
        }
        LocalTime time = parseTimeFlexible(registrationTimeField.getText());
        return LocalDateTime.of(date, time);
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

    private static String paymentMethodFromEvent(Event e) {
        String p = e.getPaymentType();
        if (p == null || p.isBlank()) {
            return "CASH";
        }
        String u = p.toUpperCase(Locale.ROOT).trim();
        if (u.contains("CARD")) {
            return "CARD";
        }
        return "CASH";
    }

    private static String labelPaymentFr(String code) {
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CASH" -> "Espèces";
            case "CARD" -> "Carte bancaire";
            default -> code;
        };
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage s = (Stage) submitBtn.getScene().getWindow();
        s.close();
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
