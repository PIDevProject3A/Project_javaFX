package com.esprit.controllers;

import com.esprit.entities.Registration;
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
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RegistrationListController {


    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MESSAGE_RED_STYLE = "-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-font-weight: bold;";

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
        participantBanner.setText("Affichage des inscriptions (filtres : événement et paiement).");
        participantBanner.setStyle("-fx-text-fill: #c62828; -fx-font-size: 12px; -fx-font-weight: bold;");
        if (searchMessageLabel != null) {
            searchMessageLabel.setText("Saisissez un critère puis cliquez sur Rechercher.");
            searchMessageLabel.setStyle(MESSAGE_RED_STYLE);
        }
    }

    private void configureListCells() {
        registrationList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label badgeDate = new Label();
            private final Label badgeAmount = new Label();
            private final Label badgePay = new Label();
            private final Label lineInscrit = new Label();
            private final HBox badgeRow = new HBox(8, badgeDate, badgeAmount, badgePay);
            private final VBox left = new VBox(8, title, badgeRow, lineInscrit);
            private final Button viewB = new Button("👁  Voir");
            private final Button editB = new Button("✏  Modifier");
            private final Button delB = new Button("🗑  Supprimer");
            private final VBox actions = new VBox(8, viewB, editB, delB);
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
                lineInscrit.getStyleClass().add("reg-meta-line");
                card.getStyleClass().add("bledna-card");
                inner.setAlignment(Pos.TOP_LEFT);
                inner.setPadding(new Insets(0));
                actions.setAlignment(Pos.TOP_RIGHT);
                actions.getStyleClass().add("action-buttons");
                viewB.getStyleClass().add("btn-action-view");
                editB.getStyleClass().add("btn-action-edit");
                delB.getStyleClass().add("btn-action-delete");
                viewB.setMaxWidth(Double.MAX_VALUE);
                editB.setMaxWidth(Double.MAX_VALUE);
                delB.setMaxWidth(Double.MAX_VALUE);

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
            }

            @Override
            protected void updateItem(Registration item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText("📍 " + (item.getEventName() != null ? item.getEventName() : "Événement"));
                    badgeDate.setText("🗓 " + (item.getRegistrationDate() != null ? item.getRegistrationDate().format(DT) : "—"));
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

                    String who = item.getFullName();
                    if (who == null || who.isBlank()) {
                        if (AppSession.hasRegistrant()) {
                            who = AppSession.getRegistrantFirstName() + " " + AppSession.getRegistrantLastName();
                        } else {
                            who = "—";
                        }
                    }
                    lineInscrit.setText("👤 Inscrit : " + who);

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
                searchMessageLabel.setStyle(MESSAGE_RED_STYLE);
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
            searchMessageLabel.setStyle(MESSAGE_RED_STYLE);
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
                "Inscrit : " + nullToDash(r.getFullName()),
                "Date : " + (r.getRegistrationDate() != null ? r.getRegistrationDate().format(DT) : "—"),
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
