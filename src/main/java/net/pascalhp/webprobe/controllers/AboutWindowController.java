package net.pascalhp.webprobe.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import net.pascalhp.webprobe.Application;

import static net.pascalhp.webprobe.Application.app;

public class AboutWindowController {
    @FXML
    public TextArea aboutText;

    public void onCloseButtonClick() {
        app.aboutWindow.close();
    }

    @FXML
    public void onAppIconClick() {
        Application.openHyperlink("https://pascalhp.net/webprobe/");
    }
}