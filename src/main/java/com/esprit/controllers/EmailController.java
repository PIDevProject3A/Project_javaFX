package com.esprit.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import com.esprit.services.EmailService;
import com.esprit.utils.SceneNavigator;

public class EmailController {

    @FXML
    private TextField toField;

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea messageArea;

    @FXML
    private Button sendButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    private EmailService emailService;

    @FXML
    public void initialize() {
        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        emailService = new EmailService();
        // Check for admin User context first, then AppUser context
        if (com.esprit.utils.UserSession.getUserToEdit() != null) {
            toField.setText(com.esprit.utils.UserSession.getUserToEdit().getEmail());
            com.esprit.utils.UserSession.setUserToEdit(null); // Clear after use so it doesn't persist
        } else if (com.esprit.utils.UserSession.getAppUserToEdit() != null) {
            toField.setText(com.esprit.utils.UserSession.getAppUserToEdit().getEmail());
            com.esprit.utils.UserSession.setAppUserToEdit(null);
        }
    }

    @FXML
    public void handleSendEmail(ActionEvent event) {
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (to.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatusMessage("Please fill in all fields.", false);
            return;
        }

        // Desactiver le bouton et afficher l'indicateur de chargement
        sendButton.setDisable(true);
        loadingIndicator.setVisible(true);
        statusLabel.setVisible(false);

        Task<Boolean> sendTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return emailService.sendEmail(to, subject, message);
            }
        };

        sendTask.setOnSucceeded(e -> {
            boolean success = sendTask.getValue();
            resetUI(success);
            if (success) {
                showStatusMessage("Email sent successfully!", true);
                clearFields();
            } else {
                showStatusMessage("Error sending email.", false);
            }
        });

        sendTask.setOnFailed(e -> {
            resetUI(false);
            showStatusMessage("Critical error during sending.", false);
            sendTask.getException().printStackTrace();
        });

        // Demarrer la tache dans un nouveau thread
        new Thread(sendTask).start();
    }

    private void resetUI(boolean success) {
        Platform.runLater(() -> {
            sendButton.setDisable(false);
            loadingIndicator.setVisible(false);
        });
    }

    private void showStatusMessage(String message, boolean isSuccess) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            if (isSuccess) {
                statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
    }

    private void clearFields() {
        Platform.runLater(() -> {
            toField.clear();
            subjectField.clear();
            messageArea.clear();
        });
    }

    @FXML
    private void goBack() {
        SceneNavigator.navigate(sendButton, "/AdminDashboard.fxml", msg -> showStatusMessage(msg, false));
    }
}

