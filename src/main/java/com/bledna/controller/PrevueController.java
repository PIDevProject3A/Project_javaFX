package com.bledna.controller;

import com.bledna.dao.PrevueCollectionDAO;
import com.bledna.dao.WasteCollectionDAO;
import com.bledna.model.PrevueCollection;
import com.bledna.model.WasteCollection;
import com.bledna.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import com.esprit.utils.UserSession;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrevueController {

    @FXML private TableView<PrevueCollection>            tableView;
    @FXML private TableColumn<PrevueCollection, Integer> colId;
    @FXML private TableColumn<PrevueCollection, String>  colTypeCollection;
    @FXML private TableColumn<PrevueCollection, Double>  colQuantite;
    @FXML private TableColumn<PrevueCollection, String>  colUnit;
    @FXML private TableColumn<PrevueCollection, String>  colStatut;
    @FXML private TableColumn<PrevueCollection, java.time.LocalDateTime> colDate;
    @FXML private TableColumn<PrevueCollection, Integer> colWasteId;
    @FXML private TableColumn<PrevueCollection, Void>    colActions;
    @FXML private Label                                  lblCountdown;
    @FXML private VBox formContainer;

    private final PrevueCollectionDAO dao      = new PrevueCollectionDAO();
    private final WasteCollectionDAO  wasteDAO = new WasteCollectionDAO();
    private final ObservableList<PrevueCollection> data = FXCollections.observableArrayList();
    private List<WasteCollection> wasteList = new ArrayList<>();

    @FXML
    public void initialize() {
        try { 
            int userId = UserSession.getCurrentUserId();
            if (userId != -1) {
                wasteList = wasteDAO.getByCollector(userId);
            } else {
                wasteList = wasteDAO.getAll(); 
            }
        } catch (SQLException ignored) {}
        setupColumns();
        loadData();
        startCountdown();
    }

    private void startCountdown() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateCountdown() {
        if (data == null || data.isEmpty()) {
            lblCountdown.setText("No scheduled collections");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        PrevueCollection next = data.stream()
                .filter(p -> p.getCollectionDate() != null && p.getCollectionDate().isAfter(now))
                .min((p1, p2) -> p1.getCollectionDate().compareTo(p2.getCollectionDate()))
                .orElse(null);

        if (next == null) {
            lblCountdown.setText("None upcoming");
            return;
        }

        long seconds = ChronoUnit.SECONDS.between(now, next.getCollectionDate());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, secs));
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTypeCollection.setCellValueFactory(new PropertyValueFactory<>("typeCollection"));
        colQuantite.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("collectionDate"));
        colWasteId.setCellValueFactory(new PropertyValueFactory<>("wasteCollectionId"));

        // Boutons Edit et Delete sur chaque ligne
        colActions.setCellFactory(col -> new TableCell<>() {
            Button btnEdit   = new Button("🖋");
            Button btnDelete = new Button("🗑");
            HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(2, 0, 2, 4));
                btnEdit.getStyleClass().add("btn-edit");
                btnDelete.getStyleClass().add("btn-delete");
                btnEdit.setOnAction(e -> showForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableView.setItems(data);
    }

    private void loadData() {
        data.clear();
        try {
            int userId = UserSession.getCurrentUserId();
            if (userId != -1) {
                data.addAll(dao.getByCollector(userId));
            } else {
                data.addAll(dao.getAll());
            }
        } catch (SQLException e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleNew() { showForm(null); }

    private void handleDelete(PrevueCollection item) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer \"" + item.getTypeCollection() + "\" ?")) return;
        try {
            dao.delete(item.getId());
            data.remove(item);
        } catch (SQLException e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    private void showForm(PrevueCollection existing) {
        boolean isEdit = (existing != null);
        formContainer.getChildren().clear();
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        // ── Header
        Label header = new Label(isEdit ? "✏ Edit Prévue" : "➕ New Prévue");
        header.getStyleClass().add("form-title");
        formContainer.getChildren().add(header);

        // ── Fields
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("PLASTIC","ORGANIC","METAL","GLASS","ELECTRONIC","OTHER"));
        typeBox.getStyleClass().add("form-field");
        typeBox.setValue("PLASTIC");

        TextField quantiteField = new TextField();
        quantiteField.getStyleClass().add("form-field");
        quantiteField.setPrefWidth(120);

        ComboBox<String> unitBox = new ComboBox<>(FXCollections.observableArrayList("kg","g","t","L"));
        unitBox.getStyleClass().add("form-field");
        unitBox.setValue("kg");

        ComboBox<String> statutBox = new ComboBox<>(FXCollections.observableArrayList("PENDING", "IN_PROGRESS", "COMPLETED"));
        statutBox.getStyleClass().add("form-field");
        statutBox.setValue("PENDING");

        DatePicker datePicker = new DatePicker(java.time.LocalDate.now());
        datePicker.getStyleClass().add("form-field");

        TextField timeField = new TextField("08:00");
        timeField.getStyleClass().add("form-field");
        timeField.setPrefWidth(80);

        ComboBox<WasteCollection> wasteBox = new ComboBox<>();
        wasteBox.getStyleClass().add("form-field");
        wasteBox.setConverter(new StringConverter<>() {
            @Override public String toString(WasteCollection w) {
                return w == null ? "— Aucun —" : "#" + w.getId() + " – " + w.getLocationName();
            }
            @Override public WasteCollection fromString(String s) { return null; }
        });
        List<WasteCollection> items = new ArrayList<>();
        items.add(null);
        items.addAll(wasteList);
        wasteBox.setItems(FXCollections.observableArrayList(items));

        // Pre-fill
        if (isEdit) {
            typeBox.setValue(existing.getTypeCollection());
            quantiteField.setText(String.valueOf(existing.getQuantite()));
            unitBox.setValue(existing.getUnit() != null ? existing.getUnit() : "kg");
            statutBox.setValue(existing.getStatut());
            if (existing.getCollectionDate() != null) {
                datePicker.setValue(existing.getCollectionDate().toLocalDate());
                timeField.setText(existing.getCollectionDate().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            wasteList.stream()
                    .filter(w -> w.getId() == existing.getWasteCollectionId())
                    .findFirst()
                    .ifPresent(wasteBox::setValue);
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(30); grid.setVgap(15);
        
        // Row 0: Type & Statut
        grid.add(createLabel("Type *"),           0, 0); grid.add(typeBox,       1, 0);
        grid.add(createLabel("Statut"),           2, 0); grid.add(statutBox,     3, 0);
        
        // Row 1: Quantité & Date
        grid.add(createLabel("Quantité *"),        0, 1); grid.add(new HBox(10, quantiteField, unitBox), 1, 1);
        grid.add(createLabel("Date *"),            2, 1); grid.add(new HBox(10, datePicker, createLabel("at"), timeField), 3, 1);
        
        // Row 2: Waste Collection (Full width)
        grid.add(createLabel("Waste Collection"),  0, 2); 
        grid.add(wasteBox,      1, 2, 3, 1);
        wasteBox.setMaxWidth(Double.MAX_VALUE);

        // Buttons
        Button btnSave = new Button("✔ Save Prévue");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("✖ Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        HBox actions = new HBox(15, btnSave, btnCancel);
        actions.setPadding(new Insets(15, 0, 0, 0));

        btnCancel.setOnAction(e -> {
            formContainer.setVisible(false);
            formContainer.setManaged(false);
        });

        btnSave.setOnAction(e -> {
            if (typeBox.getValue() == null || typeBox.getValue().isBlank()) { AlertUtil.showError("Missing", "Type is required"); return; }
            double qty;
            try { qty = Double.parseDouble(quantiteField.getText().trim()); if (qty < 0) throw new Exception(); }
            catch (Exception ex) { AlertUtil.showError("Invalid Qty", "Enter positive number"); return; }
            if (datePicker.getValue() == null) { AlertUtil.showError("Missing", "Date is required"); return; }

            PrevueCollection p = isEdit ? existing : new PrevueCollection();
            if (!isEdit) p.setCollectorId(UserSession.getCurrentUserId() != -1 ? UserSession.getCurrentUserId() : 1);
            p.setTypeCollection(typeBox.getValue());
            p.setQuantite(qty);
            p.setUnit(unitBox.getValue());
            p.setStatut(statutBox.getValue());
            
            java.time.LocalTime heure = java.time.LocalTime.of(8, 0);
            try { heure = java.time.LocalTime.parse(timeField.getText().trim(), java.time.format.DateTimeFormatter.ofPattern("HH:mm")); }
            catch (Exception ignored) {}
            p.setCollectionDate(java.time.LocalDateTime.of(datePicker.getValue(), heure));

            WasteCollection selected = wasteBox.getValue();
            p.setWasteCollectionId(selected != null ? selected.getId() : 0);

            try {
                if (isEdit) { dao.update(p); data.set(data.indexOf(existing), p); }
                else { dao.insert(p); loadData(); }
                formContainer.setVisible(false);
                formContainer.setManaged(false);
                AlertUtil.showInfo("Success", isEdit ? "Updated" : "Added");
            } catch (SQLException ex) { AlertUtil.showError("DB Error", ex.getMessage()); }
        });

        formContainer.getChildren().addAll(grid, actions);
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }
}