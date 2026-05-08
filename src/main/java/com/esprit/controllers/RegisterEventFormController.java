package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.entities.Registration;
import com.esprit.services.RegistrationService;
import com.esprit.services.StripePaymentService;
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
                "Event date: " + (d == null ? "—" : d.format(DT)),
                "Location: " + (event.getLocation() != null ? event.getLocation() : "—"),
                "Type: " + (paidEvent ? "Paid" : "Free"),
                "Slots: " + event.getCurrentParticipants() + " / " + (event.getMaxPlaces() > 0 ? event.getMaxPlaces() : "∞")
        );
        eventDetailsLabel.setText(details);

        if (paidEvent) {
            fixedAmountLabel.setText(String.format(Locale.US,
                    "Amount to pay: %.2f TND ", event.getPrice()));
        } else {
            fixedAmountLabel.setText("Free event — amount: 0.00 TND");
        }

        registrationDatePicker.setValue(LocalDate.now());
        registrationTimeField.setText(LocalTime.now().truncatedTo(ChronoUnit.MINUTES).format(HM));

        submitBtn.setText(paidEvent ? "✓ Pay and Confirm" : "✓ Confirm Registration");
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

        if (com.esprit.utils.UserSession.getCurrentUserEmail() != null) {
            String currentEmail = com.esprit.utils.UserSession.getCurrentUserEmail();
            com.esprit.services.UserService userService = new com.esprit.services.UserService();
            if (com.esprit.utils.UserSession.isAppUser()) {
                com.esprit.entities.AppUser appUser = userService.findAppUserByEmail(currentEmail);
                if (appUser != null) {
                    if (firstName == null || firstName.isBlank()) firstName = appUser.getFirstName();
                    if (lastName == null || lastName.isBlank()) lastName = appUser.getLastName();
                    if (email == null || email.isBlank()) email = appUser.getEmail();
                }
            } else {
                com.esprit.entities.User admin = userService.findByEmail(currentEmail);
                if (admin != null) {
                    if (firstName == null || firstName.isBlank()) firstName = admin.getFirstName();
                    if (lastName == null || lastName.isBlank()) lastName = admin.getLastName();
                    if (email == null || email.isBlank()) email = admin.getEmail();
                }
            }
        }

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
            showError("First name, last name, and email are required.");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Invalid email. Example: user@email.com");
            return;
        }

        // ===== CHECK IF REGISTRATION IS POSSIBLE (SLOTS) =====
        try {
            if (!registrationService.peutSInscrire(event.getId())) {
                showError("This event is full.");
                return;
            }
        } catch (SQLException e) {
            showError("Error checking slots: " + e.getMessage());
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
                showError("You are already registered for this event with this name.");
                return;
            }

            // ===== CREATE REGISTRATION =====
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

            // ===== ADD REGISTRATION =====
            registrationService.ajouter(r);
            Registration fresh = registrationService.trouverParIdUtilisateur(
                    AppSession.getCurrentUserId(), event.getId(), fn, ln);
            if (fresh == null) {
                AppSession.setRegistrant(fn, ln, email);
                showInfo("✅ Registration confirmed.");
                close();
                return;
            }

            // Paid event => Systematic Stripe payment.
            if (isStripeRequired(event)) {
                handleStripePayment(fresh);
            } else {
                AppSession.setRegistrant(fn, ln, email);
                showInfo("✅ Registration confirmed.");
            }

            close();
        } catch (SQLException e) {
            showError("Error: " + e.getMessage());
        }
    }

    private LocalDateTime parseRegistrationDateTime() {
        LocalDate date = registrationDatePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Choose the registration date.");
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
                throw new IllegalArgumentException("Invalid time (0–23 for hours, 0–59 for minutes).");
            }
            return LocalTime.of(h, m);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time. Use HH:mm format (e.g., 14:30).");
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

    private static String labelPaymentEn(String code) {
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CASH" -> "Cash";
            case "CARD" -> "Credit Card";
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
            confirm.setHeaderText("Stripe Payment");
            confirm.setContentText("The Stripe page is open in your browser.\n"
                    + "Click OK after completing the payment.");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean paid = stripe.waitForCheckoutSessionPaid(session.sessionId(), 10, 1500);
                if (paid) {
                    registrationService.effectuerPaiement(registration.getId());
                    registrationService.saveStripeSession(registration.getId(), session.sessionId(), "PAID");
                    AppSession.setRegistrant(registration.getFirstName(), registration.getLastName(), registration.getEmail());
                    showInfo("✅ Registration and Stripe payment confirmed.");
                } else {
                    showError("The payment is not yet confirmed by Stripe.\n"
                            + "Wait a few seconds and try \"Pay and Confirm\" again.");
                }
            }
        } catch (Exception ex) {
            showError("Stripe payment unavailable: " + ex.getMessage());
        }
    }

    private void openInBrowser(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Stripe URL is empty.");
        }
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Browser opening not supported.");
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
        a.setTitle("Error");
        a.setHeaderText("bledna");
        a.setContentText(m);
        a.showAndWait();
    }

    private void showInfo(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText("bledna");
        a.setContentText(m);
        a.showAndWait();
    }
}

