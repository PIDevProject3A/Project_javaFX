module com.bledna {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;
    requires java.net.http;

    opens com.bledna to javafx.fxml, javafx.graphics;
    opens com.bledna.controller to javafx.fxml;
    opens com.bledna.model to javafx.base;

    exports com.bledna;
}
