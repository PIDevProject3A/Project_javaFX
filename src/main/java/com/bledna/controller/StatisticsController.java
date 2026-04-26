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
            // rcuperer tous les wastes depuis la base de données
            List<WasteCollection> collections = dao.getAll();

            //  Pie Chart :
            Map<String, Double> typeMap = new HashMap<>();

            // pour cumuler la quantité par type
            for (WasteCollection w : collections) {
                // Si le type existe dreja additionner, sinon creer nv
                typeMap.put(w.getWasteType(), typeMap.getOrDefault(w.getWasteType(), 0.0) + w.getQuantity());
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

            // Boucle sur typeMap pour créer une part de camembert par type

            typeMap.forEach((type, qty) -> pieData.add(new PieChart.Data(type + " (" + qty + " kg)", qty)));

            // Envoyer les données au PieChart pour affichage
            pieChart.setData(pieData);

            //Bar Chart : quantité totale par date de collecte
            XYChart.Series<String, Double> series = new XYChart.Series<>();
            series.setName("Waste Quantity");

            Map<String, Double> dateMap = new HashMap<>();
            // Format de date utilisé comme clé : "yyyy-MM-dd" ex: "2026-04-09"
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Boucle sur tous les wastes pour cumuler la quantité par date

            for (WasteCollection w : collections) {
                if (w.getCollectionDate() != null) {
                    // Convertir LocalDateTime en String "yyyy-MM-dd"
                    String dateStr = w.getCollectionDate().format(fmt);
                    // Si la date existe déjà → additionner, sinon → créer avec 0.0
                    dateMap.put(dateStr, dateMap.getOrDefault(dateStr, 0.0) + w.getQuantity());
                }
            }

            // Boucle sur dateMap triée par date croissante pour créer les barres du graphique
            // .stream()  → traiter les entrées comme un flux
            // .sorted()  → trier par date (ordre alphabétique )
            // .forEach() → pour chaque entrée, ajouter une barre dans le BarChart
            dateMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

            // Envoyer la série au BarChart pour affichage
            barChart.getData().setAll(series);

        } catch (SQLException e) {
            AlertUtil.showError("Erreur Stats", "Impossible de charger les données : " + e.getMessage());
        }
    }

    // ── Bouton Retour : revenir à la vue Waste ────────────────────────────
    @FXML
    private void handleBack() {
        try {
            // Chercher le StackPane central (#contentPane) défini dans main.fxml
            StackPane contentPane = (StackPane) pieChart.getScene().lookup("#contentPane");
            if (contentPane != null) {
                // Charger waste_view.fxml et le placer dans le StackPane central
                Node view = FXMLLoader.load(getClass().getResource("/com/bledna/waste_view.fxml"));
                contentPane.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}