package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {
    private static final double APP_WIDTH = 900;
    private static final double APP_HEIGHT = 600;

    @Override
    public void start(Stage stage) throws Exception {
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages", java.util.Locale.ENGLISH);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"), bundle);
        Parent root = loader.load();
        Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
        stage.setTitle("BLADNA");
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

