package com.esprit.controllers;

import com.esprit.entities.Event;
import com.esprit.services.EventService;
import com.esprit.services.RegistrationService;
import com.esprit.utils.NavigationManager;
import com.esprit.utils.StyleHelper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EventAdminController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ListView<Event> eventList;
    @FXML
    private Button addBtn;

    private final EventService eventService = new EventService();
    private final RegistrationService registrationService = new RegistrationService();

    @FXML
    public void initialize() {
        configureListCells();
        loadEvents();
    }

    @FXML
    private void handleHome() throws IOException {
        NavigationManager.navigateTo("Home.fxml");
    }

    private void configureListCells() {
        eventList.setCellFactory(lv -> new ListCell<>() {
            private final Label title = new Label();
            private final Label meta = new Label();
            private final Label desc = new Label();
            private final Button registrantsBtn = new Button();
            private final VBox textCol = new VBox(4, title, meta, desc, registrantsBtn);
            private final Button viewBtn = new Button("👁 Voir");
            private final Button editBtn = new Button("✏ Modifier");
            private final Button delBtn = new Button("🗑 Supprimer");
            private final HBox actions = new HBox(8, viewBtn, editBtn, delBtn);
            private final Region spacer = new Region();
            private final HBox row = new HBox(12, textCol, spacer, actions);
            private final VBox card = new VBox(row);

            {
                card.getStyleClass().add("bledna-card");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                textCol.setPrefWidth(720);
                title.getStyleClass().add("reg-event-title");
                meta.getStyleClass().add("reg-meta-line");
                desc.getStyleClass().add("reg-meta-line");
                desc.setWrapText(true);
                desc.setMaxWidth(720);
                registrantsBtn.getStyleClass().add("btn-outline");
                registrantsBtn.setMaxWidth(280);
                registrantsBtn.setAlignment(Pos.CENTER_LEFT);
                registrantsBtn.setStyle("-fx-background-color:#e8f5e9; -fx-border-color:#81c784; -fx-border-radius:14; -fx-background-radius:14; -fx-text-fill:#1b5e20; -fx-font-weight:bold;");
                viewBtn.getStyleClass().add("btn-action-view");
                editBtn.getStyleClass().add("btn-action-edit");
                delBtn.getStyleClass().add("btn-action-delete");
                actions.getStyleClass().add("action-buttons");
                actions.setAlignment(Pos.TOP_RIGHT);
                actions.setPadding(new Insets(4, 0, 0, 0));
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(0));
                viewBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        showDetails(item);
                    }
                });
                editBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        openEdit(item);
                    }
                });
                delBtn.setOnAction(ev -> {
                    Event item = getItem();
                    if (item != null) {
                        confirmDelete(item);
                    }
                });
            }

            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText(item.getName() != null ? item.getName() : "—");
                    LocalDateTime d = item.getEventDate();
                    int max = item.getMaxPlaces();
                    String places = max > 0
                            ? (item.getCurrentParticipants() + " / " + max)
                            : (item.getCurrentParticipants() + " / ∞");
                    meta.setText(String.join("  ·  ",
                            d == null ? "—" : d.format(DT),
                            item.getLocation() != null ? item.getLocation() : "—",
                            String.format(Locale.FRANCE, "%.2f TND", item.getPrice()),
                            item.getEventType() != null ? item.getEventType() : "—",
                            "Places : " + places));
                    desc.setText(shortDesc(item.getDescription()));
                    int count = item.getCurrentParticipants();
                    registrantsBtn.setText("👥 Inscrits (" + count + ")");
                    registrantsBtn.setOnAction(ev -> exportRegistrantsExcel(item));
                    setText(null);
                    setGraphic(card);
                }
            }
        });
    }

    private static String shortDesc(String d) {
        if (d == null || d.isBlank()) {
            return "—";
        }
        String t = d.trim().replaceAll("\\s+", " ");
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }

    private void loadEvents() {
        try {
            List<Event> events = eventService.afficher();
            Map<Integer, String> summaries = registrationService.loadRegistrantSummariesByEvent();
            Map<Integer, Integer> counts = registrationService.loadRegistrantCountsByEvent();
            for (Event e : events) {
                e.setRegistrantsSummary(summaries.getOrDefault(e.getId(), "—"));
                e.setCurrentParticipants(counts.getOrDefault(e.getId(), 0));
            }
            eventList.setItems(FXCollections.observableArrayList(events));
        } catch (SQLException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    private void exportRegistrantsExcel(Event event) {
        try {
            List<RegistrationService.RegistrantExportRow> rows = registrationService.listRegistrantsForEvent(event.getId());
            if (rows.isEmpty()) {
                showError("Aucun inscrit pour cet événement.");
                return;
            }
            showRegistrantsDialog(event, rows);
        } catch (Exception e) {
            showError("Affichage impossible : " + e.getMessage());
        }
    }

    private void showRegistrantsDialog(Event event, List<RegistrationService.RegistrantExportRow> rows) {
        TableView<RegistrationService.RegistrantExportRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RegistrationService.RegistrantExportRow, String> colFirst = new TableColumn<>("Prénom");
        colFirst.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().firstName()));

        TableColumn<RegistrationService.RegistrantExportRow, String> colLast = new TableColumn<>("Nom");
        colLast.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().lastName()));

        TableColumn<RegistrationService.RegistrantExportRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().email()));

        TableColumn<RegistrationService.RegistrantExportRow, String> colCheck = new TableColumn<>("Check-in");
        colCheck.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().checkedIn() ? "✔ Présent" : "✖ Absent"));
        colCheck.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTextFill(item.startsWith("✔") ? Color.web("#2e7d32") : Color.web("#c62828"));
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        table.getColumns().addAll(colFirst, colLast, colEmail, colCheck);
        table.getItems().setAll(rows);

        Label title = new Label("Inscrits - " + (event.getName() != null ? event.getName() : "Événement"));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill:#1b5e20;");
        Label count = new Label(rows.size() + " inscrit(s)");
        count.setStyle("-fx-text-fill:#455a64;");

        Button scanBtn = new Button("Scanner QR code");
        scanBtn.getStyleClass().add("btn-primary");
        scanBtn.setOnAction(e -> handleScanQrFile(event, table, count));
        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().add("btn-outline");
        HBox actions = new HBox(8, scanBtn, closeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, title, count, table, actions);
        root.setPadding(new Insets(14));

        Stage st = new Stage();
        st.setTitle("bledna — liste des inscrits");
        Scene sc = new Scene(root, 720, 460);
        StyleHelper.apply(sc);
        st.setScene(sc);
        closeBtn.setOnAction(e -> st.close());
        Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
        if (owner != null) {
            st.initOwner(owner);
            owner.hide();
        }
        try {
            st.showAndWait();
        } finally {
            if (owner != null) {
                owner.show();
            }
        }
    }

    private void handleScanQrFile(Event event, TableView<RegistrationService.RegistrantExportRow> table, Label count) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir le reçu (PDF ou image)");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers QR", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif", "*.webp"));
            File file = chooser.showOpenDialog(eventList.getScene().getWindow());
            if (file == null) {
                return;
            }
            String raw = decodeQrFromFile(file);
            if (raw == null || raw.isBlank()) {
                showQrError("Aucun QR valide détecté dans ce fichier.");
                return;
            }

            boolean ok = registrationService.markCheckedInFromReceiptCode(event.getId(), raw);
            if (!ok) {
                showQrError("QR détecté, mais code non valide pour cet événement.");
                return;
            }
            refreshRegistrantsTable(event, table, count);
        } catch (Exception ex) {
            showError("Scan QR impossible : " + ex.getMessage());
        }
    }

    private String decodeQrFromFile(File file) throws Exception {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(file)) {
                if (doc.getNumberOfPages() == 0) {
                    return null;
                }
                PDFRenderer renderer = new PDFRenderer(doc);
                BufferedImage image = renderer.renderImageWithDPI(0, 220);
                return decodeQrFromImage(image);
            }
        }
        BufferedImage image = ImageIO.read(file);
        return decodeQrFromImage(image);
    }

    private String decodeQrFromImage(BufferedImage image) throws Exception {
        if (image == null) {
            return null;
        }
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new MultiFormatReader().decode(bitmap).getText();
    }

    private void refreshRegistrantsTable(Event event, TableView<RegistrationService.RegistrantExportRow> table, Label count) throws SQLException {
        List<RegistrationService.RegistrantExportRow> refreshed = registrationService.listRegistrantsForEvent(event.getId());
        table.getItems().setAll(refreshed);
        count.setText(refreshed.size() + " inscrit(s)");
    }

    private void showQrError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("bledna");
        a.setHeaderText("QR incorrect");
        a.setContentText(message);
        if (a.getDialogPane() != null) {
            a.getDialogPane().setStyle("-fx-background-color:#fff5f5;");
            var header = a.getDialogPane().lookup(".header-panel .label");
            if (header != null) {
                header.setStyle("-fx-text-fill:#c62828; -fx-font-weight:bold;");
            }
            var content = a.getDialogPane().lookup(".content.label");
            if (content != null) {
                content.setStyle("-fx-text-fill:#c62828;");
            }
        }
        a.showAndWait();
    }

    private void showDetails(Event event) {
        LocalDateTime d = event.getEventDate();
        String body = String.join("\n",
                "Nom : " + event.getName(),
                "Description : " + (event.getDescription() != null ? event.getDescription() : "—"),
                "Date : " + (d == null ? "—" : d.format(DT)),
                "Lieu : " + (event.getLocation() != null ? event.getLocation() : "—"),
                String.format(Locale.FRANCE, "Prix : %.2f TND", event.getPrice()),
                "Type : " + (event.getEventType() != null ? event.getEventType() : "—"),
                "Paiement (événement) : " + (event.getPaymentType() != null ? event.getPaymentType() : "—"),
                "Places : " + event.getCurrentParticipants() + " / " + (event.getMaxPlaces() > 0 ? event.getMaxPlaces() : "∞"),
                "Statut : " + (event.getStatus() != null ? event.getStatus() : "—"),
                "Inscrits : " + (event.getRegistrantsSummary() != null ? event.getRegistrantsSummary() : "—")
        );
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(event.getName());
        a.setContentText(body);
        a.showAndWait();
    }

    private void openEdit(Event event) {
        try {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.hide();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/EventEdit.fxml"));
            Stage st = new Stage();
            st.setTitle("bledna — modifier l'événement");
            Scene sc = new Scene(loader.load(), 820, 620);
            StyleHelper.apply(sc);
            st.setScene(sc);
            if (owner != null) {
                st.initOwner(owner);
            }
            loader.<EventEditController>getController().setEventToEdit(event);
            st.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.show();
            }
        }
    }

    private void confirmDelete(Event event) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setContentText("Supprimer « " + event.getName() + " » ?");
        Optional<ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                eventService.supprimer(event.getId());
                loadEvents();
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.hide();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/esprit/EventAdd.fxml"));
            Stage st = new Stage();
            st.setTitle("bledna — nouvel événement");
            Scene sc = new Scene(loader.load(), 820, 620);
            StyleHelper.apply(sc);
            st.setScene(sc);
            if (owner != null) {
                st.initOwner(owner);
            }
            st.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showError(e.getMessage());
        } finally {
            Stage owner = eventList.getScene() != null && eventList.getScene().getWindow() instanceof Stage s ? s : null;
            if (owner != null) {
                owner.show();
            }
        }
    }

    private void showError(String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(m);
        a.showAndWait();
    }
}

