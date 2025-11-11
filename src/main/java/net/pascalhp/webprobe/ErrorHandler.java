package net.pascalhp.webprobe;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static net.pascalhp.webprobe.Application.app;

public class ErrorHandler implements Thread.UncaughtExceptionHandler {
    public final String errorLogFile;
    private Handler handler;
    private Logger logger;

    public ErrorHandler(String errorLogFile) {
        this.errorLogFile = errorLogFile;
    }

    public static void logException(Throwable e) {
        log(Level.WARNING, formatException(e));
    }

    public static String formatException(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static void log(Level level, String message) {
        Logger logger = Logger.getGlobal();
        logger.log(level, message);
    }

    public void setup() throws IOException {
        this.logger = LogManager.getLogManager().getLogger("");
        if (this.logger == null) {
            throw new RuntimeException("Failed to get default logger");
        }

        this.handler = new FileHandler(this.errorLogFile, false);
        this.handler.setFormatter(new SimpleFormatter());
        this.logger.addHandler(this.handler);

        log(Level.INFO, "WebProbe version: " + app.getVersion().getFull());
        log(Level.INFO, "OS: " + Application.getOsInfo());

        Thread.currentThread().setUncaughtExceptionHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void uncaughtException(Thread thread, Throwable e) {
        this.logger.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), e);
    }
}
