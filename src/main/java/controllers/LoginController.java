package controllers;

import entities.User;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import services.CompreFaceFaceIdService;
import services.RecaptchaService;
import services.UserService;
import utils.LocalRecaptchaPageServer;
import utils.RecaptchaConfig;
import utils.SceneNavigator;
import utils.UserSession;

import java.io.File;
import java.io.IOException;

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

    @FXML
    private void initialize() {
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

        User user = userService.login(emailField.getText(), passwordField.getText());
        if (user == null) {
            showError("Invalid email or password.");
            return;
        }
        UserSession.setCurrentUserEmail(user.getEmail());
        UserSession.setCurrentUserRole(user.getAdminType());
        switchScene("/Welcome.fxml");
    }

    @FXML
    private void handleFaceLogin() {
        if (!faceIdService.isConfigured()) {
            showError("CompreFace n'est pas configure.");
            return;
        }

        File imageFile = pickFaceImage();
        if (imageFile == null) {
            showError("Selection d'image annulee.");
            return;
        }

        CompreFaceFaceIdService.RecognitionResult recognitionResult =
                faceIdService.recognizeByImage(imageFile.toPath());

        if (recognitionResult.status() != CompreFaceFaceIdService.RecognitionStatus.MATCH) {
            showError(recognitionResult.message());
            return;
        }

        User user = userService.findByFaceSubject(recognitionResult.subject());
        if (user == null) {
            showError("Visage reconnu, mais aucun compte local n'est associe.");
            return;
        }

        UserSession.setCurrentUserEmail(user.getEmail());
        UserSession.setCurrentUserRole(user.getAdminType());
        switchScene("/Welcome.fxml");
    }

    @FXML
    private void openFaceEnrollment() {
        switchScene("/FaceIdEnroll.fxml");
    }


    private void switchScene(String fxml) {
        SceneNavigator.navigate(emailField, fxml, this::showError);
    }

    private void showError(String message) {
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add("status-error");
        messageLabel.setText(message);
    }

    private File pickFaceImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image de visage");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.webp"
        ));
        return chooser.showOpenDialog(emailField.getScene() == null ? null : emailField.getScene().getWindow());
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

