package com.bledna.controller;

import com.bledna.dao.WasteCollectionDAO;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import com.bledna.util.AIService;
import com.esprit.utils.UserSession;

import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class WasteController {

    @FXML private TableView<WasteCollection>                  tableView;
    @FXML private TableColumn<WasteCollection, Integer>       colId;
    @FXML private TableColumn<WasteCollection, String>        colLocationName;
    @FXML private TableColumn<WasteCollection, String>        colWasteType;
    @FXML private TableColumn<WasteCollection, Double>        colQuantity;
    @FXML private TableColumn<WasteCollection, String>        colUnit;
    @FXML private TableColumn<WasteCollection, String>        colStatus;
    @FXML private TableColumn<WasteCollection, LocalDateTime> colDate;
    @FXML private TableColumn<WasteCollection, String>        colDescription;
    @FXML private TableColumn<WasteCollection, Void>          colActions;
    @FXML private VBox formContainer;

    private final WasteCollectionDAO dao = new WasteCollectionDAO();
    private final ObservableList<WasteCollection> data = FXCollections.observableArrayList();
    private final AIService aiService = new com.bledna.util.RoboflowAIService();


    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLocationName.setCellValueFactory(new PropertyValueFactory<>("locationName"));
        colWasteType.setCellValueFactory(new PropertyValueFactory<>("wasteType"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("collectionDate"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

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

    private void handleDelete(WasteCollection item) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer \"" + item.getLocationName() + "\" ?")) return;
        try {
            dao.delete(item.getId());
            data.remove(item);
        } catch (SQLException e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    private void showForm(WasteCollection existing) {
        boolean isEdit = (existing != null);
        formContainer.getChildren().clear();
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        // ── Header
        Label header = new Label(isEdit ? "✏ Edit Collection" : "➕ New Collection");
        header.getStyleClass().add("form-title");
        formContainer.getChildren().add(header);

        // ── Fields
        TextField locationField = new TextField();
        locationField.getStyleClass().add("form-field");
        
        TextField quantityField = new TextField();
        quantityField.getStyleClass().add("form-field");
        quantityField.setPrefWidth(100);
        
        TextField gpsField      = new TextField();
        gpsField.getStyleClass().add("form-field");
        
        TextField timeField     = new TextField("08:00");
        timeField.getStyleClass().add("form-field");
        timeField.setPrefWidth(80);
        
        TextArea  descArea      = new TextArea();
        descArea.getStyleClass().add("form-field");
        descArea.setPrefRowCount(2);

        ComboBox<String> typeBox   = new ComboBox<>(FXCollections.observableArrayList("PLASTIC","ORGANIC","METAL","GLASS","ELECTRONIC","OTHER"));
        typeBox.getStyleClass().add("form-field");
        
        ComboBox<String> unitBox   = new ComboBox<>(FXCollections.observableArrayList("kg","g","t","L"));
        unitBox.getStyleClass().add("form-field");
        
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("PENDING","IN_PROGRESS","COMPLETED","CANCELLED"));
        statusBox.getStyleClass().add("form-field");
        
        DatePicker datePicker      = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("form-field");

        // ── Photo UI (Only for New or if user wants to keep it)
        ImageView preview = new ImageView();
        preview.setFitWidth(100); preview.setFitHeight(100);
        preview.setPreserveRatio(true);
        TextField imagePathField = new TextField();
        imagePathField.getStyleClass().add("form-field");
        imagePathField.setEditable(false);
        imagePathField.setPrefWidth(200);
        
        Button btnPick = new Button("📷 Browse");
        btnPick.getStyleClass().add("btn-secondary");
        
        Button btnAnalyze = new Button("🤖 AI Analyze");
        btnAnalyze.getStyleClass().add("btn-secondary");
        btnAnalyze.setDisable(true);

        final File[] selectedFile = {null};

        btnPick.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(null);
            if (file != null) {
                selectedFile[0] = file;
                imagePathField.setText(file.getAbsolutePath());
                preview.setImage(new Image(file.toURI().toString()));
                btnAnalyze.setDisable(false);
            }
        });

        btnAnalyze.setOnAction(e -> {
            if (selectedFile[0] == null) return;
            btnAnalyze.setText("⏳ Analyzing...");
            btnAnalyze.setDisable(true);
            new Thread(() -> {
                String result = aiService.analyzeWaste(selectedFile[0]);
                javafx.application.Platform.runLater(() -> {
                    String cleanResult = result.toUpperCase();
                    String detectedType = "OTHER";
                    if (cleanResult.contains("PLASTIC") || cleanResult.contains("BOTTLE") || cleanResult.contains("CONTAINER")) detectedType = "PLASTIC";
                    else if (cleanResult.contains("FOOD") || cleanResult.contains("ORGANIC")) detectedType = "ORGANIC";
                    else if (cleanResult.contains("METAL") || cleanResult.contains("CAN")) detectedType = "METAL";
                    else if (cleanResult.contains("GLASS") || cleanResult.contains("JAR")) detectedType = "GLASS";
                    else if (cleanResult.contains("ELECTRONIC") || cleanResult.contains("PHONE")) detectedType = "ELECTRONIC";
                    
                    typeBox.setValue(detectedType);
                    String advice = "Please dispose of this responsibly.";
                    switch (detectedType) {
                        case "PLASTIC": advice = "Rinse and place in yellow bin."; break;
                        case "ORGANIC": advice = "Perfect for composting!"; break;
                        case "METAL": advice = "Metals are infinitely recyclable."; break;
                        case "GLASS": advice = "Recycle in glass container."; break;
                        case "ELECTRONIC": advice = "Take to E-waste point."; break;
                    }
                    descArea.setText("AI Detection: " + result + "\n\nAdvice: " + advice);
                    btnAnalyze.setText("🤖 AI Analyze");
                    btnAnalyze.setDisable(false);
                });
            }).start();
        });


        // Pre-fill
        if (isEdit) {
            locationField.setText(existing.getLocationName());
            typeBox.setValue(existing.getWasteType());
            quantityField.setText(String.valueOf(existing.getQuantity()));
            unitBox.setValue(existing.getUnit());
            if (existing.getCollectionDate() != null) {
                datePicker.setValue(existing.getCollectionDate().toLocalDate());
                timeField.setText(existing.getCollectionDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            gpsField.setText(existing.getGpsLocation());
            statusBox.setValue(existing.getStatus());
            descArea.setText(existing.getDescription());
        } else {
            typeBox.setValue("PLASTIC");
            unitBox.setValue("kg");
            statusBox.setValue("PENDING");
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(30); grid.setVgap(15);
        
        // Row 0: Location (Full width)
        grid.add(createLabel("Location *"),  0, 0); 
        grid.add(locationField, 1, 0, 3, 1);
        
        // Row 1: Type & Statut
        grid.add(createLabel("Type *"),      0, 1); grid.add(typeBox,       1, 1);
        grid.add(createLabel("Statut"),      2, 1); grid.add(statusBox,     3, 1);
        
        // Row 2: Quantité & Date
        grid.add(createLabel("Quantité *"),  0, 2); grid.add(new HBox(8, quantityField, unitBox), 1, 2);
        grid.add(createLabel("Date *"),      2, 2); grid.add(new HBox(8, datePicker, createLabel("at"), timeField), 3, 2);
        
        // Row 3: GPS & Description
        grid.add(createLabel("GPS"),         0, 3); grid.add(gpsField,      1, 3);
        grid.add(createLabel("Description"), 2, 3); grid.add(descArea,      3, 3);
        
        // Row 4: Image (Hide if Edit as requested)
        if (!isEdit) {
            grid.add(createLabel("Image"),       0, 4); 
            HBox imgButtons = new HBox(10, imagePathField, btnPick, btnAnalyze);
            imgButtons.setAlignment(Pos.CENTER_LEFT);
            grid.add(imgButtons, 1, 4, 3, 1);
            grid.add(preview, 1, 5, 3, 1);
        }
        

        // Buttons
        Button btnSave = new Button("✔ Save Collection");
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
            if (locationField.getText().isBlank()) { AlertUtil.showError("Missing", "Location is required"); return; }
            double qty;
            try { qty = Double.parseDouble(quantityField.getText().trim()); if (qty < 0) throw new Exception(); }
            catch (Exception ex) { AlertUtil.showError("Invalid Qty", "Enter positive number"); return; }
            if (datePicker.getValue() == null) { AlertUtil.showError("Missing", "Date is required"); return; }
            LocalTime heure;
            try { heure = LocalTime.parse(timeField.getText().trim(), DateTimeFormatter.ofPattern("HH:mm")); }
            catch (Exception ex) { AlertUtil.showError("Invalid Time", "Use HH:mm"); return; }

            WasteCollection w = isEdit ? existing : new WasteCollection();
            if (!isEdit) w.setCollectorId(UserSession.getCurrentUserId() != -1 ? UserSession.getCurrentUserId() : 1);
            w.setLocationName(locationField.getText().trim());
            w.setWasteType(typeBox.getValue());
            w.setQuantity(qty);
            w.setUnit(unitBox.getValue());
            w.setCollectionDate(LocalDateTime.of(datePicker.getValue(), heure));
            w.setGpsLocation(gpsField.getText().trim());
            w.setStatus(statusBox.getValue());
            w.setDescription(descArea.getText().trim());

            if (selectedFile[0] != null) {
                try {
                    File uploadDir = new File("uploads");
                    if (!uploadDir.exists()) uploadDir.mkdir();
                    File destFile = new File(uploadDir, System.currentTimeMillis() + "_" + selectedFile[0].getName());
                    Files.copy(selectedFile[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    w.setImagePath(destFile.getAbsolutePath());
                } catch (IOException ex) { System.err.println("Img Err: " + ex.getMessage()); }
            }

            try {
                if (isEdit) { dao.update(w); data.set(data.indexOf(existing), w); }
                else { dao.insert(w); loadData(); }
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

    @FXML
    private void showStatistics() {
        try {
            // Find the StackPane from the scene (it's in main.fxml)
            StackPane contentPane = (StackPane) tableView.getScene().lookup("#contentPane");
            if (contentPane != null) {
                Node view = FXMLLoader.load(getClass().getResource("/com/bledna/statistics_view.fxml"));
                contentPane.getChildren().setAll(view);
            }
        } catch (IOException e) {
            AlertUtil.showError("Erreur", "Impossible de charger les statistiques : " + e.getMessage());
        }
    }

    private void showLargeImage(String path) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Image Preview");
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        ImageView imageView = new ImageView();
        try {
            File file = new File(path);
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception e) {
            return;
        }

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setStyle("-fx-background: #0f172a; -fx-background-color: #0f172a;");
        
        javafx.scene.Scene scene = new javafx.scene.Scene(scrollPane);
        stage.setScene(scene);
        stage.show();
    }
}