package com.esprit;

import com.esprit.utils.NavigationManager;
import com.esprit.utils.StyleHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        NavigationManager.setStage(primaryStage);
        FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("/com/esprit/Home.fxml"));
        primaryStage.setTitle("bledna — événements");
        Scene scene = new Scene(loader.load(), 1120, 720);
        StyleHelper.apply(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
