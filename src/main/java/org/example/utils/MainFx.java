package org.example.utils;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherTopic.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        stage.setTitle("Community topics");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        List<String> fxArgs = new ArrayList<>();
        for (String a : args) {
            fxArgs.add(a);
        }
        launch(MainFx.class, fxArgs.toArray(new String[0]));
    }
}
