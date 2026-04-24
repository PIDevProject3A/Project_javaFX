package com.esprit.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {
    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void navigateTo(String fxmlFile) throws IOException {
        var url = NavigationManager.class.getResource("/com/esprit/" + fxmlFile);
        if (url == null) {
            throw new IOException("FXML introuvable: /com/esprit/" + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1100, 680);
        StyleHelper.apply(scene);
        stage.setScene(scene);
        stage.setTitle(switch (fxmlFile) {
            case "Home.fxml" -> "bledna";
            case "EventAdmin.fxml" -> "bledna — Administration";
            case "EventCatalog.fxml" -> "bledna — Catalogue";
            case "RegistrationList.fxml" -> "bledna — Mes inscriptions";
            default -> "bledna";
        });
    }

    public static Stage getStage() {
        return stage;
    }
}


