package com.esprit.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Utility pour créer une barre de contrôle de langue et thème
 */
public class AppearanceControlBar {
    
    /**
     * Crée une barre de contrôle contenant les boutons de langue et thème
     */
    public static HBox createAppearanceBar(Stage stage, Scene scene) {
        HBox container = new HBox();
        container.setSpacing(12);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: -color-surface; -fx-background-radius: 16; -fx-border-color: -color-border; -fx-border-width: 1; -fx-border-radius: 16;");
        
        // Language button group
        HBox langGroup = createLanguageButtonGroup(stage, scene);
        
        // Theme toggle button
        Button themeButton = createThemeButton(stage, scene);
        
        container.getChildren().addAll(langGroup, themeButton);
        HBox.setHgrow(langGroup, Priority.ALWAYS);
        
        return container;
    }
    
    /**
     * Crée le groupe de boutons de langue
     */
    private static HBox createLanguageButtonGroup(Stage stage, Scene scene) {
        HBox group = new HBox();
        group.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10; -fx-padding: 6; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 10;");
        group.setSpacing(4);
        
        Button enButton = new Button("English");
        enButton.setStyle("-fx-padding: 8 16; -fx-font-weight: bold;");
        enButton.getStyleClass().add("lang-button");
        
        Button frButton = new Button("Français");
        frButton.setStyle("-fx-padding: 8 16; -fx-font-weight: bold;");
        frButton.getStyleClass().add("lang-button");
        
        // Set active button based on current locale
        updateLanguageButtons(enButton, frButton);
        
        // English button action
        enButton.setOnAction(e -> {
            LanguageManager.getInstance().setLocale("en");
            ThemeManager.getInstance().applyThemeToNode((javafx.scene.Parent) scene.getRoot());
            updateLanguageButtons(enButton, frButton);
        });
        
        // French button action
        frButton.setOnAction(e -> {
            LanguageManager.getInstance().setLocale("fr");
            ThemeManager.getInstance().applyThemeToNode((javafx.scene.Parent) scene.getRoot());
            updateLanguageButtons(enButton, frButton);
        });
        
        // Listen for locale changes
        LanguageManager.getInstance().localeProperty().addListener((obs, oldVal, newVal) -> {
            updateLanguageButtons(enButton, frButton);
        });
        
        group.getChildren().addAll(enButton, frButton);
        return group;
    }
    
    /**
     * Met à jour l'état actif des boutons de langue
     */
    private static void updateLanguageButtons(Button enButton, Button frButton) {
        String currentLocale = UserSession.getCurrentLocale();
        if ("fr".equalsIgnoreCase(currentLocale)) {
            frButton.getStyleClass().clear();
            frButton.getStyleClass().addAll("lang-button", "lang-button-active");
            enButton.getStyleClass().clear();
            enButton.getStyleClass().add("lang-button");
        } else {
            enButton.getStyleClass().clear();
            enButton.getStyleClass().addAll("lang-button", "lang-button-active");
            frButton.getStyleClass().clear();
            frButton.getStyleClass().add("lang-button");
        }
    }
    
    /**
     * Crée le bouton de basculement du thème
     */
    private static Button createThemeButton(Stage stage, Scene scene) {
        Button themeButton = new Button();
        themeButton.getStyleClass().add("theme-button");
        themeButton.setMinWidth(44);
        themeButton.setMinHeight(44);
        
        // Update button text based on theme
        updateThemeButtonText(themeButton);
        
        themeButton.setOnAction(e -> {
            ThemeManager.getInstance().toggleDarkMode();
            updateThemeButtonText(themeButton);
            ThemeManager.getInstance().applyThemeToNode((javafx.scene.Parent) scene.getRoot());
        });
        
        // Listen for theme changes
        ThemeManager.getInstance().darkModeProperty().addListener((obs, oldVal, newVal) -> {
            updateThemeButtonText(themeButton);
        });
        
        return themeButton;
    }
    
    /**
     * Met à jour le texte du bouton thème
     */
    private static void updateThemeButtonText(Button button) {
        if (ThemeManager.getInstance().isDarkMode()) {
            button.setText("☀️");
        } else {
            button.setText("🌙");
        }
    }
}
