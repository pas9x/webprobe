package net.pascalhp.webprobe.windows;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.pascalhp.webprobe.Localization;
import net.pascalhp.webprobe.controllers.AboutWindowController;
import net.pascalhp.webprobe.helpers.MiscHelper;

import java.io.IOException;
import java.util.Map;

import static net.pascalhp.webprobe.Application.app;
import static net.pascalhp.webprobe.Localization.lang;

public class AboutWindow {
    Parent root;
    Stage stage;
    AboutWindowController controller;

    public AboutWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(MiscHelper.getResourceURL("windows/AboutWindow.fxml"), Localization.getInstance().getBundle());
        this.root = loader.load();
        this.stage = new Stage();
        this.stage.setTitle(lang("about_window.title"));
        this.stage.setScene(new Scene(root));
        this.stage.initOwner(app.mainWindow.stage);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.controller = loader.getController();

        Map<String, String> data = Map.of("version", app.getVersion().getFull());
        this.controller.aboutText.setText(lang("about_window.about", data));
    }

    public void close() {
        this.stage.close();
    }

    public void show() {
        this.stage.show();
    }
}
