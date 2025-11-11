package net.pascalhp.webprobe;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.scene.layout.Region;
import net.pascalhp.webprobe.dto.AppVersion;
import net.pascalhp.webprobe.exceptions.BugException;
import net.pascalhp.webprobe.exceptions.NotFoundException;
import net.pascalhp.webprobe.helpers.FileHelpers;
import net.pascalhp.webprobe.helpers.MiscHelper;
import net.pascalhp.webprobe.helpers.SystemHelper;
import net.pascalhp.webprobe.windows.AboutWindow;
import net.pascalhp.webprobe.windows.MainWindow;
import net.pascalhp.webprobe.windows.ProgressWindow;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;

import static net.pascalhp.webprobe.Localization.lang;

public class Application {
    public static final Application app;
    public OSType os;
    public MainWindow mainWindow;
    public AboutWindow aboutWindow;
    public ProgressWindow progressWindow;
    public ErrorHandler errorHandler;
    public String appDir;
    public String baseDir;
    public String systemInfo;

    private Settings settings;
    private AppVersion version;

    static {
        app = new Application();
    }

    public void init() throws Throwable {
        this.os = OSType.detect();

        this.appDir = this.getAppDir();
        this.baseDir = this.detectBaseDir();
        System.out.println("WebProbe data dir: " + this.appDir);

        this.errorHandler = new ErrorHandler(this.appDir + "/error.log");
        this.errorHandler.setup();

        this.getVersion();
        Settings settings = this.getSettings();
        if (!settings.lang.equals(Localization.getInstance().code)) {
            Localization.switchTo(settings.lang);
        }

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");
        this.initCacerts();
    }

    private void initCacerts() {
        File cacertsFile = new File(this.appDir + "/cacert.jks");
        if (!cacertsFile.isFile()) {
            InputStream cacertsInputStream = MiscHelper.getResourceStream("cacert.jks");
            if (cacertsInputStream == null) {
                ErrorHandler.log(Level.WARNING, "Alternative CA certificates store (cacert.jks) not found");
                return;
            }
            try (FileOutputStream cacertsOutputStream = new FileOutputStream(cacertsFile)) {
                cacertsInputStream.transferTo(cacertsOutputStream);
            } catch (Throwable e) {
                ErrorHandler.logException(e);
                return;
            }
        }
        ErrorHandler.log(Level.INFO, "Using alternative CA certificates store " + this.appDir + "/cacert.jks");
        System.setProperty("javax.net.ssl.trustStore", this.appDir + "/cacert.jks");
    }

    public static void main(String[] args) throws Throwable {
        app.init();
        MainWindow.go(args);
    }

    public void onJavafxInitialized() throws IOException {
        this.aboutWindow = new AboutWindow();
        this.progressWindow = new ProgressWindow();
    }

    public static void alert(String text, String title, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        // Selectalbe text
        /*
        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth(Double.MAX_VALUE);
        gridPane.add(textArea, 0, 0);
        alert.getDialogPane().setContent(gridPane);
         */

        alert.showAndWait();
    }

    public static void displayInfo(String text, String title) {
        alert(text, title, AlertType.INFORMATION);
    }

    public static void displayInfo(String text) {
        displayInfo(text, "WebProbe");
    }

    public static void displayError(String message, String title) {
        alert(message, title, Alert.AlertType.ERROR);
    }

    public static void displayError(String message) {
        displayError(message, lang("error_message_title"));
    }

