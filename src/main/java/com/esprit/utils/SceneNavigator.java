package com.esprit.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

public final class SceneNavigator {
    private static final Duration FADE_OUT_DURATION = Duration.millis(120);
    private static final Duration FADE_IN_DURATION = Duration.millis(220);
    private static final Duration SLIDE_DURATION = Duration.millis(220);

    private SceneNavigator() {
        // Utility class
    }

    public static void navigate(Node source, String fxmlPath, Consumer<String> errorHandler) {
        try {
            // Load resource bundle based on current user locale
            java.util.Locale locale = new java.util.Locale(UserSession.getCurrentLocale());
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages", locale);

            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath), bundle);
            Parent newRoot = loader.load();
            
            // Critical views that MUST replace the entire root (Login, Welcome, Main Shells)
            boolean forceNormal = fxmlPath.equalsIgnoreCase("/Login.fxml") 
                               || fxmlPath.equalsIgnoreCase("/Welcome.fxml") 
                               || fxmlPath.equalsIgnoreCase("/Dashboard.fxml")
                               || fxmlPath.equalsIgnoreCase("/UserDashboard.fxml");
            
            navigateWithRoot(source, newRoot, forceNormal, errorHandler);
        } catch (Exception e) {
            handleError(e, errorHandler);
        }
    }

    public static void navigateWithRoot(Node source, Parent newRoot, Consumer<String> errorHandler) {
        navigateWithRoot(source, newRoot, false, errorHandler);
    }

    public static void navigateWithRoot(Node source, Parent newRoot, boolean forceNormal, Consumer<String> errorHandler) {
        try {
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(newRoot));
                return;
            }

            // SHELL-AWARE NAVIGATION: Inject into center if possible, unless forced to replace root
            Node contentStack = scene.lookup("#mainContentStack");
            if (contentStack instanceof javafx.scene.layout.StackPane stack && !forceNormal) {
                Node finalNode = newRoot;
                // If we're loading another BorderPane (shell) into the stack, only take its center
                if (newRoot instanceof javafx.scene.layout.BorderPane bp && bp.getCenter() != null) {
                    finalNode = bp.getCenter();
                }

                stack.getChildren().clear();
                stack.getChildren().add(finalNode);
                
                if (UserSession.isDarkMode()) {
                    finalNode.getStyleClass().add("dark-mode");
                }

                finalNode.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(300), finalNode);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
                return;
            }

            // NORMAL NAVIGATION: Replace the entire root (effectively removes sidebars/shells)
            Parent currentRoot = scene.getRoot();
            FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                newRoot.setOpacity(0.0);
                newRoot.setTranslateX(16);
                
                if (UserSession.isDarkMode()) {
                    newRoot.getStyleClass().add("dark-mode");
                }
                
                scene.setRoot(newRoot);

                FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, newRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                TranslateTransition slideIn = new TranslateTransition(SLIDE_DURATION, newRoot);
                slideIn.setFromX(16);
                slideIn.setToX(0);

                new ParallelTransition(fadeIn, slideIn).play();
            });

            fadeOut.play();
        } catch (Exception e) {
            handleError(e, errorHandler);
        }
    }

    private static void handleError(Exception e, Consumer<String> errorHandler) {
        e.printStackTrace();
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (errorHandler != null) {
            errorHandler.accept("Unable to open page:\n" + e.getMessage() + "\nCause: " + cause.toString());
        }
    }
}


