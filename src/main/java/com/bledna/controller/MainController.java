package com.bledna.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Root controller — manages sidebar navigation and dynamic view loading.
 */
public class MainController {

    @FXML private StackPane contentPane;
    @FXML private Button    btnWaste;
    @FXML private Button    btnPrevue;

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
        if (!active.getStyleClass().contains("nav-btn-active")) {
            active.getStyleClass().add("nav-btn-active");
        }
    }
}
