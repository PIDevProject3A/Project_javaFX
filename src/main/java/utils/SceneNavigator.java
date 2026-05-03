package utils;

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
            Parent newRoot = FXMLLoader.load(SceneNavigator.class.getResource(fxmlPath));
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = stage.getScene();

            if (scene == null) {
                stage.setScene(new Scene(newRoot));
                System.out.println("[Navigation] Redirected to: " + fxmlPath);
                return;
            }

            Parent currentRoot = scene.getRoot();
            FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, currentRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(event -> {
                newRoot.setOpacity(0.0);
                newRoot.setTranslateX(16);
                scene.setRoot(newRoot);

                FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, newRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                TranslateTransition slideIn = new TranslateTransition(SLIDE_DURATION, newRoot);
                slideIn.setFromX(16);
                slideIn.setToX(0);

                new ParallelTransition(fadeIn, slideIn).play();
                System.out.println("[Navigation] Redirected to: " + fxmlPath);
            });

            fadeOut.play();
        } catch (IOException e) {
            System.err.println("[Navigation Error] Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
            if (errorHandler != null) {
                errorHandler.accept("Unable to open page.");
            }
        }
    }
}

