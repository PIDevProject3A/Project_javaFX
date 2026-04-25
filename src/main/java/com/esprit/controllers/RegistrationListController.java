package com.esprit.controllers;

import com.esprit.entities.Registration;
import com.esprit.entities.Event;
import com.esprit.Services.EventService;
import com.esprit.Services.PaymentReceiptService;
import com.esprit.Services.ReceiptMailService;
import com.esprit.Services.RegistrationService;
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

    private final RegistrationService registrationService = new RegistrationService();
    private final EventService eventService = new EventService();
    private final PaymentReceiptService receiptService = new PaymentReceiptService();
    private final ReceiptMailService receiptMailService = new ReceiptMailService();

    @FXML
    public void initialize() {
        filterPaymentCombo.setItems(FXCollections.observableArrayList("TOUTES", "CASH", "CARD"));
        filterPaymentCombo.getSelectionModel().selectFirst();
        filterEventField.setTooltip(new Tooltip("Saisissez un mot du nom d'événement (ex: Beach)."));
        filterPaymentCombo.setTooltip(new Tooltip("Filtrez par mode de paiement ou laissez TOUTES."));
        configureListCells();
        refreshBanner();
        loadData();
    }

    private void refreshBanner() {

        participantBanner.setStyle(MESSAGE_INFO_STYLE);
        if (searchMessageLabel != null) {
            searchMessageLabel.setText("Saisissez un critère puis cliquez sur Rechercher.");
            searchMessageLabel.setStyle(MESSAGE_INFO_STYLE);
        }
    }

    private void configureListCells() {
        registrationList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label badgeDate = new Label();
            private final Label badgeAmount = new Label();
            private final Label badgePay = new Label();
            private final HBox badgeRow = new HBox(8, badgeDate, badgeAmount, badgePay);
            private final VBox left = new VBox(8, title, badgeRow);
            private final Button viewB = new Button("👁  Voir");
            private final Button editB = new Button("✏  Modifier");
            private final Button delB = new Button("🗑  Supprimer");
            private final Button pdfB = new Button("📄  Reçu de paiement");
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
                    title.setText("📍 " + (item.getEventName() != null ? item.getEventName() : "Événement"));
                    String regDate = item.getRegistrationDate() != null
                            ? item.getRegistrationDate().format(DT)
                            : (item.getPaymentDate() != null ? item.getPaymentDate().format(DT) : "Date non renseignée");
                    badgeDate.setText("🗓 " + regDate);
                    badgeAmount.setText("💰 " + String.format(Locale.FRANCE, "%.2f TND", item.getAmount()));

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
            case "CASH" -> "Espèces";
            case "CARD" -> "Carte";
            default -> code;
        };
    }

    private void loadData() {
        try {
            String pay = filterPaymentCombo.getSelectionModel().getSelectedItem();
            List<Registration> list = registrationService.listerFiltre(
                    AppSession.getCurrentUserId(),
                    null,
                    null,
                    filterEventField.getText(),
                    pay
            );
            registrationList.setItems(FXCollections.observableArrayList(list));

            if (searchMessageLabel != null) {
                searchMessageLabel.setText(
                        list.isEmpty()
                                ? "Aucun résultat. Modifiez votre saisie puis relancez la recherche."
                                : list.size() + " inscription(s) trouvée(s)."
                );
                searchMessageLabel.setStyle(list.isEmpty() ? MESSAGE_INFO_STYLE : MESSAGE_SUCCESS_STYLE);
            }
        } catch (SQLException e) {
            showError("Erreur de chargement : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        String eventText = filterEventField.getText() != null ? filterEventField.getText().trim() : "";
        String pay = filterPaymentCombo.getSelectionModel().getSelectedItem();

        if (eventText.isEmpty() && (pay == null || "TOUTES".equalsIgnoreCase(pay))) {
            searchMessageLabel.setText("Astuce : saisissez un événement ou choisissez un paiement pour filtrer.");
            searchMessageLabel.setStyle(MESSAGE_INFO_STYLE);
        }

        loadData();
    }

    @FXML
    private void handleBack() throws IOException {
        NavigationManager.navigateTo("EventCatalog.fxml");
    }

    private void showView(Registration r) {
        String body = String.join("\n",
                "Événement : " + nullToDash(r.getEventName()),
                "Date : " + (r.getRegistrationDate() != null ? r.getRegistrationDate().format(DT)
                        : (r.getPaymentDate() != null ? r.getPaymentDate().format(DT) : "Date non renseignée")),
                String.format(Locale.FRANCE, "Montant : %.2f TND", r.getAmount()),
                "Paiement : " + labelPayment(r.getPaymentMethod()),
                "Statut : " + nullToDash(r.getStatus())
        );

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("bledna — détail");
        a.setTitle("bledna");
        a.setContentText(body);
        a.showAndWait();
    }

    private void openEdit(Registration r) {
        try {
            Registration fresh = registrationService.trouverParId(r.getId());
            if (fresh == null) {
                showError("Inscription introuvable.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/RegistrationEdit.fxml"));
            Stage stage = new Stage();
            stage.setTitle("bledna — modifier l'inscription");
            Scene sc = new Scene(loader.load(), 540, 460);

            StyleHelper.apply(sc);
            stage.setScene(sc);

            RegistrationEditController c = loader.getController();
            c.setRegistration(fresh);

            stage.showAndWait();
            loadData();

        } catch (IOException e) {
            showError("Erreur de chargement : " + e.getMessage());
        } catch (SQLException e) {
            showError("Erreur DB : " + e.getMessage());
        }
    }

    private void confirmDelete(Registration r) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("bledna");
        c.setHeaderText("Confirmer la suppression");
        c.setContentText("Supprimer l'inscription à « " + r.getEventName() + " » ?");

        Optional<ButtonType> res = c.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                registrationService.supprimer(r.getId());
                loadData();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("bledna");
                success.setHeaderText("Succès");
                success.setContentText("Inscription supprimée avec succès.");
                success.showAndWait();

            } catch (SQLException e) {
                showError("Erreur suppression : " + e.getMessage());
            }
        }
    }

    private void exportPdfPlaceholder(Registration r) {
        try {
            Registration full = registrationService.trouverParId(r.getId());
            if (full == null) {
                showError("Inscription introuvable.");
                return;
            }
            Event ev = eventService.trouverParId(full.getEventId());
            PaymentReceiptService.ReceiptData data = receiptService.buildReceiptData(full, ev);
            openReceiptDialog(data);
        } catch (Exception ex) {
            showError("Impossible d'ouvrir le reçu : " + ex.getMessage());
        }
    }

    private void openReceiptDialog(PaymentReceiptService.ReceiptData data) throws Exception {
        Label appTag = new Label("BLEDNA");
        appTag.setStyle("-fx-text-fill: #607d8b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label title = new Label("Reçu de paiement");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        Button downloadBtn = new Button("⬇ Télécharger PDF");
        downloadBtn.getStyleClass().add("btn-primary");
        downloadBtn.setOnAction(e -> {
            try {
                var file = receiptService.exportPdfReceipt(downloadBtn.getScene().getWindow(), data);
                if (file == null) {
                    return;
                }
                boolean mailed = false;
                if (data.email() != null && !data.email().isBlank() && !"—".equals(data.email())) {
                    byte[] pdfBytes = receiptService.generatePdfBytes(data);
                    receiptMailService.sendReceiptEmail(data.email(), data.eventName(), data.receiptCode(), pdfBytes);
                    mailed = true;
                }
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("bledna");
                ok.setHeaderText("PDF généré");
                ok.setContentText(mailed
                        ? "Le fichier PDF a été téléchargé sur votre PC.\nLe reçu a aussi été envoyé par email."
                        : "Le fichier PDF a été téléchargé sur votre PC.\nEmail non envoyé (adresse manquante).");
                ok.showAndWait();
            } catch (Exception ex) {
                showError("Erreur export PDF : " + ex.getMessage());
            }
        });

        Label code = new Label("Code reçu : " + data.receiptCode());
        code.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label eventTitle = new Label(data.eventName());
        eventTitle.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0d1b2a;");
        Label eventMeta = new Label("📅 " + (data.eventDate() != null ? data.eventDate().format(DT) : "—")
                + "    📍 " + data.location());
        eventMeta.setStyle("-fx-text-fill:#455a64; -fx-font-size: 14px;");

        Label participant = new Label("Participant : " + data.participantName());
        Label email = new Label("Email : " + data.email());
        Label regDate = new Label("Date d'inscription : " + (data.registrationDate() != null ? data.registrationDate().format(DT) : "—"));
        participant.setStyle("-fx-font-size: 14px;");
        email.setStyle("-fx-font-size: 14px;");
        regDate.setStyle("-fx-font-size: 14px;");

        Label paymentMethod = new Label("Méthode de paiement : " + data.paymentMethod());
        paymentMethod.setStyle("-fx-font-size: 14px;");
        Label amount = new Label(String.format(Locale.FRANCE, "Montant payé : %.2f TND", data.amount()));
        amount.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill:#0d1b2a;");

        ImageView qr = new ImageView(receiptService.createQrFxImage(data.qrPayload(), 180));
        qr.setFitWidth(180);
        qr.setFitHeight(180);
        Label qrInfo = new Label("Présentez ce reçu à l'entrée\nScannez le QR code pour vérifier l'authenticité.");
        qrInfo.setStyle("-fx-text-fill:#607d8b; -fx-font-size:12px;");

        HBox top = new HBox(12, appTag, title);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox contentCard = new VBox(10, code, new Separator(), eventTitle, eventMeta, new Separator(),
                participant, email, regDate, new Separator(), paymentMethod, amount);
        contentCard.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:12; -fx-padding:16; -fx-border-color:#dbe5ef; -fx-border-radius:12;");

        HBox qrRow = new HBox(14, qr, qrInfo);
        qrRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14, top, downloadBtn, contentCard, qrRow);
        root.setStyle("-fx-padding: 20; -fx-background-color: white;");
        root.setAlignment(Pos.TOP_LEFT);

        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("bledna — reçu de paiement");
        Scene sc = new Scene(root, 760, 760);
        StyleHelper.apply(sc);
        st.setScene(sc);
        st.showAndWait();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("bledna");
        a.setHeaderText("Erreur");
        a.setContentText(m);
        a.showAndWait();
    }

}
