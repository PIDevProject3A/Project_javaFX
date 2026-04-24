package com.bledna.controller;

import com.bledna.dao.WasteCollectionDAO;
import com.bledna.model.WasteCollection;
import com.bledna.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsController {

    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Double> barChart;

    private final WasteCollectionDAO dao = new WasteCollectionDAO();

    @FXML
    public void initialize() {
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            List<WasteCollection> collections = dao.getAll();
            
            // ── Pie Chart Data (Type vs Quantity)
            Map<String, Double> typeMap = new HashMap<>();
            for (WasteCollection w : collections) {
                typeMap.put(w.getWasteType(), typeMap.getOrDefault(w.getWasteType(), 0.0) + w.getQuantity());
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            typeMap.forEach((type, qty) -> pieData.add(new PieChart.Data(type + " (" + qty + " kg)", qty)));
            pieChart.setData(pieData);

            // ── Bar Chart Data (Date vs Quantity)
            XYChart.Series<String, Double> series = new XYChart.Series<>();
            series.setName("Waste Quantity");

            Map<String, Double> dateMap = new HashMap<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (WasteCollection w : collections) {
                if (w.getCollectionDate() != null) {
                    String dateStr = w.getCollectionDate().format(fmt);
                    dateMap.put(dateStr, dateMap.getOrDefault(dateStr, 0.0) + w.getQuantity());
                }
            }

            // Sort by date or just add
            dateMap.entrySet().stream()
                   .sorted(Map.Entry.comparingByKey())
                   .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

            barChart.getData().setAll(series);

        } catch (SQLException e) {
            AlertUtil.showError("Erreur Stats", "Impossible de charger les données : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            StackPane contentPane = (StackPane) pieChart.getScene().lookup("#contentPane");
            if (contentPane != null) {
                Node view = FXMLLoader.load(getClass().getResource("/com/bledna/waste_view.fxml"));
                contentPane.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
