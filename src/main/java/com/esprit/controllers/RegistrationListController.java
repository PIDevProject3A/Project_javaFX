package com.esprit.controllers;

import com.esprit.entities.Registration;
import com.esprit.entities.Event;
import com.esprit.services.EventService;
import com.esprit.services.PaymentReceiptService;
import com.esprit.services.ReceiptMailService;
import com.esprit.services.RegistrationService;
import com.esprit.utils.AppSession;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.StyleHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Modality;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RegistrationListController {


    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MESSAGE_INFO_STYLE = "-fx-text-fill: #546e7a; -fx-font-size: 12px;";
    private static final String MESSAGE_SUCCESS_STYLE = "-fx-text-fill: #2e7d32; -fx-font-size: 12px; -fx-font-weight: bold;";

    @FXML
    private Label participantBanner;
    @FXML
    private TextField filterEventField;
    @FXML
    private ComboBox<String> filterPaymentCombo;
    @FXML
    private Label searchMessageLabel;
    @FXML
    private ListView<Registration> registrationList;
    @FXML
    private java.util.ResourceBundle resources;

    private final RegistrationService registrationService = new RegistrationService();
    private final EventService eventService = new EventService();
    private final PaymentReceiptService receiptService = new PaymentReceiptService();
    private final ReceiptMailService receiptMailService = new ReceiptMailService();

    @FXML
    public void initialize() {
        filterPaymentCombo.setItems(FXCollections.observableArrayList(resources.getString("generic.all"), "CASH", "CARD"));
        filterPaymentCombo.getSelectionModel().selectFirst();
        filterEventField.setTooltip(new Tooltip(resources.getString("reglist.search_placeholder")));
        filterPaymentCombo.setTooltip(new Tooltip(resources.getString("reglist.filter_payment")));
        configureListCells();
        refreshBanner();
        loadData();
    }

    private void refreshBanner() {
        participantBanner.setStyle(MESSAGE_INFO_STYLE);
        if (searchMessageLabel != null) {
            searchMessageLabel.setText(resources.getString("reglist.no_results"));
            searchMessageLabel.setStyle(MESSAGE_INFO_STYLE);
        }
    }

    private void configureListCells() {
        registrationList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label badgeDate = new Label();
            private final Label badgeAmount = new Label();
            private final Label badgePay = new Label();
            private final Label participantInfo = new Label();
            private final Label emailInfo = new Label();
            private final HBox badgeRow = new HBox(8, badgeDate, badgeAmount, badgePay);
            private final VBox left = new VBox(8, title, badgeRow, participantInfo, emailInfo);
            private final Button viewB = new Button(resources.getString("reglist.btn.view"));
            private final Button editB = new Button(resources.getString("reglist.btn.edit"));
            private final Button delB = new Button(resources.getString("reglist.btn.delete"));
            private final Button pdfB = new Button(resources.getString("reglist.btn.pdf"));
            private final VBox actions = new VBox(8, viewB, editB, delB, pdfB);
            private final Region spacer = new Region();
            private final HBox inner = new HBox(14, left, spacer, actions);
            private final VBox card = new VBox(inner);

            {
                VBox.setVgrow(left, Priority.ALWAYS);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox.setHgrow(left, Priority.ALWAYS);
                left.setMaxWidth(Double.MAX_VALUE);
                title.getStyleClass().add("reg-event-title");
                badgeRow.getStyleClass().add("badge-row");
                badgeDate.getStyleClass().addAll("badge", "badge-date");
                badgeAmount.getStyleClass().addAll("badge", "badge-amount");
                participantInfo.setStyle("-fx-font-size: 13px; -fx-text-fill:#1f2937;");
                emailInfo.setStyle("-fx-font-size: 12px; -fx-text-fill:#455a64;");
                card.getStyleClass().add("bledna-card");
                inner.setAlignment(Pos.TOP_LEFT);
                inner.setPadding(new Insets(0));
                actions.setAlignment(Pos.TOP_RIGHT);
                actions.getStyleClass().add("action-buttons");
                viewB.getStyleClass().add("btn-action-view");
                editB.getStyleClass().add("btn-action-edit");
                delB.getStyleClass().add("btn-action-delete");
                pdfB.getStyleClass().add("btn-action-view");
                viewB.setMaxWidth(Double.MAX_VALUE);
                editB.setMaxWidth(Double.MAX_VALUE);
                delB.setMaxWidth(Double.MAX_VALUE);
                pdfB.setMaxWidth(Double.MAX_VALUE);

                viewB.setOnAction(e -> {
                    Registration r = getItem();
                    if (r != null) showView(r);
                });

                editB.setOnAction(e -> {
                    Registration r = getItem();
                    if (r != null) openEdit(r);
                });

                delB.setOnAction(e -> {
                    Registration r = getItem();
                    if (r != null) confirmDelete(r);
                });
                pdfB.setOnAction(e -> {
                    Registration r = getItem();
                    if (r != null) exportPdfPlaceholder(r);
                });
            }

            @Override
            protected void updateItem(Registration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText("📍 " + (item.getEventName() != null ? item.getEventName() : "Event"));
                    String regDate = item.getRegistrationDate() != null
                            ? item.getRegistrationDate().format(DT)
                            : (item.getPaymentDate() != null ? item.getPaymentDate().format(DT) : "Date not specified");
                    badgeDate.setText("🗓 " + regDate);
                    badgeAmount.setText("💰 " + String.format(Locale.US, "%.2f TND", item.getAmount()));
                    participantInfo.setText("👤 " + resolveParticipantForReceipt(item.getFullName()));
                    emailInfo.setText("✉ " + resolveEmailForReceipt(item.getEmail()));

                    String payCode = item.getPaymentMethod() != null ? item.getPaymentMethod() : "";
                    badgePay.getStyleClass().removeAll("badge-pay-cash", "badge-pay-card");

                    if ("CARD".equalsIgnoreCase(payCode)) {
                        badgePay.getStyleClass().add("badge-pay-card");
                        badgePay.setText("💳 " + labelPayment(payCode));
                    } else {
                        badgePay.getStyleClass().add("badge-pay-cash");
                        badgePay.setText("💵 " + labelPayment(payCode));
                    }

                    setText(null);
                    setGraphic(card);
                }
            }
        });
    }

    private static String labelPayment(String code) {
        if (code == null || code.isBlank()) return "—";
        return switch (code.toUpperCase(Locale.ROOT)) {
            case "CASH" -> "Cash";
            case "CARD" -> "Card";
            default -> code;
        };
    }

    private void loadData() {
        try {
            String pay = filterPaymentCombo.getSelectionModel().getSelectedItem();
            // Normalize "TOUTES" or "ALL" for the service layer
            if (resources.getString("generic.all").equalsIgnoreCase(pay)) {
                pay = "ALL";
            }
            
            List<Registration> list = registrationService.listerFiltre(
                    AppSession.getCurrentUserId(),
                    null,
                    null,
                    filterEventField.getText(),
                    pay
            );
            registrationList.setItems(FXCollections.observableArrayList(list));

            if (searchMessageLabel != null) {
                if (list.isEmpty()) {
                    searchMessageLabel.setText(resources.getString("reglist.no_results"));
                    searchMessageLabel.setStyle(MESSAGE_INFO_STYLE);
                } else {
                    searchMessageLabel.setText(resources.getString("generic.success") + ": " + list.size());
                    searchMessageLabel.setStyle(MESSAGE_SUCCESS_STYLE);
                }
            }
        } catch (SQLException e) {
            showError(resources.getString("generic.error") + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String eventText = filterEventField.getText() != null ? filterEventField.getText().trim() : "";
        String pay = filterPaymentCombo.getSelectionModel().getSelectedItem();

        if (eventText.isEmpty() && (pay == null || "ALL".equalsIgnoreCase(pay))) {
            searchMessageLabel.setText("Tip: enter an event or choose a payment to filter.");
            searchMessageLabel.setStyle(MESSAGE_INFO_STYLE);
        }

        loadData();
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) registrationList.getScene().getWindow();
            javafx.scene.Scene scene = new Scene(root, 1100, 680);
            StyleHelper.apply(scene);
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Navigation error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() throws IOException {
        switchScene("/com/esprit/EventCatalog.fxml");
    }

    private void showView(Registration r) {
        String body = String.join("\n",
                "Event: " + nullToDash(r.getEventName()),
                "Date: " + (r.getRegistrationDate() != null ? r.getRegistrationDate().format(DT)
                        : (r.getPaymentDate() != null ? r.getPaymentDate().format(DT) : "Date not specified")),
                String.format(Locale.US, "Amount: %.2f TND", r.getAmount()),
                "Payment: " + labelPayment(r.getPaymentMethod()),
                "Status: " + nullToDash(r.getStatus())
        );

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("bledna — details");
        a.setTitle("bledna");
        a.setContentText(body);
        a.showAndWait();
    }

    private void openEdit(Registration r) {
        try {
            Registration fresh = registrationService.trouverParId(r.getId());
            if (fresh == null) {
                showError("Registration not found.");
                return;
            }

            Stage owner = registrationList.getScene() != null && registrationList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.hide();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/RegistrationEdit.fxml"));
            Stage stage = new Stage();
            stage.setTitle("bledna — modify registration");
            Scene sc = new Scene(loader.load(), 540, 460);

            StyleHelper.apply(sc);
            stage.setScene(sc);
            if (owner != null) {
                stage.initOwner(owner);
            }

            RegistrationEditController c = loader.getController();
            c.setRegistration(fresh);

            stage.showAndWait();
            loadData();

        } catch (IOException e) {
            showError("Loading error: " + e.getMessage());
        } catch (SQLException e) {
            showError("DB Error: " + e.getMessage());
        } finally {
            Stage owner = registrationList.getScene() != null && registrationList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.show();
            }
        }
    }

    private void confirmDelete(Registration r) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("bledna");
        c.setHeaderText("Confirm Deletion");
        c.setContentText("Delete registration for \"" + r.getEventName() + "\"?");

        Optional<ButtonType> res = c.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                registrationService.supprimer(r.getId());
                loadData();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("bledna");
                success.setHeaderText("Success");
                success.setContentText("Registration deleted successfully.");
                success.showAndWait();

            } catch (SQLException e) {
                showError("Deletion error: " + e.getMessage());
            }
        }
    }

    private void exportPdfPlaceholder(Registration r) {
        try {
            Registration full = registrationService.trouverParId(r.getId());
            if (full == null) {
                showError("Registration not found.");
                return;
            }
            Event ev = eventService.trouverParId(full.getEventId());
            PaymentReceiptService.ReceiptData data = receiptService.buildReceiptData(full, ev);
            openReceiptDialog(data);
        } catch (Exception ex) {
            showError("Unable to open receipt: " + ex.getMessage());
        }
    }

    private void openReceiptDialog(PaymentReceiptService.ReceiptData data) throws Exception {
        String resolvedParticipant = resolveParticipantForReceipt(data.participantName());
        String resolvedEmail = resolveEmailForReceipt(data.email());
        PaymentReceiptService.ReceiptData displayData = withResolvedIdentity(data, resolvedParticipant, resolvedEmail);

        Label appTag = new Label("BLEDNA");
        appTag.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label title = new Label("Payment Receipt");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        Button downloadBtn = new Button("⬇ Download PDF");
        downloadBtn.getStyleClass().add("btn-primary");
        downloadBtn.setOnAction(e -> {
            try {
                var file = receiptService.exportPdfReceipt(downloadBtn.getScene().getWindow(), displayData);
                if (file == null) {
                    return;
                }
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("bledna");
                ok.setHeaderText("PDF generated");
                ok.setContentText("The PDF file has been downloaded to your PC.");
                ok.showAndWait();
            } catch (Exception ex) {
                showError("PDF export error: " + ex.getMessage());
            }
        });

        Button mailBtn = new Button("✉ Send email");
        mailBtn.getStyleClass().add("btn-action-view");
        mailBtn.setOnAction(e -> {
            try {
                String targetEmail = resolveEmailForSending(data, resolvedEmail);
                if (targetEmail == null || targetEmail.isBlank() || "—".equals(targetEmail)) {
                    showError("Email not found. Add an email in the registration form.");
                    return;
                }
                PaymentReceiptService.ReceiptData mailData = withResolvedIdentity(data, resolvedParticipant, targetEmail);
                byte[] pdfBytes = receiptService.generatePdfBytes(mailData);
                receiptMailService.sendReceiptEmail(targetEmail, mailData.eventName(), mailData.receiptCode(), pdfBytes);
                registrationService.updateEmailById(data.registrationId(), targetEmail);
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("bledna");
                ok.setHeaderText("Email sent");
                ok.setContentText("The receipt has been sent to: " + targetEmail);
                ok.showAndWait();
            } catch (Exception ex) {
                showError("Error sending email: " + ex.getMessage());
            }
        });

        Label code = new Label("Receipt code: " + data.receiptCode());
        code.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label eventTitle = new Label(data.eventName());
        eventTitle.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0d1b2a;");
        Label eventMeta = new Label("📅 " + (data.eventDate() != null ? data.eventDate().format(DT) : "—")
                + "    📍 " + data.location());
        eventMeta.setStyle("-fx-text-fill:#455a64; -fx-font-size: 14px;");

        Label participant = new Label("Participant: " + resolvedParticipant);
        Label email = new Label("Email: " + resolvedEmail);
        Label regDate = new Label("Registration date: " + (data.registrationDate() != null ? data.registrationDate().format(DT) : "—"));
        participant.setStyle("-fx-font-size: 14px;");
        email.setStyle("-fx-font-size: 14px;");
        regDate.setStyle("-fx-font-size: 14px;");

        Label paymentMethod = new Label("Payment method: " + data.paymentMethod());
        paymentMethod.setStyle("-fx-font-size: 14px;");
        Label amount = new Label(String.format(Locale.US, "Amount paid: %.2f TND", data.amount()));
        amount.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill:#0d1b2a;");

        ImageView qr = new ImageView(receiptService.createQrFxImage(data.qrPayload(), 180));
        qr.setFitWidth(180);
        qr.setFitHeight(180);
        Label qrInfo = new Label("Present this receipt at the entrance\nScan the QR code to verify authenticity.");
        qrInfo.setStyle("-fx-text-fill:#607d8b; -fx-font-size:12px;");

        HBox top = new HBox(12, appTag, title);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox contentCard = new VBox(10, code, new Separator(), eventTitle, eventMeta, new Separator(),
                participant, email, regDate, new Separator(), paymentMethod, amount);
        contentCard.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:12; -fx-padding:16; -fx-border-color:#dbe5ef; -fx-border-radius:12;");

        HBox qrRow = new HBox(14, qr, qrInfo);
        qrRow.setAlignment(Pos.CENTER_LEFT);

        HBox actionButtons = new HBox(10, downloadBtn, mailBtn);
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, top, actionButtons, contentCard, qrRow);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");
        root.setAlignment(Pos.TOP_LEFT);

        Stage owner = registrationList.getScene() != null && registrationList.getScene().getWindow() instanceof Stage s ? s : null;
        if (owner != null) {
            owner.hide();
        }
        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("bledna — payment receipt");
        Scene sc = new Scene(root, 760, 760);
        StyleHelper.apply(sc);
        st.setScene(sc);
        if (owner != null) {
            st.initOwner(owner);
        }
        try {
            st.showAndWait();
        } finally {
            if (owner != null) {
                owner.show();
            }
        }
    }

    private String resolveEmailForReceipt(String currentEmail) {
        if (currentEmail != null && !currentEmail.isBlank() && !"—".equals(currentEmail)) {
            return currentEmail.trim();
        }
        String sessionMail = AppSession.getRegistrantEmail();
        if (sessionMail != null && !sessionMail.isBlank()) {
            return sessionMail.trim();
        }
        return "—";
    }

    private String resolveParticipantForReceipt(String participantName) {
        if (participantName != null && !participantName.isBlank() && !"—".equals(participantName)) {
            return participantName.trim();
        }
        String first = AppSession.getRegistrantFirstName() != null ? AppSession.getRegistrantFirstName().trim() : "";
        String last = AppSession.getRegistrantLastName() != null ? AppSession.getRegistrantLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isBlank() ? "—" : combined;
    }

    private String resolveEmailForSending(PaymentReceiptService.ReceiptData data, String displayEmail) {
        if (displayEmail != null && !displayEmail.isBlank() && !"—".equals(displayEmail)) {
            return displayEmail.trim();
        }
        if (data.email() != null && !data.email().isBlank() && !"—".equals(data.email())) {
            return data.email().trim();
        }
        String sessionMail = AppSession.getRegistrantEmail();
        if (sessionMail != null && !sessionMail.isBlank()) {
            return sessionMail.trim();
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email required");
        dialog.setHeaderText("Enter the email to send the receipt");
        dialog.setContentText("Email:");
        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty()) {
            return "—";
        }
        String email = res.get().trim();
        if (!isValidEmail(email)) {
            showError("Invalid email. The receipt will only be available as a local PDF.");
            return "—";
        }
        return email;
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static PaymentReceiptService.ReceiptData withResolvedIdentity(
            PaymentReceiptService.ReceiptData d, String participant, String email) {
        return new PaymentReceiptService.ReceiptData(
                d.registrationId(),
                d.receiptCode(),
                d.eventName(),
                d.eventDate(),
                d.location(),
                participant,
                email,
                d.registrationDate(),
                d.amount(),
                d.paymentMethod(),
                d.status(),
                d.qrPayload()
        );
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("bledna");
        a.setHeaderText("Error");
        a.setContentText(m);
        a.showAndWait();
    }

}

