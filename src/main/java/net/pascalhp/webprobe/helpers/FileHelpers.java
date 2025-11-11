package net.pascalhp.webprobe.helpers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHelpers {
    public static boolean isDir(String dir) {
        Path path = Paths.get(dir);
        if (path == null) {
            return false;
        }
        return Files.isDirectory(path);
    }

    public static boolean isFile(String dir) {
        Path path = Paths.get(dir);
        if (path == null) {
            return false;
        }
        return Files.isRegularFile(path);
    }
}
