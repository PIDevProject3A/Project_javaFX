package com.esprit.utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Gestionnaire centralisé pour la gestion dynamique de la langue
 */
public class LanguageManager {
    private static final LanguageManager INSTANCE = new LanguageManager();
    
    private final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(Locale.ENGLISH);
    private ResourceBundle bundle;
    
    private LanguageManager() {
        updateBundle();
    }
    
    public static LanguageManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Change la langue et met à jour les ressources
     */
    public void setLocale(String languageCode) {
        Locale newLocale;
        if ("fr".equalsIgnoreCase(languageCode)) {
            newLocale = Locale.FRENCH;
            UserSession.setCurrentLocale("fr");
        } else {
            newLocale = Locale.ENGLISH;
            UserSession.setCurrentLocale("en");
        }
        locale.set(newLocale);
        updateBundle();
    }
    
    /**
     * Met à jour le ResourceBundle avec la langue actuelle
     */
    private void updateBundle() {
        try {
            bundle = ResourceBundle.getBundle("messages", locale.get());
        } catch (Exception e) {
            e.printStackTrace();
            bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }
    
    /**
     * Obtient la ressource bundle actuelle
     */
    public ResourceBundle getBundle() {
        return bundle;
    }
    
    /**
     * Obtient une string traduite
     */
    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }
    
    /**
     * Obtient la locale actuelle
     */
    public Locale getLocale() {
        return locale.get();
    }
    
    /**
     * Recharge une scène avec la nouvelle langue
     */
    public void reloadScene(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                LanguageManager.class.getResource(fxmlPath),
                getBundle()
            );
            Parent root = loader.load();
            
            Scene currentScene = stage.getScene();
            if (currentScene != null && currentScene.getRoot() != null) {
                // Préserver les stylesheets
                root.getStylesheets().addAll(currentScene.getRoot().getStylesheets());
            }
            
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Observable property pour les listeners
     */
    public ObjectProperty<Locale> localeProperty() {
        return locale;
    }
}
