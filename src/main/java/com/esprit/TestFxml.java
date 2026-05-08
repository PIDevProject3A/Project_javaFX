package com.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestFxml extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            com.esprit.utils.UserSession.setCurrentUserRole(com.esprit.entities.User.AdminType.ADMIN_ACCOUNT);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminDashboard.fxml"));
            Parent root = loader.load();
            System.out.println("SUCCESS!");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("FAILED TO LOAD FXML:");
            e.printStackTrace();
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                System.err.println("CAUSED BY: " + cause);
                cause.printStackTrace();
            }
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
