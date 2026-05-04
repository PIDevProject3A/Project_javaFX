import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;

public class TestLoad {
    public static void main(String[] args) {
        try {
            Platform.startup(() -> {});
            URL url = TestLoad.class.getResource("/com/esprit/EventAdmin.fxml");
            System.out.println("URL: " + url);
            Parent root = FXMLLoader.load(url);
            System.out.println("Load SUCCESS!");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
