package com.bledna;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/bledna/main.fxml")
        );
        Scene scene = new Scene(loader.load(), 1500, 720);
        
        var cssResource = getClass().getResource("/com/bledna/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("Warning: CSS resource not found at /com/bledna/styles.css");
        }

        stage.setTitle("Bledna — Waste Management System");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
