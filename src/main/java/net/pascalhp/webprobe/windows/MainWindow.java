package net.pascalhp.webprobe.windows;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.pascalhp.webprobe.Application;
import net.pascalhp.webprobe.Event;
import net.pascalhp.webprobe.EventRouter;
import net.pascalhp.webprobe.Localization;
import net.pascalhp.webprobe.OSType;
import net.pascalhp.webprobe.Report;
import net.pascalhp.webprobe.checker.CheckExecutor;
import net.pascalhp.webprobe.checker.events.OnCheckExit;
import net.pascalhp.webprobe.checker.events.OnCheckStart;
import net.pascalhp.webprobe.checker.events.OnExecutorExit;
import net.pascalhp.webprobe.controllers.MainWindowController;
import net.pascalhp.webprobe.exceptions.BugException;
import net.pascalhp.webprobe.exceptions.ErrorMessage;
import net.pascalhp.webprobe.helpers.MiscHelper;
import net.pascalhp.webprobe.helpers.StringHelper;

import static net.pascalhp.webprobe.Application.app;
import static net.pascalhp.webprobe.Localization.lang;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class MainWindow extends javafx.application.Application {
    public Stage stage;
    public MainWindowController controller;

    private boolean isCheckRunning = false;
    private boolean isStopping = false;
    private CheckExecutor checker;
    private EventRouter checkerEvents;
    private Report report;

    public MainWindow() {
        app.mainWindow = this;
    }

    public static void go(String[] args) {
        System.setProperty("apple.awt.application.name", "Webprobe");

        //Set app icon on macos
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(MiscHelper.getResourceURL("icons/app-icon.png"));
                taskbar.setIconImage(dockIcon);
            }
        }

        launch(args);
    }

    public void start(Stage stage) throws IOException, BugException {
        this.stage = stage;

        URL markupUrl = MiscHelper.getResourceURL("windows/MainWindow.fxml");
        if (markupUrl == null) {
            throw new BugException("Failed to load resource windows/MainWindow.fxml");
        }

        FXMLLoader loader = new FXMLLoader(markupUrl, Localization.getInstance().getBundle());
        Parent root = loader.load();
        this.controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("WebProbe");
        stage.setWidth(640);
        stage.setHeight(400);

        Image icon = new Image(MiscHelper.getResourceStream("icons/app-icon.png"));
        stage.getIcons().add(icon);
        this.controller.hideAllLines();

        this.rebuildLanguagesMenu();
        app.onJavafxInitialized();

        stage.show();
    }

    public void rebuildLanguagesMenu() {
        ObservableList<MenuItem> menuItems =  this.controller.languagesMenu.getItems();
        menuItems.clear();
        String currentLanguageCode = Localization.getInstance().code;

        for (Localization.LangPreview langPreview : Localization.getLangPreviews().values()) {
            String title = langPreview.menuitem;
            CheckMenuItem langItem = new CheckMenuItem(title);
            if (langPreview.code.equals(currentLanguageCode)) {
                langItem.setSelected(true);
            }
            langItem.getProperties().put("languageCode", langPreview.code);
            langItem.setOnAction(this.controller::onLanguageMenuItemClick);
            menuItems.add(langItem);
        }
    }

    public synchronized void startCheck() {
        if (this.isStopping) {
            return;
        }
        if (this.isCheckRunning) {
            this.stopCheck();
            return;
        }

        URL url;
        try {
            url = this.parseUrl();
        } catch (ErrorMessage e) {
            e.alert();
            return;
        }

        this.checkMtrOnMac();

        this.checkerEvents = new EventRouter();
        this.checkerEvents.addEventListener(this::onEvent);
        try {
            this.checker = new CheckExecutor(url, app.os, app.baseDir, app.getSettings().pings, this.checkerEvents);
        } catch (Throwable e) {
            app.displayUnexpectedError(e);
            return;
        }

        this.controller.startCheckButton.setText(lang("main_window.stop_check"));
        this.controller.hideAllLines();
        this.controller.hideAllStatus();
        this.report = null;
        this.controller.saveReportButton.setDisable(true);

        this.isCheckRunning = true;
        this.checker.start();
    }

    public void saveReport() {
        if (this.report == null) {
            Application.displayError("There's no report. Nothing to save. This shouldn't be happening; it's a bug. You can submit a bug report to the program developer.");
            return;
        }
        FileChooser dialog = new FileChooser();
        dialog.setInitialDirectory(new File(System.getProperty("user.home")));
        dialog.setTitle("save_report.dialog_title");
        dialog.setInitialFileName(this.report.title + ".zip");
        dialog.getExtensionFilters().clear();
        dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter(lang("save_report.dialog_ext_zip"), "*.zip"));

        File selectedFile = dialog.showSaveDialog(this.stage);
        if (selectedFile == null) {
            return;
        }

        app.progressWindow.show(lang("save_report.wait"));

        Runnable save = () -> {
            try {
                this.report.saveToZip(selectedFile);
            } catch (Throwable e) {
                Platform.runLater(() -> Application.displayError(lang("save_report.fail", Map.of("error", e.toString()))));
            } finally {
                Platform.runLater(app.progressWindow::close);
            }
        };
        new Thread(save).start();
    }

    public synchronized void stopCheck() {
        if (this.isStopping) {
            return;
        }
        if (!this.isCheckRunning) {
            return;
        }
        this.isStopping = true;
        this.controller.startCheckButton.setDisable(true);
        this.controller.startCheckButton.setText(lang("main_window.stopping"));
        if (this.checker != null) {
            this.checker.stop("The check was stopped by the user");
        }
        this.controller.hideAllLines();
    }

    private void checkMtrOnMac() {
        if (app.os != OSType.MACOS) {
            return;
        }
    }

    private URL parseUrl() {
        String userUrl = this.controller.siteUrlText.getText().trim();
        if (userUrl.isEmpty()) {
            throw new ErrorMessage(lang("main_window.urlerror.empty"));
        }
        if (!userUrl.matches("^[a-zA-Z0-9]{1,50}://.+")) {
            userUrl = "https://" + userUrl;
        }
        URL url;
        try {
            url = new URL(userUrl);
        } catch (Throwable e) {
            throw new ErrorMessage(lang("main_window.urlerror.invalid"));
        }

        String protocol = url.getProtocol();
        String domain = StringHelper.getRawDomain(url.getHost());
        String uri = url.getPath();
        String query = url.getQuery();
        String basicAuth = url.getUserInfo();

        String rawDomain = StringHelper.getRawDomain(domain);
        if (!rawDomain.matches("^[a-zA-Z0-9\\.\\-]+$")) {
            throw new ErrorMessage(lang("main_window.urlerror.invalid"));
        }

        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new ErrorMessage(lang("main_window.urlerror.bad_protocol"));
        }
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }
        if (basicAuth != null) {
            throw new ErrorMessage(lang("main_window.urlerror.basicauth"));
        }
        if (query != null) {
            uri += "?" + query;
        }

        try {
            return new URL(protocol + "://" + rawDomain + uri);
        } catch (Throwable e) {
            throw new ErrorMessage(e.toString());
        }
    }

    private void onEvent(Event event) {
        Platform.runLater(() -> this.onEventJavafxThread(event));
    }

    private void onEventJavafxThread(Event event) {
        if (event instanceof OnCheckStart) {
            OnCheckStart startEvent = (OnCheckStart)event;
            if (startEvent.taskId.equals("gethostbyname")) this.controller.initGethostbyname();
            else if (startEvent.taskId.equals("jndi_dns")) this.controller.initJndiDns();
            else if (startEvent.taskId.equals("dnsjava")) this.controller.initDnsjava();
            else if (startEvent.taskId.equals("ping")) this.controller.initPing();
            else if (startEvent.taskId.equals("mtr")) this.controller.initMtr();
            else if (startEvent.taskId.equals("http")) this.controller.initHttp();
            else {
                // TODO: log error
            }
            return;
        }

        if (event instanceof OnCheckExit) {
            OnCheckExit exitEvent = (OnCheckExit)event;
            if (exitEvent.taskId.equals("gethostbyname")) this.controller.doneGethostbyname(exitEvent.result.success);
            else if (exitEvent.taskId.equals("jndi_dns")) this.controller.doneJndiDns(exitEvent.result.success);
            else if (exitEvent.taskId.equals("dnsjava")) this.controller.doneDnsjava(exitEvent.result.success);
            else if (exitEvent.taskId.equals("ping")) this.controller.donePing(exitEvent.result.success);
            else if (exitEvent.taskId.equals("mtr")) this.controller.doneMtr(exitEvent.result.success);
            else if (exitEvent.taskId.equals("http")) this.controller.doneHttp(exitEvent.result.success);
            else {
                // TODO: log error
            }

            return;
        }

        if (event instanceof OnExecutorExit) {
            this.onCheckExit();
            return;
        }
    }

    private void onCheckExit() {
        if (!this.isStopping) {
            this.report = this.checker.getReport();
            this.extendReport();
            if (this.report != null) {
                this.controller.saveReportButton.setDisable(false);
            }
            this.controller.lineAllChecksDone.setVisible(true);
            this.controller.lineAllChecksDone.setManaged(true);
        }
        this.isStopping = false;
        this.isCheckRunning = false;
        this.controller.startCheckButton.setText(lang("main_window.start_check"));
        this.controller.startCheckButton.setDisable(false);
    }

    private void extendReport() {
        Report.Page page = this.report.pages.get(0);
        String version = app.getVersion().getFull();
        page.log(lang("check.main_page.app_version", Map.of("version", version)));
        version = Application.getOsInfo();
        if (version != null) {
            page.log(lang("check.main_page.os_version", Map.of("version", version)));
        }
    }
}
