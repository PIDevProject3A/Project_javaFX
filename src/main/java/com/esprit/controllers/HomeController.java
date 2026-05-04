package com.esprit.controllers;

import com.esprit.utils.NavigationManager;
import javafx.fxml.FXML;

import java.io.IOException;

public class HomeController {

    @FXML
    private void openAdmin() throws IOException {
        NavigationManager.navigateTo("EventAdmin.fxml");
    }

    @FXML
    private void openCatalog() throws IOException {
        NavigationManager.navigateTo("EventCatalog.fxml");
    }
}

