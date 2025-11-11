package net.pascalhp.webprobe.helpers;

import net.pascalhp.webprobe.ErrorHandler;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MiscHelper {

    public static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            ErrorHandler.log(Level.INFO, "Sleep(" + seconds + "): thread interrupted");
        }
    }

    public static URL getResourceURL(String path) {
        return ClassLoader.getSystemClassLoader().getResource(path);
    }

    public static InputStream getResourceStream(String path) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(path);
    }
}