    public static void openHyperlink(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.net.URI uri = new java.net.URI(url);
                    desktop.browse(uri);
                    return;
                }
            }
        } catch (Throwable e) {
            ErrorHandler.logException(e);
            openHyperlinkFallback(url);
            return;
        }

        if (app.os == OSType.UNIX) {
            SystemHelper.ExecResult result = null;
            try {
                result = SystemHelper.exec("xdg-open " + url);
            } catch (Throwable e) {
                ErrorHandler.logException(e);
            }
            if (result != null && result.exitCode == 0) {
                return;
            }
        }

        openHyperlinkFallback(url);
    }

    private static void openHyperlinkFallback(String url) {
        displayInfo(lang("no_browser", Map.of("url", url)));
    }

    public void switchLanguage(String code) {
        if (code == null) {
            throw new NullPointerException("Language code == null");
        }
        if (code.equals(Localization.getInstance().code)) {
            return;
        }

        Localization lang;
        try {
            lang  = Localization.getLanguageByCode(code);
        } catch (NotFoundException e) {
            displayError(e.getMessage());
            return;
        }

        Settings settings = this.getSettings();
        settings.lang = lang.code;
        this.saveSettings();

        displayInfo(lang("main_window.switch_language_reboot"));
    }

    protected String getSettingsFile() {
        return this.appDir + "/settings.json";
    }

    public Settings getSettings() {
        if (this.settings == null) {
            String settingsFile = this.getSettingsFile();
            try {
                this.settings = Settings.loadFromFile(settingsFile);
            } catch (FileNotFoundException e) {
                this.settings = Settings.getDefaultSettings(this);
            } catch (Throwable e) {
                ErrorHandler.logException(e);
                this.settings = Settings.getDefaultSettings(this);
            }
        }
        return this.settings;
    }

    public void saveSettings() {
        try {
            Settings settings = this.getSettings();
            String settingsFile = this.getSettingsFile();
            settings.saveToFile(settingsFile);
        } catch (Throwable e) {
            String error = "Failed to save application settings: " + e.toString();
            displayError(error);
        }
    }

    private String detectBaseDir() throws URISyntaxException, IOException {
        var classLocation = Application.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        File file = new File(classLocation);
        if (file.isFile()) {
            File parent = file.getParentFile();
            if (parent == null) {
                throw new RuntimeException("Failed to detect application base directory (1)");
            }
            return parent.getCanonicalPath();
        } else {
            return file.getCanonicalPath();
        }
    }

    private String getAppDir() {
        String dir = this.detectAppDir();
        if (!FileHelpers.isDir(dir)) {
            try {
                Files.createDirectory(Paths.get(dir));
            } catch (Throwable e) {
                throw new RuntimeException("Failed to create data directory " + this.appDir, e);
            }
        }
        return dir;
    }

    protected String detectAppDir() {
        if (this.os == OSType.UNIX) {
            return this.detectAppDirUnix();
        }
        if (this.os == OSType.WINDOWS) {
            return this.detectAppDirWindows();
        }
        if (this.os == OSType.MACOS) {
            return this.detectAppDirMac();
        }
        throw new BugException("Unsupported operating system");
    }

    protected String detectAppDirWindows() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            return appData + "\\webprobe";
        }

        String homedir = System.getProperty("user.home");
        if (homedir != null && !homedir.isEmpty()) {
            return homedir + "\\AppData\\Roaming\\webprobe";
        }

        throw new RuntimeException("Failed to determine application data dir");
    }

    protected String detectAppDirUnix() {
        String homedir = System.getProperty("user.home");
        if (homedir == null || homedir.isEmpty()) {
            throw new RuntimeException("Failed to determine application data dir");
        }
        return homedir + "/.webprobe";
    }

    protected String detectAppDirMac() {
        String homedir = System.getProperty("user.home");
        if (homedir == null || homedir.isEmpty()) {
            throw new RuntimeException("Failed to determine application data dir (1)");
        }
        String appsDir = homedir + "/Library/Application Support";
        if (!FileHelpers.isDir(appsDir)) {
            throw new RuntimeException("Failed to determine application data dir (2)");
        }
        return appsDir + "/webprobe";
    }

    public AppVersion getVersion() {
        if (this.version == null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                InputStream stream = MiscHelper.getResourceStream("version.json");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                this.version = mapper.readValue(reader, AppVersion.class);
            } catch (Throwable e) {
                this.version = new AppVersion();
            }
        }
        return this.version;
    }

    public void displayUnexpectedError(Throwable e) {
        ErrorHandler.logException(e);
        String error = e.getClass().getName() + ": " + e.getMessage();
        String msg = lang("unexpected_error", Map.of("error", error, "errorLog", this.errorHandler.errorLogFile));
        Platform.runLater(() -> displayError(msg));
    }

    public static String getOsInfo() {
        try {
            if (app.os == OSType.UNIX) {
                if (FileHelpers.isFile("/etc/os-release")) {
                    return Files.readString(Paths.get("/etc/os-release"));
                } else {
                    SystemHelper.ExecResult execResult = SystemHelper.exec("uname -a");
                    if (execResult.exitCode == 0) {
                        return execResult.stdoutString();
                    }
                }
            } else if (app.os == OSType.MACOS) {
                SystemHelper.ExecResult execResult = SystemHelper.exec("sw_vers");
                if (execResult.exitCode == 0) {
                    return execResult.stdoutString();
                }
            } else if (app.os == OSType.WINDOWS) {
                SystemHelper.ExecResult execResult = SystemHelper.exec("ver");
                if (execResult.exitCode == 0) {
                    return execResult.stdoutString().trim();
                }
            }
        } catch (Throwable e) {
        }
        return null;
    }
}
