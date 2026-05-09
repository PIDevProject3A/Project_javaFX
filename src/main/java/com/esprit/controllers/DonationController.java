package com.esprit.controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esprit.utils.MyDataBase;
import com.esprit.utils.SceneNavigator;
import com.esprit.services.DonationDao;
import com.esprit.entities.Donation;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class DonationController {

    private static final String TYPE_CASH = "Cash";
    private static final String TYPE_TREE_SPONSORSHIP = "Tree Sponsorship";
    private static final String TYPE_GOODS = "Goods";

    private static final ObservableList<String> CASH_PAYMENT_METHODS = FXCollections.observableArrayList(List.of(
        "Card",
        "Bank Transfer",
        "Mobile Wallet",
        "Cash"
    ));

    private static final ObservableList<String> TREE_SPONSORSHIP_PAYMENT_METHODS = FXCollections.observableArrayList(List.of(
        "Card",
        "Bank Transfer",
        "Mobile Wallet",
        "Cash"
    ));

    private static final ObservableList<String> GOODS_DELIVERY_METHODS = FXCollections.observableArrayList(List.of(
        "Drop-off",
        "Partner Pickup",
        "Collection Point"
    ));

    @FXML
    private TableView<Donation> donationTable;
    @FXML
    private TableColumn<Donation, Integer> idColumn;
    @FXML
    private TableColumn<Donation, String> donorColumn;
    @FXML
    private TableColumn<Donation, String> typeColumn;
    @FXML
    private TableColumn<Donation, String> amountColumn;
    @FXML
    private TableColumn<Donation, String> paymentColumn;
    @FXML
    private TableColumn<Donation, LocalDate> dateColumn;
    @FXML
    private TableColumn<Donation, String> statusColumn;

    @FXML
    private TextField donorNameField;
    @FXML
    private ComboBox<String> donationTypeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private Label amountLabel;
    @FXML
    private TextField treeCountField;
    @FXML
    private Label treeCountLabel;
    @FXML
    private ComboBox<String> paymentMethodCombo;
    @FXML
    private Label paymentMethodLabel;
    @FXML
    private DatePicker donationDatePicker;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TextArea notesArea;

    @FXML
    private Label summaryLabel;
    @FXML
    private Label formStatusLabel;
    @FXML
    private Label leaderboardSubtitleLabel;
    @FXML
    private Label leaderboardFirstDonorLabel;
    @FXML
    private Label leaderboardFirstAmountLabel;
    @FXML
    private Label leaderboardSecondDonorLabel;
    @FXML
    private Label leaderboardSecondAmountLabel;
    @FXML
    private Label leaderboardThirdDonorLabel;
    @FXML
    private Label leaderboardThirdAmountLabel;

    private final ObservableList<Donation> donations = FXCollections.observableArrayList();
    private final DonationDao donationDao = new DonationDao();

    @FXML
    private void initialize() {
        configureTable();
        configureFormOptions();
        configureDonationTypeBehavior();
        bindSelectionToForm();

        setDefaultFormValues();

        try {
            MyDataBase.getInstance();
            loadDonations();
            formStatusLabel.setText("Connected to database. Donations loaded.");
        } catch (SQLException ex) {
            formStatusLabel.setText("Database error: " + ex.getMessage());
        }

        refreshSummary();
        refreshLeaderboard();
    }

    private void loadDonations() throws SQLException {
        donations.setAll(donationDao.findAll());
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        donorColumn.setCellValueFactory(new PropertyValueFactory<>("donorName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("donationType"));
        amountColumn.setCellValueFactory(cellData -> {
            Donation donation = cellData.getValue();
            if (TYPE_TREE_SPONSORSHIP.equals(donation.getDonationType())) {
                int trees = donation.getTreeCount() == null ? 0 : donation.getTreeCount();
                return new ReadOnlyStringWrapper(trees + " trees");
            }
            if (TYPE_GOODS.equals(donation.getDonationType())) {
                return new ReadOnlyStringWrapper("Goods donation");
            }
            return new ReadOnlyStringWrapper(String.format("%.3f TND", donation.getAmount()));
        });
        paymentColumn.setCellValueFactory(cellData -> {
            Donation donation = cellData.getValue();
            String prefix = TYPE_GOODS.equals(donation.getDonationType()) ? "Delivery: " : "Payment: ";
            return new ReadOnlyStringWrapper(prefix + donation.getPaymentMethod());
        });
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("donationDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        donationTable.setItems(donations);
    }

    private void configureFormOptions() {
        donationTypeCombo.setItems(FXCollections.observableArrayList(List.of(
            TYPE_CASH,
            TYPE_GOODS,
                "Corporate",
                "Event Support",
            TYPE_TREE_SPONSORSHIP
        )));

        statusCombo.setItems(FXCollections.observableArrayList(List.of(
                "Pending",
                "Confirmed",
                "Allocated",
                "Cancelled"
        )));
    }

    private void configureDonationTypeBehavior() {
        donationTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyDonationTypeRules(newValue));
    }

    private void applyDonationTypeRules(String donationType) {
        boolean isTreeSponsorship = TYPE_TREE_SPONSORSHIP.equals(donationType);
        boolean isGoods = TYPE_GOODS.equals(donationType);
        boolean isAmountType = isAmountBasedType(donationType);

        setVisibleManaged(isAmountType, amountLabel, amountField);
        setVisibleManaged(isTreeSponsorship, treeCountLabel, treeCountField);

        if (isGoods) {
            paymentMethodLabel.setText("Delivery method");
            paymentMethodCombo.setItems(FXCollections.observableArrayList(GOODS_DELIVERY_METHODS));
            amountField.clear();
            treeCountField.clear();
        } else if (isTreeSponsorship) {
            paymentMethodLabel.setText("Payment method");
            paymentMethodCombo.setItems(FXCollections.observableArrayList(TREE_SPONSORSHIP_PAYMENT_METHODS));
            amountField.clear();
        } else {
            paymentMethodLabel.setText("Payment method");
            paymentMethodCombo.setItems(FXCollections.observableArrayList(CASH_PAYMENT_METHODS));
            treeCountField.clear();
        }

        if (!paymentMethodCombo.getItems().contains(paymentMethodCombo.getValue())) {
            paymentMethodCombo.getSelectionModel().selectFirst();
        }
    }

    private void setVisibleManaged(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void bindSelectionToForm() {
        donationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                return;
            }

            donorNameField.setText(newSelection.getDonorName());
            donationTypeCombo.setValue(newSelection.getDonationType());
            if (isAmountBasedType(newSelection.getDonationType())) {
                amountField.setText(String.format("%.3f", newSelection.getAmount()));
            } else {
                amountField.clear();
            }
            if (newSelection.getTreeCount() != null) {
                treeCountField.setText(String.valueOf(newSelection.getTreeCount()));
            } else {
                treeCountField.clear();
            }
            paymentMethodCombo.setValue(newSelection.getPaymentMethod());
            donationDatePicker.setValue(newSelection.getDonationDate());
            statusCombo.setValue(newSelection.getStatus());
            notesArea.setText(newSelection.getNotes());
            formStatusLabel.setText("Selected donation ID " + newSelection.getId() + ". You can now update or delete it.");
        });
    }

    @FXML
    private void createDonation() {
        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        String donationType = donationTypeCombo.getValue();
        Integer treeCount = resolveTreeCountForType(donationType);
        double amount = resolveAmountForType(donationType, treeCount);

        Donation donation = new Donation(
                0,
                donorNameField.getText().trim(),
            donationType,
            amount,
                paymentMethodCombo.getValue(),
                donationDatePicker.getValue(),
                statusCombo.getValue(),
            notesArea.getText().trim(),
            treeCount
        );

        // Aligned fields mapping
        donation.setUserId(1); // Placeholder: Default to admin/system user
        donation.setTransactionStatus(mapUiStatusToTransactionStatus(statusCombo.getValue()));
        donation.setDonationType(mapUiTypeToDonationType(donationType));

        try {
            Donation inserted = donationDao.insert(donation);
            donations.add(0, inserted);
            donationTable.getSelectionModel().select(inserted);
            refreshSummary();
            refreshLeaderboard();
            formStatusLabel.setText("Donation created successfully.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to create donation: " + ex.getMessage());
        }
    }

    @FXML
    private void updateDonation() {
        Donation selected = donationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a donation from the table before updating.");
            return;
        }

        ValidationResult validation = validateForm();
        if (!validation.valid()) {
            formStatusLabel.setText(validation.message());
            return;
        }

        String donationType = donationTypeCombo.getValue();
        Integer treeCount = resolveTreeCountForType(donationType);
        double amount = resolveAmountForType(donationType, treeCount);

        selected.setDonorName(donorNameField.getText().trim());
        selected.setDonationType(donationType);
        selected.setAmount(amount);
        selected.setPaymentMethod(paymentMethodCombo.getValue());
        selected.setDonationDate(donationDatePicker.getValue());
        selected.setStatus(statusCombo.getValue());
        selected.setNotes(notesArea.getText().trim());
        selected.setTreeCount(treeCount);

        selected.setTransactionStatus(mapUiStatusToTransactionStatus(statusCombo.getValue()));
        selected.setDonationType(mapUiTypeToDonationType(donationType));

        try {
            donationDao.update(selected);
            donationTable.refresh();
            refreshSummary();
            refreshLeaderboard();
            formStatusLabel.setText("Donation ID " + selected.getId() + " updated.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to update donation: " + ex.getMessage());
        }
    }

    @FXML
    private void deleteDonation() {
        Donation selected = donationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formStatusLabel.setText("Please select a donation from the table before deleting.");
            return;
        }

        int deletedId = selected.getId();
        try {
            donationDao.deleteById(deletedId);
            donations.remove(selected);
            donationTable.getSelectionModel().clearSelection();
            clearFormValuesOnly();
            refreshSummary();
            refreshLeaderboard();
            formStatusLabel.setText("Donation ID " + deletedId + " deleted.");
        } catch (SQLException | IllegalArgumentException ex) {
            formStatusLabel.setText("Failed to delete donation: " + ex.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        donationTable.getSelectionModel().clearSelection();
        clearFormValuesOnly();
        formStatusLabel.setText("Form cleared.");
    }

    @FXML
    private void openTransactionTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/transaction.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openDashboardTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/Dashboard.fxml", err -> System.err.println(err));
    }

    @FXML
    private void openGpsTab() throws IOException {
        SceneNavigator.navigate(summaryLabel, "/gps.fxml", err -> System.err.println(err));
    }

    private void clearFormValuesOnly() {
        donorNameField.clear();
        donationTypeCombo.getSelectionModel().selectFirst();
        amountField.clear();
        treeCountField.clear();
        applyDonationTypeRules(donationTypeCombo.getValue());
        donationDatePicker.setValue(LocalDate.now());
        statusCombo.getSelectionModel().selectFirst();
        notesArea.clear();
    }

    private void setDefaultFormValues() {
        donationDatePicker.setValue(LocalDate.now());
        donationTypeCombo.getSelectionModel().selectFirst();
        applyDonationTypeRules(donationTypeCombo.getValue());
        statusCombo.getSelectionModel().selectFirst();
    }

    private ValidationResult validateForm() {
        if (donorNameField.getText() == null || donorNameField.getText().trim().isEmpty()) {
            return new ValidationResult(false, "Donor name is required.");
        }

        if (donationTypeCombo.getValue() == null) {
            return new ValidationResult(false, "Donation type is required.");
        }

        if (paymentMethodCombo.getValue() == null) {
            return new ValidationResult(false, "Payment method is required.");
        }

        if (statusCombo.getValue() == null) {
            return new ValidationResult(false, "Status is required.");
        }

        if (donationDatePicker.getValue() == null) {
            return new ValidationResult(false, "Donation date is required.");
        }

        String donationType = donationTypeCombo.getValue();

        if (isAmountBasedType(donationType)) {
            try {
                double amount = parseAmount();
                if (amount <= 0) {
                    return new ValidationResult(false, "Amount must be greater than zero.");
                }
            } catch (NumberFormatException ex) {
                return new ValidationResult(false, "Amount must be a valid number, for example 50 or 50.500.");
            }
        }

        if (TYPE_TREE_SPONSORSHIP.equals(donationType)) {
            try {
                int trees = parseTreeCount();
                if (trees <= 0) {
                    return new ValidationResult(false, "Trees count must be greater than zero.");
                }
            } catch (NumberFormatException ex) {
                return new ValidationResult(false, "Trees count must be a valid integer, for example 10.");
            }
        }

        return new ValidationResult(true, "OK");
    }

    private boolean isAmountBasedType(String donationType) {
        return TYPE_CASH.equals(donationType) || "Corporate".equals(donationType) || "Event Support".equals(donationType);
    }

    private Integer resolveTreeCountForType(String donationType) {
        if (TYPE_TREE_SPONSORSHIP.equals(donationType)) {
            return parseTreeCount();
        }
        return null;
    }

    private double resolveAmountForType(String donationType, Integer treeCount) {
        if (TYPE_TREE_SPONSORSHIP.equals(donationType)) {
            return treeCount == null ? 0.0 : treeCount.doubleValue();
        }
        if (TYPE_GOODS.equals(donationType)) {
            return 0.0;
        }
        return parseAmount();
    }

    private double parseAmount() {
        return Double.parseDouble(amountField.getText().trim());
    }

    private int parseTreeCount() {
        return Integer.parseInt(treeCountField.getText().trim());
    }

    private void refreshSummary() {
        double total = donations.stream().mapToDouble(Donation::getAmount).sum();
        summaryLabel.setText(String.format("Donations: %d | Total: %.3f TND", donations.size(), total));
    }

    private void refreshLeaderboard() {
        List<DonorLeaderboardEntry> topEntries = buildLeaderboardEntries();
        setLeaderboardRow(topEntries, 0, leaderboardFirstDonorLabel, leaderboardFirstAmountLabel);
        setLeaderboardRow(topEntries, 1, leaderboardSecondDonorLabel, leaderboardSecondAmountLabel);
        setLeaderboardRow(topEntries, 2, leaderboardThirdDonorLabel, leaderboardThirdAmountLabel);

        if (topEntries.isEmpty()) {
            leaderboardSubtitleLabel.setText("No donations yet. Create records to see top supporters.");
            return;
        }

        DonorLeaderboardEntry leader = topEntries.get(0);
        leaderboardSubtitleLabel.setText(String.format(
            "Current leader: %s with %.3f TND across %d donations.",
            leader.donorName(),
            leader.totalAmount(),
            leader.donationCount()
        ));
    }

    private List<DonorLeaderboardEntry> buildLeaderboardEntries() {
        Map<String, DonorAggregate> aggregateByDonor = new HashMap<>();

        for (Donation donation : donations) {
            if ("Cancelled".equalsIgnoreCase(donation.getStatus())) {
                continue;
            }

            String donor = donation.getDonorName();
            if (donor == null || donor.trim().isEmpty()) {
                continue;
            }

            DonorAggregate aggregate = aggregateByDonor.computeIfAbsent(donor.trim(), key -> new DonorAggregate());
            aggregate.totalAmount += donation.getAmount();
            aggregate.donationCount += 1;
        }

        List<DonorLeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<String, DonorAggregate> entry : aggregateByDonor.entrySet()) {
            entries.add(new DonorLeaderboardEntry(entry.getKey(), entry.getValue().totalAmount, entry.getValue().donationCount));
        }

        entries.sort(
            Comparator.comparingDouble(DonorLeaderboardEntry::totalAmount).reversed()
                .thenComparingInt(DonorLeaderboardEntry::donationCount).reversed()
                .thenComparing(DonorLeaderboardEntry::donorName)
        );

        return entries;
    }

    private void setLeaderboardRow(List<DonorLeaderboardEntry> entries, int rankIndex, Label donorLabel, Label amountLabel) {
        if (rankIndex >= entries.size()) {
            donorLabel.setText("No donor yet");
            amountLabel.setText("0.000 TND");
            return;
        }

        DonorLeaderboardEntry entry = entries.get(rankIndex);
        donorLabel.setText(entry.donorName());
        amountLabel.setText(String.format("%.3f TND in %d donation(s)", entry.totalAmount(), entry.donationCount()));
    }

    private static final class DonorAggregate {
        private double totalAmount;
        private int donationCount;
    }

    private record DonorLeaderboardEntry(String donorName, double totalAmount, int donationCount) {
    }

    private String mapUiStatusToTransactionStatus(String uiStatus) {
        if (uiStatus == null) return "Pending";
        return switch (uiStatus) {
            case "Confirmed", "Allocated" -> "Completed";
            default -> "Pending";
        };
    }

    private String mapUiTypeToDonationType(String uiType) {
        if (uiType == null) return "Money";
        return switch (uiType) {
            case TYPE_GOODS, TYPE_TREE_SPONSORSHIP -> "Goods";
            default -> "Money";
        };
    }

    private record ValidationResult(boolean valid, String message) {
    }
}
