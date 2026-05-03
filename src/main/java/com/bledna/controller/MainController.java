package com.bledna.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import utils.MyDataBase;
import utils.SceneNavigator;
import utils.UserSession;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentPane;
    @FXML private Button    btnWaste;
    @FXML private Button    btnPrevue;
    @FXML private Button    btnLogout;

    @FXML
    public void initialize() {
        // initialise appel direc fi chargm fxml
        loadView("/com/bledna/waste_view.fxml", btnWaste);
    }

    @FXML
    public void showWaste() {
        loadView("/com/bledna/waste_view.fxml", btnWaste);
    }

    @FXML
    public void showPrevue() {
        loadView("/com/bledna/prevue_view.fxml", btnPrevue);
    }

    @FXML
    public void handleLogout() {
        // Update logout time in database
        int logId = UserSession.getCurrentLoginLogId();
        if (logId != -1) {
            MyDataBase.getInstance().updateLogoutTime(logId);
        }

        UserSession.clear();
        SceneNavigator.navigate(btnLogout, "/Login.fxml", System.err::println);
    }

    // load view hyya bch tchargi nouveau fxml et le place dans un stackpane

    private void loadView(String fxmlPath, Button activeBtn) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);
            setActiveButton(activeBtn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button active) {
        btnWaste.getStyleClass().remove("nav-btn-active");
        btnPrevue.getStyleClass().remove("nav-btn-active");
        btnLogout.getStyleClass().remove("nav-btn-active");
        
        if (active != null && !active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }
}
