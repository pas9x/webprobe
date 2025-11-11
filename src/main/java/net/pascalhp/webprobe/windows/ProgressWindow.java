package net.pascalhp.webprobe.windows;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.pascalhp.webprobe.Localization;
import net.pascalhp.webprobe.controllers.ProgressWindowController;
import net.pascalhp.webprobe.helpers.MiscHelper;

import java.io.IOException;

import static net.pascalhp.webprobe.Application.app;
import static net.pascalhp.webprobe.Localization.lang;

public class ProgressWindow {
    Parent root;
    Stage stage;
    ProgressWindowController controller;

    public ProgressWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(MiscHelper.getResourceURL("windows/ProgressWindow.fxml"), Localization.getInstance().getBundle());
        this.root = loader.load();
        this.stage = new Stage();
        this.stage.setTitle(lang("progress_window.title"));
        this.stage.setScene(new Scene(root));
        this.stage.initOwner(app.mainWindow.stage);
        this.stage.initModality(Modality.WINDOW_MODAL);
        this.stage.setOnCloseRequest(this::onCloseRequest);
        this.controller = loader.getController();
    }

    public void close() {
        this.stage.close();
    }

    public void show(String message) {
        this.controller.message.setText(message);
        this.stage.show();
    }

    public void onCloseRequest(WindowEvent event) {
        event.consume();
    }
}
