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
    @FXML private TableColumn<WasteCollection, String>        colImage;

    private final WasteCollectionDAO dao = new WasteCollectionDAO();
    private final ObservableList<WasteCollection> data = FXCollections.observableArrayList();
    private final AIService aiService = new com.bledna.util.RoboflowAIService();


    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
                
                setCursor(javafx.scene.Cursor.HAND);
                setOnMouseClicked(e -> {
                    if (!isEmpty() && getItem() != null) {
                        System.out.println("Image clicked: " + getItem());
                        showLargeImage(getItem());
                    }
                });
            }
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isBlank()) {
                    setGraphic(null);
                } else {
                    try {
                        File file = new File(path);
                        if (file.exists()) {
                            imageView.setImage(new Image(file.toURI().toString()));
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });
        colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

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
            Button btnEdit   = new Button("✏ Edit");
            Button btnDelete = new Button("🗑");
            HBox   box       = new HBox(6, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(2, 0, 2, 4));
                btnEdit.setOnAction(e -> showDialog(getTableView().getItems().get(getIndex())));
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
    private void handleNew() { showDialog(null); }

    private void handleDelete(WasteCollection item) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer \"" + item.getLocationName() + "\" ?")) return;
        try {
            dao.delete(item.getId());
            data.remove(item);
        } catch (SQLException e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    private void showDialog(WasteCollection existing) {
        boolean isEdit = (existing != null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier Waste" : "Nouveau Waste");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // ── Champs du formulaire
        TextField locationField = new TextField();
        TextField quantityField = new TextField();
        TextField gpsField      = new TextField();
        TextField timeField     = new TextField("08:00");
        TextArea  descArea      = new TextArea();

        ComboBox<String> typeBox   = new ComboBox<>(FXCollections.observableArrayList("PLASTIC","ORGANIC","METAL","GLASS","ELECTRONIC","OTHER"));
        ComboBox<String> unitBox   = new ComboBox<>(FXCollections.observableArrayList("kg","g","t","L"));
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("PENDING","IN_PROGRESS","COMPLETED","CANCELLED"));
        DatePicker datePicker      = new DatePicker(LocalDate.now());

        // verifier img
        ImageView preview = new ImageView();
        preview.setFitWidth(100); preview.setFitHeight(100);
        preview.setPreserveRatio(true);
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false);
        Button btnPick = new Button("📷 Browse");
        Button btnAnalyze = new Button("🤖 AI Analyze");
        btnAnalyze.setDisable(true);
        //ableau d'un élément pour pouvoir modifier la variable

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
            
            // Run in background to avoid freezing UI
            new Thread(() -> {
                String result = aiService.analyzeWaste(selectedFile[0]);
                javafx.application.Platform.runLater(() -> {
                    String cleanResult = result.toUpperCase();
                    
                    // Detect type from keywords in caption
                    String detectedType = "OTHER";
                    if (cleanResult.contains("PLASTIC") || cleanResult.contains("BOTTLE") || cleanResult.contains("CONTAINER")) {
                        detectedType = "PLASTIC";
                    } else if (cleanResult.contains("FOOD") || cleanResult.contains("ORGANIC") || cleanResult.contains("FRUIT") || cleanResult.contains("VEGETABLE")) {
                        detectedType = "ORGANIC";
                    } else if (cleanResult.contains("METAL") || cleanResult.contains("CAN") || cleanResult.contains("IRON") || cleanResult.contains("STEEL")) {
                        detectedType = "METAL";
                    } else if (cleanResult.contains("GLASS") || cleanResult.contains("WINE") || cleanResult.contains("JAR")) {
                        detectedType = "GLASS";
                    } else if (cleanResult.contains("ELECTRONIC") || cleanResult.contains("PHONE") || cleanResult.contains("COMPUTER") || cleanResult.contains("BATTERY")) {
                        detectedType = "ELECTRONIC";
                    }
                    
                    typeBox.setValue(detectedType);
                    
                    // Set advice based on type
                    String advice = "Please dispose of this responsibly according to local guidelines.";
                    switch (detectedType) {
                        case "PLASTIC": advice = "Rinse and place in the yellow recycling bin. Avoid single-use plastics."; break;
                        case "ORGANIC": advice = "Perfect for composting! Avoid mixing with non-biodegradables."; break;
                        case "METAL": advice = "Metals are infinitely recyclable. Keep them clean and dry."; break;
                        case "GLASS": advice = "Recycle in the glass container. Remove caps and rinse if possible."; break;
                        case "ELECTRONIC": advice = "Don't throw in trash! Take to a specialized E-waste collection point."; break;
                    }

                    descArea.setText("AI Detection: " + result + "\n\nRecycling Advice: " + advice);
                    btnAnalyze.setText("🤖 AI Analyze");
                    btnAnalyze.setDisable(false);
                });
            }).start();
        });

        typeBox.setValue("PLASTIC");
        unitBox.setValue("kg");
        statusBox.setValue("PENDING");
        descArea.setPrefRowCount(2);

        // Pré-remplir si modification
        if (isEdit) {
            locationField.setText(existing.getLocationName() != null ? existing.getLocationName() : "");
            typeBox.setValue(existing.getWasteType() != null ? existing.getWasteType() : "PLASTIC");
            quantityField.setText(String.valueOf(existing.getQuantity()));
            unitBox.setValue(existing.getUnit() != null ? existing.getUnit() : "kg");
            if (existing.getCollectionDate() != null) {
                datePicker.setValue(existing.getCollectionDate().toLocalDate());
                timeField.setText(existing.getCollectionDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            gpsField.setText(existing.getGpsLocation() != null ? existing.getGpsLocation() : "");
            statusBox.setValue(existing.getStatus() != null ? existing.getStatus() : "PENDING");
            descArea.setText(existing.getDescription() != null ? existing.getDescription() : "");
            if (existing.getImagePath() != null) {
                File f = new File(existing.getImagePath());
                if (f.exists()) {
                    preview.setImage(new Image(f.toURI().toString()));
                    imagePathField.setText(f.getAbsolutePath());
                }
            }
        }

        // ── Mise en page
        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Location *"),  0, 0); grid.add(locationField, 1, 0);
        grid.add(new Label("Type *"),      0, 1); grid.add(typeBox,       1, 1);
        grid.add(new Label("Quantité *"),  0, 2); grid.add(new HBox(8, quantityField, unitBox), 1, 2);
        grid.add(new Label("Date *"),      0, 3); grid.add(new HBox(8, datePicker, new Label("H:"), timeField), 1, 3);
        grid.add(new Label("GPS"),         0, 4); grid.add(gpsField,      1, 4);
        grid.add(new Label("Statut"),      0, 5); grid.add(statusBox,     1, 5);
        grid.add(new Label("Description"), 0, 6); grid.add(descArea,      1, 6);
        grid.add(new Label("Image"),       0, 7); 
        grid.add(new HBox(10, imagePathField, btnPick, btnAnalyze), 1, 7);
        grid.add(preview, 1, 8);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(650);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveBtn) return;

        // ── Contrôle de saisie ────────────────────────────────────────────
        // 1. Location obligatoire
        if (locationField.getText().isBlank()) {
            AlertUtil.showError("Champ manquant", "La localisation est obligatoire.");
            return;
        }

        // 2. Quantité obligatoire et doit être un nombre positif
        double qty;
        try {
            qty = Double.parseDouble(quantityField.getText().trim());
            if (qty < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertUtil.showError("Quantité invalide", "Entrez un nombre positif. Exemple : 10.5");
            return;
        }

        // 3. Date obligatoire
        if (datePicker.getValue() == null) {
            AlertUtil.showError("Date manquante", "Veuillez sélectionner une date.");
            return;
        }

        // 4. Heure au bon format HH:mm
        LocalTime heure = LocalTime.of(8, 0);
        try {
            heure = LocalTime.parse(timeField.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            AlertUtil.showError("Heure invalide", "Format attendu : HH:mm  Exemple : 08:30");
            return;
        }

        // 5. GPS optionnel mais si rempli doit contenir une virgule (lat,lng)
        if (!gpsField.getText().isBlank() && !gpsField.getText().contains(",")) {
            AlertUtil.showError("GPS invalide", "Format attendu : latitude,longitude  Exemple : 36.73,3.08");
            return;
        }

        // ── Sauvegarder
        WasteCollection w = isEdit ? existing : new WasteCollection();
        if (!isEdit) {
            int userId = UserSession.getCurrentUserId();
            w.setCollectorId(userId != -1 ? userId : 1);
        }
        w.setLocationName(locationField.getText().trim());
        w.setWasteType(typeBox.getValue());
        w.setQuantity(qty);
        w.setUnit(unitBox.getValue());
        w.setCollectionDate(LocalDateTime.of(datePicker.getValue(), heure));
        w.setGpsLocation(gpsField.getText().trim());
        w.setStatus(statusBox.getValue());
        w.setDescription(descArea.getText().trim());

        // Save image to uploads folder
        if (selectedFile[0] != null) {
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) uploadDir.mkdir();
                String fileName = System.currentTimeMillis() + "_" + selectedFile[0].getName();
                File destFile = new File(uploadDir, fileName);
                Files.copy(selectedFile[0].toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                w.setImagePath(destFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error saving image: " + e.getMessage());
            }
        }

        try {
            if (isEdit) {
                dao.update(w);
                data.set(data.indexOf(existing), w);
                AlertUtil.showInfo("Modifié", "Collecte modifiée avec succès.");
            } else {
                dao.insert(w);
                AlertUtil.showInfo("Ajouté", "Collecte ajoutée avec succès.");
                loadData();
            }
        } catch (SQLException ex) {
            AlertUtil.showError("Erreur BDD", ex.getMessage());
        }
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