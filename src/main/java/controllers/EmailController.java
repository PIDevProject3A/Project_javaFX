package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import services.EmailService;
import utils.SceneNavigator;

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
        if (utils.UserSession.getUserToEdit() != null) {
            toField.setText(utils.UserSession.getUserToEdit().getEmail());
            utils.UserSession.setUserToEdit(null);
        } else if (utils.UserSession.getAppUserToEdit() != null) {
            toField.setText(utils.UserSession.getAppUserToEdit().getEmail());
            utils.UserSession.setAppUserToEdit(null);
        }
    }

    @FXML
    public void handleSendEmail(ActionEvent event) {
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (to.isEmpty() || subject.isEmpty() || message.isEmpty()) {
            showStatusMessage("Veuillez remplir tous les champs.", false);
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
                showStatusMessage("Email envoye avec succes !", true);
                clearFields();
            } else {
                showStatusMessage("Erreur lors de l'envoi de l'email.", false);
            }
        });

        sendTask.setOnFailed(e -> {
            resetUI(false);
            showStatusMessage("Erreur critique lors de l'envoi.", false);
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
