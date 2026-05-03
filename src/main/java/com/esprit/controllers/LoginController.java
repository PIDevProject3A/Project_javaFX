package com.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.esprit.entities.AppUser;
import com.esprit.entities.User;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import com.esprit.services.CompreFaceFaceIdService;
import com.esprit.services.RecaptchaService;
import com.esprit.services.UserService;
import com.esprit.utils.LocalRecaptchaPageServer;
import com.esprit.utils.RecaptchaConfig;
import com.esprit.utils.SceneNavigator;
import com.esprit.utils.UserSession;
import com.esprit.services.EmailService;
import com.esprit.utils.MyDataBase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private WebView recaptchaView;

    private final UserService userService = new UserService();
    private final RecaptchaService recaptchaService = new RecaptchaService();
    private final LocalRecaptchaPageServer recaptchaPageServer = new LocalRecaptchaPageServer();
    private final CompreFaceFaceIdService faceIdService = new CompreFaceFaceIdService();
    private final EmailService emailService = new EmailService();

    @FXML
    private void initialize() {
        messageLabel.visibleProperty().bind(messageLabel.textProperty().isNotEmpty());
        messageLabel.managedProperty().bind(messageLabel.visibleProperty());
        loadRecaptchaWidget();
    }

    @FXML
    private void handleLogin() {
        if (!RecaptchaConfig.isConfigured()) {
            showError("reCAPTCHA n'est pas configure.");
            return;
        }

        String recaptchaToken = readTokenFromWidget();
        if (recaptchaToken.isBlank()) {
            showError("Veuillez valider le reCAPTCHA avant de vous connecter.");
            return;
        }

        if (!recaptchaService.verifyToken(recaptchaToken)) {
            showError("Verification reCAPTCHA echouee. Reessayez.");
            resetCaptcha();
            return;
        }

        UserService.LoginResult result = userService.loginUnified(emailField.getText(), passwordField.getText());
        if (result == null) {
            showError("Invalid email or password.");
            return;
        }

        if (result.isAdmin()) {
            User user = result.getAdminUser();
            UserSession.setCurrentUserEmail(user.getEmail());
            UserSession.setCurrentUserRole(user.getAdminType());
            UserSession.setIsAppUser(false);

            int logId = MyDataBase.getInstance().insertLoginLog(user.getId(), user.getAdminType().name());
            UserSession.setCurrentLoginLogId(logId);

            notifyAdminsForAdmin(user, "Connexion");
            if (user.getAdminType() == User.AdminType.EVENT_MANAGER) {
                switchScene("/com/esprit/EventAdmin.fxml");
            } else {
                switchScene("/Dashboard.fxml");
            }
        } else {
            AppUser appUser = result.getAppUser();
            UserSession.setCurrentUserEmail(appUser.getEmail());
            UserSession.setIsAppUser(true);
            UserSession.setCurrentAppUserType(appUser.getUserType());
            UserSession.setCurrentUserRole(null);

            int logId = MyDataBase.getInstance().insertLoginLog(appUser.getId(), appUser.getUserType().name());
            UserSession.setCurrentLoginLogId(logId);

            notifyAdminsForAppUser(appUser, "Connexion");
            switchScene("/UserDashboard.fxml");
        }
    }

    @FXML
    private void handleFaceLogin() {
        if (!faceIdService.isConfigured()) {
            showError("CompreFace n'est pas configure.");
            return;
        }

        Path capturedImage;
        try {
            capturedImage = captureFaceImageFromCamera();
        } catch (IOException | IllegalStateException exception) {
            showError(exception.getMessage());
            return;
        }

        if (capturedImage == null) {
            // L'utilisateur a annule la capture.
            return;
        }

        CompreFaceFaceIdService.RecognitionResult recognitionResult =
                faceIdService.recognizeByImage(capturedImage);

        try {
            Files.deleteIfExists(capturedImage);
        } catch (IOException ignored) {
            // Nettoyage best effort du fichier temporaire.
        }

        if (recognitionResult.status() != CompreFaceFaceIdService.RecognitionStatus.MATCH) {
            showError(recognitionResult.message());
            return;
        }

        // Try admin tables first
        User adminUser = userService.findByFaceSubject(recognitionResult.subject());
        if (adminUser != null) {
            UserSession.setCurrentUserEmail(adminUser.getEmail());
            UserSession.setCurrentUserRole(adminUser.getAdminType());
            UserSession.setIsAppUser(false);

            int logId = MyDataBase.getInstance().insertLoginLog(adminUser.getId(), adminUser.getAdminType().name());
            UserSession.setCurrentLoginLogId(logId);

            notifyAdminsForAdmin(adminUser, "Connexion (Face ID)");
            switchScene("/Dashboard.fxml");
            return;
        }

        // Try users table
        AppUser appUser = userService.findAppUserByFaceSubject(recognitionResult.subject());
        if (appUser != null) {
            UserSession.setCurrentUserEmail(appUser.getEmail());
            UserSession.setIsAppUser(true);
            UserSession.setCurrentAppUserType(appUser.getUserType());
            UserSession.setCurrentUserRole(null);

            int logId = MyDataBase.getInstance().insertLoginLog(appUser.getId(), appUser.getUserType().name());
            UserSession.setCurrentLoginLogId(logId);

            notifyAdminsForAppUser(appUser, "Connexion (Face ID)");
            switchScene("/UserDashboard.fxml");
            return;
        }

        showError("Visage reconnu, mais aucun compte local n'est associe.");
    }

    private void switchScene(String fxml) {
        SceneNavigator.navigate(emailField, fxml, this::showError);
    }

    private void notifyAdminsForAdmin(User user, String actionType) {
        String subject = actionType + " utilisateur";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String message = String.format(
            "Un utilisateur s'est connecte.\n\n" +
            "Nom : %s %s\n" +
            "Email : %s\n" +
            "Date et heure : %s\n" +
            "Type d'action : %s",
            user.getFirstName(), user.getLastName(), user.getEmail(), now, actionType
        );

        // Run in background to avoid blocking UI
        new Thread(() -> emailService.sendEmailToAdmins(subject, message)).start();
    }

    private void notifyAdminsForAppUser(AppUser appUser, String actionType) {
        String subject = actionType + " utilisateur";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
            "Un utilisateur s'est connecte.\n\n" +
            "Nom : %s %s\n" +
            "Email : %s\n" +
            "Role : %s\n" +
            "Date et heure : %s\n" +
            "Type d'action : %s",
            appUser.getFirstName(), appUser.getLastName(), appUser.getEmail(),
            appUser.getUserType().name(), now, actionType
        );

        new Thread(() -> emailService.sendEmailToAdmins(subject, message)).start();
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add("status-error");
        messageLabel.setText(message);
    }

    /**
     * Ouvre une fenêtre modale avec le preview webcam en direct.
     * L'utilisateur peut se positionner puis cliquer "Capturer".
     * Retourne null si l'utilisateur annule.
     */
    private Path captureFaceImageFromCamera() throws IOException {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new IllegalStateException("Aucune camera detectee sur ce PC.");
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        // Conteneur pour le résultat capturé (tableau à 1 élément pour mutation dans lambda)
        final Path[] capturedPath = {null};

        // --- Construction de la fenêtre de preview ---
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(emailField.getScene().getWindow());
        dialog.setTitle("Face ID - Preview Camera");
        dialog.setResizable(false);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(46, 125, 50, 0.25), 16, 0.2, 0, 4);");

        Label instructionLabel = new Label("Positionnez votre visage face a la camera");
        instructionLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2E7D32;");

        Button captureBtn = new Button("📸  Capturer");
        captureBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 28; -fx-cursor: hand;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: #e4f3df; -fx-text-fill: #245126; -fx-font-size: 13px; " +
                "-fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 10 22; -fx-cursor: hand;");

        HBox buttonBox = new HBox(14, captureBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.setStyle(
                "-fx-background-color: #1a1a1a; -fx-background-radius: 14; -fx-padding: 6;");

        VBox root = new VBox(16, instructionLabel, imageContainer, buttonBox);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fcf2, #F1F8E9);");

        Scene scene = new Scene(root);
        dialog.setScene(scene);

        // --- Thread en arrière-plan pour capturer les frames webcam ---
        final boolean[] running = {true};

        Thread webcamThread = new Thread(() -> {
            while (running[0]) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> imageView.setImage(fxImage));
                    }
                    Thread.sleep(33); // ~30 fps
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ex) {
                    break;
                }
            }
        });
        webcamThread.setDaemon(true);
        webcamThread.setName("webcam-preview");
        webcamThread.start();

        // --- Actions boutons ---
        captureBtn.setOnAction(e -> {
            running[0] = false;
            BufferedImage snapshot = webcam.getImage();
            if (snapshot != null) {
                try {
                    Path tempImage = Files.createTempFile("face-login-", ".jpg");
                    ImageIO.write(snapshot, "JPG", tempImage.toFile());
                    capturedPath[0] = tempImage;
                } catch (IOException ex) {
                    // sera traité comme annulation
                }
            }
            webcam.close();
            dialog.close();
        });

        cancelBtn.setOnAction(e -> {
            running[0] = false;
            webcam.close();
            dialog.close();
        });

        dialog.setOnCloseRequest(e -> {
            running[0] = false;
            if (webcam.isOpen()) {
                webcam.close();
            }
        });

        // Affichage modal bloquant (attend la fermeture)
        dialog.showAndWait();

        return capturedPath[0];
    }

    private void loadRecaptchaWidget() {
        WebEngine webEngine = recaptchaView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("captchaBridge", new CaptchaBridge());
            }
        });
        try {
            String recaptchaUrl = recaptchaPageServer.start(RecaptchaConfig.siteKey());
            webEngine.load(recaptchaUrl);
        } catch (IOException exception) {
            showError("Impossible de charger reCAPTCHA localement.");
        }
    }

    private void resetCaptcha() {
        recaptchaView.getEngine().executeScript("if (window.grecaptcha) { grecaptcha.reset(); }");
    }

    private String readTokenFromWidget() {
        try {
            Object token = recaptchaView.getEngine().executeScript(
                    "(window.grecaptcha && grecaptcha.getResponse) ? grecaptcha.getResponse() : ''"
            );
            return token == null ? "" : token.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    public class CaptchaBridge {
        public void onToken(String token) {
            // Le token est relu depuis grecaptcha.getResponse() au moment du login.
        }
    }
}

