package net.pascalhp.webprobe;

public enum OSType {
    WINDOWS,
    UNIX,
    MACOS,
    UNKNOWN;

    public static OSType detect() {
        String os = System.getProperty("os.name");
        if (os == null) {
            return OSType.UNKNOWN;
        }
        os = os.toLowerCase();

        if (os.matches("^win.*")) {
            return OSType.WINDOWS;
        }

        if (os.matches("^mac.*")) {
            return OSType.MACOS;
        }

        if (os.contains("linux")) {
            return OSType.UNIX;
        }

        if (os.contains("bsd")) {
            return OSType.UNIX;
        }

        return OSType.UNKNOWN;
    }

    public String toString() {
        if (this == WINDOWS) {
            return "Windows";
        }
        if (this == UNIX) {
            return "Unix";
        }
        if (this == MACOS) {
            return "MacOS";
        }
        return "Unknown";
    }
}
