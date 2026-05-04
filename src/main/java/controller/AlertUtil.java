package controller;

import javafx.scene.control.Alert;

final class AlertUtil {

    private AlertUtil() {
    }

    static void validation(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Check your input");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
