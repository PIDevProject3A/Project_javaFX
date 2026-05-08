package com.bledna.util;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;


public class AlertUtil {

    private AlertUtil() {}

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * @return true if the user confirmed (clicked OK)
     */
    public static boolean showConfirm(String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        
        DialogPane dialogPane = dialog.getDialogPane();
        String stylePath = AlertUtil.class.getResource("/com/bledna/styles.css").toExternalForm();
        dialogPane.getStylesheets().add(stylePath);
        dialogPane.getStyleClass().add("custom-alert");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setPrefWidth(450);

        Label iconLabel = new Label("⚠");
        iconLabel.setStyle("-fx-font-size: 50px; -fx-text-fill: #ef4444;");
        
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-text-alignment: center; -fx-font-weight: bold;");

        content.getChildren().addAll(iconLabel, msgLabel);
        dialogPane.setContent(content);

        ButtonType btnDelete = new ButtonType("Yes, Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("No, Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogPane.getButtonTypes().addAll(btnDelete, btnCancel);

        Button okButton = (Button) dialogPane.lookupButton(btnDelete);
        okButton.getStyleClass().add("btn-delete");
        
        Button cancelButton = (Button) dialogPane.lookupButton(btnCancel);
        cancelButton.getStyleClass().add("btn-secondary");

        return dialog.showAndWait().filter(r -> r == btnDelete).isPresent();
    }
}
