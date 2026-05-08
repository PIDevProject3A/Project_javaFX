import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.services.NotificationService;
import com.esprit.utils.LanguageManager;
import com.esprit.utils.ThemeManager;

public class Main extends Application {
    private static final double APP_WIDTH = 900;
    private static final double APP_HEIGHT = 600;

    @Override
    public void init() throws Exception {
        super.init();
        try {
            NotificationService.getInstance().startRestApi();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Initialize language and theme managers
        LanguageManager languageManager = LanguageManager.getInstance();
        ThemeManager themeManager = ThemeManager.getInstance();
        
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/Login.fxml"), 
            languageManager.getBundle()
        );
        Parent root = loader.load();
        
        Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        
        // Apply theme
        themeManager.applyTheme(scene);
        
        stage.setTitle("BLADNA");
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        NotificationService.getInstance().stopRestApi();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


