package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.entities.Registration;
import com.esprit.Services.RegistrationService;
import com.esprit.Services.StripePaymentService;
import com.esprit.utils.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
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
    private TextField emailField;
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
                    "Montant à payer : %.2f TND ", event.getPrice()));
        } else {
            fixedAmountLabel.setText("Événement gratuit — montant : 0,00 TND");
        }

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


        prefillFromSession();
    }


    private void prefillFromSession() {
        String firstName = AppSession.getRegistrantFirstName();
        String lastName = AppSession.getRegistrantLastName();
        String email = AppSession.getRegistrantEmail();

        if (firstName != null && !firstName.isBlank()) {
            firstNameField.setText(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            lastNameField.setText(lastName);
        }
        if (email != null && !email.isBlank()) {
            emailField.setText(email);
        }
    }

    @FXML
    private void handleSubmit() {
        if (event == null) {
            return;
        }

        String fn = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
        String ln = lastNameField.getText() != null ? lastNameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        if (fn.isEmpty() || ln.isEmpty() || email.isEmpty()) {
            showError("Le prénom, le nom et l'email sont obligatoires.");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Email invalide. Exemple attendu : utilisateur@email.com");
            return;
        }

        // ===== VÉRIFIER SI PEUT S'INSCRIRE (PLACES) =====
        try {
            if (!registrationService.peutSInscrire(event.getId())) {
                showError("Cet événement est complet.");
                return;
            }
        } catch (SQLException e) {
            showError("Erreur vérification places: " + e.getMessage());
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
            Registration existing = null;
            if (registrationService.existeInscriptionMemePersonne(
                    event.getId(), AppSession.getCurrentUserId(), fn, ln)) {
                existing = registrationService.trouverParIdUtilisateur(
                        AppSession.getCurrentUserId(), event.getId(), fn, ln);
                if (existing != null && isStripeRequired(event) && !existing.isPaid()) {
                    // Déjà inscrit mais paiement non confirmé : on met à jour les coordonnées,
                    // puis on relance Stripe avec les données à jour.
                    existing.setFirstName(fn);
                    existing.setLastName(ln);
                    existing.setEmail(email);
                    if (existing.getRegistrationDate() == null) {
                        existing.setRegistrationDate(registrationDt);
                    }
                    if (existing.getPaymentMethod() == null || existing.getPaymentMethod().isBlank()) {
                        existing.setPaymentMethod(paymentMethodFromEvent(event));
                    }
                    if (existing.getAmount() <= 0 && isStripeRequired(event)) {
                        existing.setAmount(event.getPrice());
                        existing.setBudget(event.getPrice());
                    }
                    registrationService.modifier(existing);
                    AppSession.setRegistrant(fn, ln, email);
                    handleStripePayment(existing);
                    close();
                    return;
                }
                showError("Vous êtes déjà inscrit à cet événement avec ce prénom et ce nom.");
                return;
            }

            // ===== CRÉER L'ENREGISTREMENT =====
            Registration r = new Registration();
            r.setUserId(AppSession.getCurrentUserId());
            r.setEventId(event.getId());
            r.setFirstName(fn);
            r.setLastName(ln);
            r.setEmail(email);
            r.setRegistrationDate(registrationDt);
            r.setAmount(isStripeRequired(event) ? event.getPrice() : 0.0);
            r.setBudget(isStripeRequired(event) ? event.getPrice() : 0.0);
            r.setPaymentMethod(paymentMethodFromEvent(event));
            r.setPaid(false);                        // Pas encore payé
            r.setStatus("REGISTERED");

            // ===== AJOUTER L'ENREGISTREMENT =====
            registrationService.ajouter(r);
            Registration fresh = registrationService.trouverParIdUtilisateur(
                    AppSession.getCurrentUserId(), event.getId(), fn, ln);
            if (fresh == null) {
                AppSession.setRegistrant(fn, ln, email);
                showInfo("✅ Inscription confirmée.");
                close();
                return;
            }

            // Événement payant => Stripe systématiquement.
            if (isStripeRequired(event)) {
                handleStripePayment(fresh);
            } else {
                AppSession.setRegistrant(fn, ln, email);
                showInfo("✅ Inscription confirmée.");
            }

            close();
        } catch (SQLException e) {
            showError("Erreur: " + e.getMessage());
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
        // Verrou métier : FREE ne passe jamais par Stripe.
        if (isStripeRequired(e)) {
            return "CARD";
        }
        return "CASH";
    }

    private static boolean isStripeRequired(Event e) {
        return e != null
                && e.getEventType() != null
                && e.getEventType().equalsIgnoreCase("PAID")
                && e.getPrice() > 0;
    }

    private static String labelPaymentFr(String code) {
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CASH" -> "Espèces";
            case "CARD" -> "Carte bancaire";
            default -> code;
        };
    }

    private static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void handleStripePayment(Registration registration) {
        try {
            StripePaymentService stripe = new StripePaymentService();
            StripePaymentService.CheckoutSessionInfo session = stripe.createCheckoutSession(registration, event);
            registrationService.saveStripeSession(registration.getId(), session.sessionId(), "PENDING");
            openInBrowser(session.checkoutUrl());

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText("Paiement Stripe");
            confirm.setContentText("La page Stripe est ouverte dans votre navigateur.\n"
                    + "Cliquez sur OK après avoir terminé le paiement.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean paid = stripe.waitForCheckoutSessionPaid(session.sessionId(), 10, 1500);
                if (paid) {
                    registrationService.effectuerPaiement(registration.getId());
                    registrationService.saveStripeSession(registration.getId(), session.sessionId(), "PAID");
                    AppSession.setRegistrant(registration.getFirstName(), registration.getLastName(), registration.getEmail());
                    showInfo("✅ Inscription et paiement Stripe confirmés.");
                } else {
                    showError("Le paiement n'est pas encore confirmé par Stripe.\n"
                            + "Attendez quelques secondes puis réessayez \"Payer et confirmer\".");
                }
            }
        } catch (Exception ex) {
            showError("Paiement Stripe impossible: " + ex.getMessage());
        }
    }

    private void openInBrowser(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL Stripe vide.");
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Ouverture navigateur non supportée.");
        }
        Desktop.getDesktop().browse(new URI(url));
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
