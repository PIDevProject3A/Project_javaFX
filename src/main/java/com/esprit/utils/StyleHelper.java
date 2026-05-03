package com.esprit.utils;

import javafx.scene.Scene;

/**
 * Applique la feuille de style commune « bledna » aux scènes JavaFX.
 */
public final class StyleHelper {

    private static final String STYLESHEET = "/com/esprit/bledna-theme.css";

    private StyleHelper() {
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }
        var url = StyleHelper.class.getResource(STYLESHEET);
        if (url == null) {
            return;
        }
        String external = url.toExternalForm();
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }
}

