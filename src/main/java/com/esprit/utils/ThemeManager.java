package com.esprit.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Gestionnaire centralisé pour le thème clair/sombre
 */
public class ThemeManager {
    private static final ThemeManager INSTANCE = new ThemeManager();
    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);
    
    private ThemeManager() {
    }
    
    public static ThemeManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Active/désactive le mode sombre
     */
    public void setDarkMode(boolean dark) {
        darkMode.set(dark);
        UserSession.setDarkMode(dark);
    }
    
    /**
     * Bascule le mode sombre
     */
    public void toggleDarkMode() {
        setDarkMode(!isDarkMode());
    }
    
    /**
     * Vérifie si le mode sombre est actif
     */
    public boolean isDarkMode() {
        return darkMode.get();
    }
    
    /**
     * Applique le thème à une scène
     */
    public void applyTheme(Scene scene) {
        Parent root = scene.getRoot();
        if (root != null) {
            applyThemeToNode(root);
        }
    }
    
    /**
     * Applique le thème à un nœud
     */
    public void applyThemeToNode(Parent node) {
        if (darkMode.get()) {
            node.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("dark"),
                true
            );
            if (!node.getStyleClass().contains("dark-mode")) {
                node.getStyleClass().add("dark-mode");
            }
        } else {
            node.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("dark"),
                false
            );
            node.getStyleClass().remove("dark-mode");
        }
    }
    
    /**
     * Observable property pour les listeners
     */
    public BooleanProperty darkModeProperty() {
        return darkMode;
    }
}
