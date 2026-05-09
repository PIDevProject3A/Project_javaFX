package com.esprit.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.esprit.utils.MyDataBase;
import com.esprit.utils.SceneNavigator;
import com.esprit.services.TransactionDao;
import com.esprit.entities.EcoTransaction;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class TransactionController {

    private static final String TYPE_INCOME = "Income";
    private static final String TYPE_ALLOCATION = "Allocation";
    private static final String TYPE_EXPENSE = "Expense";
    private static final String TYPE_REFUND = "Refund";

    private static final String IMPACT_NONE = "None";
    private static final String IMPACT_TREES = "Trees";
    private static final String IMPACT_WASTE = "Kg Waste";
    private static final String IMPACT_VOLUNTEERS = "Volunteers";

    private static final ObservableList<String> TRANSACTION_TYPES = FXCollections.observableArrayList(List.of(
        TYPE_INCOME,
        TYPE_ALLOCATION,
        TYPE_EXPENSE,
        TYPE_REFUND
    ));

    private static final ObservableList<String> SOURCE_TYPES = FXCollections.observableArrayList(List.of(
        "Donation Pool",
        "Recycling Sales",
        "Corporate Sponsor",
        "Grant",
        "Event Revenue",
        "Partner Contribution"
    ));

    private static final ObservableList<String> PURPOSES = FXCollections.observableArrayList(List.of(
        "Tree Planting",
        "Cleanup Campaign",
        "Community Event",
        "Equipment Purchase",
        "Awareness Program",
        "Emergency Fund",
        "General Operations"
    ));

    private static final ObservableList<String> IMPACT_UNITS = FXCollections.observableArrayList(List.of(
        IMPACT_NONE,
        IMPACT_TREES,
        IMPACT_WASTE,
        IMPACT_VOLUNTEERS
    ));

    private static final ObservableList<String> STATUSES = FXCollections.observableArrayList(List.of(
        "Draft",
        "Approved",
        "Completed",
        "Cancelled"
    ));

    @FXML
    private TableView<EcoTransaction> transactionTable;
    @FXML
    private TableColumn<EcoTransaction, Integer> idColumn;
    @FXML
    private TableColumn<EcoTransaction, String> refColumn;
    @FXML
    private TableColumn<EcoTransaction, String> typeColumn;
    @FXML
    private TableColumn<EcoTransaction, String> sourceColumn;
    @FXML
    private TableColumn<EcoTransaction, String> purposeColumn;
    @FXML
    private TableColumn<EcoTransaction, String> amountColumn;
    @FXML
    private TableColumn<EcoTransaction, String> impactColumn;
    @FXML
    private TableColumn<EcoTransaction, LocalDate> dateColumn;
    @FXML
    private TableColumn<EcoTransaction, String> statusColumn;

    @FXML
    private TextField referenceCodeField;
    @FXML
    private ComboBox<String> transactionTypeCombo;
    @FXML
    private ComboBox<String> sourceTypeCombo;
    @FXML
    private ComboBox<String> purposeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> impactUnitCombo;
    @FXML
    private Label impactQuantityLabel;
    @FXML
    private TextField impactQuantityField;
    @FXML
    private DatePicker transactionDatePicker;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea notesArea;

    @FXML
    private Label summaryLabel;
    @FXML
    private Label formStatusLabel;
    @FXML
    private PieChart sourcePieChart;
    @FXML
    private BarChart<String, Number> impactBarChart;

    private final ObservableList<EcoTransaction> transactions = FXCollections.observableArrayList();
    private final TransactionDao transactionDao = new TransactionDao();

    @FXML
    private void initialize() {
        configureTable();
        configureFormOptions();
        configureBehavior();
        bindSelectionToForm();

        setDefaultFormValues();

        try {
            MyDataBase.getInstance();
            loadTransactions();
            formStatusLabel.setText("Connected to database. Transactions loaded.");
        } catch (SQLException ex) {
            formStatusLabel.setText("Database error: " + ex.getMessage());
        }

        refreshSummary();
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        refColumn.setCellValueFactory(new PropertyValueFactory<>("referenceCode"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("transactionType"));
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourceType"));
        purposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose"));
        amountColumn.setCellValueFactory(cellData ->
            new ReadOnlyStringWrapper(String.format("%.3f TND", cellData.getValue().getAmount())));
        impactColumn.setCellValueFactory(cellData -> {
            EcoTransaction transaction = cellData.getValue();
            if (IMPACT_NONE.equals(transaction.getImpactUnit())) {
                return new ReadOnlyStringWrapper("None");
            }
            return new ReadOnlyStringWrapper(transaction.getImpactUnit() + ": " + transaction.getImpactQuantity());
        });
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        transactionTable.setItems(transactions);
    }

    private void configureFormOptions() {
        transactionTypeCombo.setItems(FXCollections.observableArrayList(TRANSACTION_TYPES));
        sourceTypeCombo.setItems(FXCollections.observableArrayList(SOURCE_TYPES));
        purposeCombo.setItems(FXCollections.observableArrayList(PURPOSES));
        impactUnitCombo.setItems(FXCollections.observableArrayList(IMPACT_UNITS));
        statusCombo.setItems(FXCollections.observableArrayList(STATUSES));
    }

    private void configureBehavior() {
        transactionTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyTransactionTypeRules(newValue));
        impactUnitCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyImpactUnitRules(newValue));
        purposeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPurposeRules(newValue));
    }

    private void applyPurposeRules(String purpose) {
        if (purpose == null) {
            return;
        }

        if (TYPE_INCOME.equals(transactionTypeCombo.getValue()) || TYPE_REFUND.equals(transactionTypeCombo.getValue())) {
            return;
        }

        if ("Tree Planting".equals(purpose)) {
            impactUnitCombo.setValue(IMPACT_TREES);
        } else if ("Cleanup Campaign".equals(purpose)) {
            impactUnitCombo.setValue(IMPACT_WASTE);
        }
    }

    private void applyTransactionTypeRules(String transactionType) {
        boolean isIncomeOrRefund = TYPE_INCOME.equals(transactionType) || TYPE_REFUND.equals(transactionType);

        impactUnitCombo.setDisable(isIncomeOrRefund);
        if (isIncomeOrRefund) {
            impactUnitCombo.setValue(IMPACT_NONE);
        } else if (impactUnitCombo.getValue() == null || IMPACT_NONE.equals(impactUnitCombo.getValue())) {
            impactUnitCombo.setValue(IMPACT_TREES);
        }

        applyImpactUnitRules(impactUnitCombo.getValue());
    }

    private void applyImpactUnitRules(String impactUnit) {
        boolean needsQuantity = impactUnit != null && !IMPACT_NONE.equals(impactUnit);
        setVisibleManaged(needsQuantity, impactQuantityLabel, impactQuantityField);

        if (!needsQuantity) {
            impactQuantityField.clear();
            return;
        }

        if (IMPACT_TREES.equals(impactUnit)) {
            impactQuantityLabel.setText("Trees count");
            impactQuantityField.setPromptText("Ex: 100");
        } else if (IMPACT_WASTE.equals(impactUnit)) {
            impactQuantityLabel.setText("Kg waste");
            impactQuantityField.setPromptText("Ex: 250");
        } else {
            impactQuantityLabel.setText("Volunteers");
            impactQuantityField.setPromptText("Ex: 40");
        }
    }

    private void bindSelectionToForm() {
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                return;
            }

            referenceCodeField.setText(newSelection.getReferenceCode());
            transactionTypeCombo.setValue(newSelection.getTransactionType());
            sourceTypeCombo.setValue(newSelection.getSourceType());
            purposeCombo.setValue(newSelection.getPurpose());
            amountField.setText(String.format("%.3f", newSelection.getAmount()));
            impactUnitCombo.setValue(newSelection.getImpactUnit());
            if (newSelection.getImpactQuantity() == null) {
                impactQuantityField.clear();
            } else {
                impactQuantityField.setText(String.valueOf(newSelection.getImpactQuantity()));
            }
            transactionDatePicker.setValue(newSelection.getTransactionDate());
            statusCombo.setValue(newSelection.getStatus());
            notesArea.setText(newSelection.getNotes());
            formStatusLabel.setText("Selected transaction ID " + newSelection.getId() + ". You can update or delete it.");
        });
    }

    @FXML
    private void createTransaction() {
        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        if (referenceCodeField.getText() == null || referenceCodeField.getText().trim().isEmpty()) {
            referenceCodeField.setText(generateReferenceCode(transactionTypeCombo.getValue()));
        }

        EcoTransaction transaction = new EcoTransaction(
            0,
            referenceCodeField.getText().trim().toUpperCase(),
            transactionTypeCombo.getValue(),
            sourceTypeCombo.getValue(),
            purposeCombo.getValue(),
            parseAmount(),
            impactUnitCombo.getValue(),
            parseImpactQuantityOrNull(),
            transactionDatePicker.getValue(),
            statusCombo.getValue(),
            notesArea.getText().trim()
        );

        // Aligned fields mapping
        transaction.setSourceUserId(1); // Placeholder: Default to admin/system user
        transaction.setTargetUserId(1); // Placeholder
        transaction.setPaymentStatus(mapUiStatusToPaymentStatus(statusCombo.getValue()));

        try {
            EcoTransaction inserted = transactionDao.insert(transaction);
            transactions.add(0, inserted);
            transactionTable.getSelectionModel().select(inserted);
            refreshSummary();
            formStatusLabel.setText("Transaction created successfully.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to create transaction: " + ex.getMessage());
        }
    }

    @FXML
    private void updateTransaction() {
        EcoTransaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a transaction from the table before updating.");
            return;
        }

        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        selected.setReferenceCode(referenceCodeField.getText().trim().toUpperCase());
        selected.setTransactionType(transactionTypeCombo.getValue());
        selected.setSourceType(sourceTypeCombo.getValue());
        selected.setPurpose(purposeCombo.getValue());
        selected.setAmount(parseAmount());
        selected.setImpactUnit(impactUnitCombo.getValue());
        selected.setImpactQuantity(parseImpactQuantityOrNull());
        selected.setTransactionDate(transactionDatePicker.getValue());
        selected.setStatus(statusCombo.getValue());
        selected.setNotes(notesArea.getText().trim());

        selected.setPaymentStatus(mapUiStatusToPaymentStatus(statusCombo.getValue()));

        try {
            transactionDao.update(selected);
            transactionTable.refresh();
            refreshSummary();
            formStatusLabel.setText("Transaction ID " + selected.getId() + " updated.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to update transaction: " + ex.getMessage());
        }
    }

    @FXML
    private void deleteTransaction() {
        EcoTransaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a transaction from the table before deleting.");
            return;
        }

        int deletedId = selected.getId();
        try {
            transactionDao.deleteById(deletedId);
            transactions.remove(selected);
            transactionTable.getSelectionModel().clearSelection();
            clearFormValuesOnly();
            refreshSummary();
            formStatusLabel.setText("Transaction ID " + deletedId + " deleted.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to delete transaction: " + ex.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        transactionTable.getSelectionModel().clearSelection();
        clearFormValuesOnly();
        formStatusLabel.setText("Form cleared.");
    }

    @FXML
    private void openDonationTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/donation.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openGpsTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/gps.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openDashboardTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/Dashboard.fxml", err -> System.err.println(err));
    }

    private void loadTransactions() throws SQLException {
        transactions.setAll(transactionDao.findAll());
    }

    private void clearFormValuesOnly() {
        referenceCodeField.clear();
        transactionTypeCombo.getSelectionModel().selectFirst();
        sourceTypeCombo.getSelectionModel().selectFirst();
        purposeCombo.getSelectionModel().selectFirst();
        amountField.clear();
        impactUnitCombo.setValue(IMPACT_NONE);
        applyTransactionTypeRules(transactionTypeCombo.getValue());
        impactQuantityField.clear();
        transactionDatePicker.setValue(LocalDate.now());
        statusCombo.getSelectionModel().selectFirst();
        notesArea.clear();
    }

    private void setDefaultFormValues() {
        transactionTypeCombo.getSelectionModel().selectFirst();
        sourceTypeCombo.getSelectionModel().selectFirst();
        purposeCombo.getSelectionModel().selectFirst();
        impactUnitCombo.setValue(IMPACT_NONE);
        applyTransactionTypeRules(transactionTypeCombo.getValue());
        transactionDatePicker.setValue(LocalDate.now());
        statusCombo.getSelectionModel().selectFirst();
    }

    private ValidationResult validateForm() {
        if (transactionTypeCombo.getValue() == null) {
            return new ValidationResult(false, "Transaction type is required.");
        }

        if (sourceTypeCombo.getValue() == null) {
            return new ValidationResult(false, "Source type is required.");
        }

        if (purposeCombo.getValue() == null) {
            return new ValidationResult(false, "Purpose is required.");
        }

        if (impactUnitCombo.getValue() == null) {
            return new ValidationResult(false, "Impact unit is required.");
        }

        if (statusCombo.getValue() == null) {
            return new ValidationResult(false, "Status is required.");
        }

        if (transactionDatePicker.getValue() == null) {
            return new ValidationResult(false, "Transaction date is required.");
        }

        if (referenceCodeField.getText() != null && !referenceCodeField.getText().trim().isEmpty()) {
            if (!referenceCodeField.getText().trim().toUpperCase().matches("[A-Z0-9-]+")) {
                return new ValidationResult(false, "Reference code allows only A-Z, 0-9 and -.");
            }
        }

        try {
            double amount = parseAmount();
            if (amount <= 0) {
                return new ValidationResult(false, "Amount must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            return new ValidationResult(false, "Amount must be a valid number, for example 200 or 200.500.");
        }

        if (!IMPACT_NONE.equals(impactUnitCombo.getValue())) {
            try {
                int qty = parseImpactQuantity();
                if (qty <= 0) {
                    return new ValidationResult(false, "Impact quantity must be greater than zero.");
                }
            } catch (NumberFormatException ex) {
                return new ValidationResult(false, "Impact quantity must be a valid integer, for example 50.");
            }
        }

        return new ValidationResult(true, "OK");
    }

    private String generateReferenceCode(String transactionType) {
        String prefix;
        if (TYPE_INCOME.equals(transactionType)) {
            prefix = "INC";
        } else if (TYPE_ALLOCATION.equals(transactionType)) {
            prefix = "ALL";
        } else if (TYPE_EXPENSE.equals(transactionType)) {
            prefix = "EXP";
        } else {
            prefix = "REF";
        }

        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int sequencePart = transactions.size() + 1;
        return prefix + "-" + datePart + "-" + sequencePart;
    }

    private double parseAmount() {
        return Double.parseDouble(amountField.getText().trim());
    }

    private int parseImpactQuantity() {
        return Integer.parseInt(impactQuantityField.getText().trim());
    }

    private Integer parseImpactQuantityOrNull() {
        if (IMPACT_NONE.equals(impactUnitCombo.getValue())) {
            return null;
        }
        return parseImpactQuantity();
    }

    private void setVisibleManaged(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void refreshSummary() {
        double inflow = transactions.stream()
            .filter(t -> TYPE_INCOME.equals(t.getTransactionType()))
            .mapToDouble(EcoTransaction::getAmount)
            .sum();

        double outflow = transactions.stream()
            .filter(t -> TYPE_ALLOCATION.equals(t.getTransactionType())
                || TYPE_EXPENSE.equals(t.getTransactionType())
                || TYPE_REFUND.equals(t.getTransactionType()))
            .mapToDouble(EcoTransaction::getAmount)
            .sum();

        double balance = inflow - outflow;

        int trees = transactions.stream()
            .filter(t -> IMPACT_TREES.equals(t.getImpactUnit()))
            .map(EcoTransaction::getImpactQuantity)
            .filter(v -> v != null)
            .mapToInt(Integer::intValue)
            .sum();

        int wasteKg = transactions.stream()
            .filter(t -> IMPACT_WASTE.equals(t.getImpactUnit()))
            .map(EcoTransaction::getImpactQuantity)
            .filter(v -> v != null)
            .mapToInt(Integer::intValue)
            .sum();

        summaryLabel.setText(String.format(
            "Inflow: %.3f TND | Outflow: %.3f TND | Balance: %.3f TND | Trees: %d | Waste: %d kg",
            inflow, outflow, balance, trees, wasteKg
        ));

        refreshCharts();
    }

    private void refreshCharts() {
        refreshSourcePieChart();
        refreshImpactBarChart();
    }

    private void refreshSourcePieChart() {
        Map<String, Double> bySource = new LinkedHashMap<>();
        for (String source : SOURCE_TYPES) {
            bySource.put(source, 0.0);
        }

        for (EcoTransaction transaction : transactions) {
            bySource.computeIfPresent(transaction.getSourceType(), (k, v) -> v + transaction.getAmount());
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        bySource.forEach((source, total) -> {
            if (total > 0) {
                pieData.add(new PieChart.Data(source, total));
            }
        });

        sourcePieChart.setData(pieData);
        sourcePieChart.setTitle("Funds by Source (TND)");
    }

    private void refreshImpactBarChart() {
        int trees = totalImpactForUnit(IMPACT_TREES);
        int waste = totalImpactForUnit(IMPACT_WASTE);
        int volunteers = totalImpactForUnit(IMPACT_VOLUNTEERS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Impact Quantity");
        series.getData().add(new XYChart.Data<>("Trees", trees));
        series.getData().add(new XYChart.Data<>("Kg Waste", waste));
        series.getData().add(new XYChart.Data<>("Volunteers", volunteers));

        impactBarChart.getData().setAll(series);
    }

    private int totalImpactForUnit(String unit) {
        return transactions.stream()
            .filter(t -> unit.equals(t.getImpactUnit()))
            .map(EcoTransaction::getImpactQuantity)
            .filter(v -> v != null)
            .mapToInt(Integer::intValue)
            .sum();
    }

    private String mapUiStatusToPaymentStatus(String uiStatus) {
        if (uiStatus == null) return "pending";
        return switch (uiStatus) {
            case "Completed" -> "completed";
            case "Cancelled" -> "failed";
            default -> "pending";
        };
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
