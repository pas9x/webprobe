package net.pascalhp.webprobe.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import net.pascalhp.webprobe.exceptions.ErrorMessage;
import net.pascalhp.webprobe.helpers.MiscHelper;

import static net.pascalhp.webprobe.Application.app;
import static net.pascalhp.webprobe.Localization.lang;

public class MainWindowController {
    private Image imageClock;
    private Image imageCheck;

    @FXML public Menu languagesMenu;
    @FXML public TextField siteUrlText;
    @FXML public Button startCheckButton;
    @FXML public Button saveReportButton;

    @FXML public HBox lineGethostbyname;
    @FXML public HBox lineJndiDns;
    @FXML public HBox lineDnsjava;
    @FXML public HBox linePing;
    @FXML public HBox lineMtr;
    @FXML public HBox lineHttp;
    @FXML public HBox lineAllChecksDone;

    @FXML public ImageView iconGethostbyname;
    @FXML public ImageView iconJndiDns;
    @FXML public ImageView iconDnsjava;
    @FXML public ImageView iconPing;
    @FXML public ImageView iconMtr;
    @FXML public ImageView iconHttp;

    @FXML public Label statusGethostbyname;
    @FXML public Label statusJndiDns;
    @FXML public Label statusDnsjava;
    @FXML public Label statusPing;
    @FXML public Label statusMtr;
    @FXML public Label statusHttp;

    public MainWindowController() {
        this.imageClock = new Image(MiscHelper.getResourceStream("icons/clock.png"));
        this.imageCheck = new Image(MiscHelper.getResourceStream("icons/check.png"));
    }

    @FXML
    public void onStartCheckClicked() {
        app.mainWindow.startCheck();
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode().getCode() == 10) {
            app.mainWindow.startCheck();
        }
    }

    @FXML
    public void onAboutButtonClick() {
        app.aboutWindow.show();
    }

    @FXML
    public void onSaveReportClick() {
        app.mainWindow.saveReport();
    }

    @FXML
    public void onLanguageMenuItemClick(ActionEvent event) {
        MenuItem menuItem = (MenuItem)event.getTarget();
        String languageCode = menuItem.getProperties().get("languageCode").toString();
        app.switchLanguage(languageCode);
    }

    @FXML
    public void languagesNotLoadedError() {
        ErrorMessage err = new ErrorMessage("For some reason, the language menu wasn't builded. This is a bug.");
        err.alert();
    }

    public void initGethostbyname() {
        this.lineGethostbyname.setManaged(true); this.lineGethostbyname.setVisible(true);
        this.iconGethostbyname.setImage(this.imageClock);
        this.statusGethostbyname.setVisible(false);
    }

    public void initJndiDns() {
        this.lineJndiDns.setManaged(true); this.lineJndiDns.setVisible(true);
        this.iconJndiDns.setImage(this.imageClock);
        this.statusJndiDns.setVisible(false);
    }

    public void initDnsjava() {
        this.lineDnsjava.setManaged(true); this.lineDnsjava.setVisible(true);
        this.iconDnsjava.setImage(this.imageClock);
        this.statusDnsjava.setVisible(false);
    }

    public void initPing() {
        this.linePing.setManaged(true); this.linePing.setVisible(true);
        this.iconPing.setImage(this.imageClock);
        this.statusPing.setVisible(false);
    }

    public void initMtr() {
        this.lineMtr.setManaged(true); this.lineMtr.setVisible(true);
        this.iconMtr.setImage(this.imageClock);
        this.statusMtr.setVisible(false);
    }

    public void initHttp() {
        this.lineHttp.setManaged(true); this.lineHttp.setVisible(true);
        this.iconHttp.setImage(this.imageClock);
        this.statusHttp.setVisible(false);
    }

    public void doneGethostbyname(Boolean success) {
        iconGethostbyname.setImage(this.imageCheck);
        statusGethostbyname.setText(doneStatus(success));
        statusGethostbyname.setVisible(true);
    }

    public void doneJndiDns(Boolean success) {
        iconJndiDns.setImage(this.imageCheck);
        statusJndiDns.setText(doneStatus(success));
        statusJndiDns.setVisible(true);
    }

    public void doneDnsjava(Boolean success) {
        iconDnsjava.setImage(this.imageCheck);
        statusDnsjava.setText(doneStatus(success));
        statusDnsjava.setVisible(true);
    }

    public void donePing(Boolean success) {
        iconPing.setImage(this.imageCheck);
        statusPing.setText(doneStatus(success));
        statusPing.setVisible(true);
    }

    public void doneMtr(Boolean success) {
        iconMtr.setImage(this.imageCheck);
        statusMtr.setText(doneStatus(success));
        statusMtr.setVisible(true);
    }

    public void doneHttp(Boolean success) {
        iconHttp.setImage(this.imageCheck);
        statusHttp.setText(doneStatus(success));
        statusHttp.setVisible(true);
    }

    private String doneStatus(Boolean success) {
        if (success == null) {
            return lang("main_window.report.done");
        }
        if (success == true) {
            return lang("main_window.report.success");
        }
        return lang("main_window.report.problem");
    }

    public void hideAllStatus() {
        statusGethostbyname.setVisible(false);
        statusJndiDns.setVisible(false);
        statusDnsjava.setVisible(false);
        statusPing.setVisible(false);
        statusMtr.setVisible(false);
        statusHttp.setVisible(false);
    }

    public void hideAllLines() {
        lineGethostbyname.setManaged(false); lineGethostbyname.setVisible(false);
        lineJndiDns.setManaged(false); lineJndiDns.setVisible(false);
        lineDnsjava.setManaged(false); lineDnsjava.setVisible(false);
        linePing.setManaged(false); linePing.setVisible(false);
        lineMtr.setManaged(false); lineMtr.setVisible(false);
        lineHttp.setManaged(false); lineHttp.setVisible(false);
        lineAllChecksDone.setManaged(false); lineAllChecksDone.setVisible(false);
    }
}
