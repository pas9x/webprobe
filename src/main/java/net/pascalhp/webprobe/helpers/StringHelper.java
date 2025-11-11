package net.pascalhp.webprobe.helpers;

import java.io.*;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import java.security.MessageDigest;

public class StringHelper {
    public static boolean isRawDomain(String domain) {
        return domain.matches("^[a-zA-Z0-9\\-\\\\.]+$");
    }

    public static String getRawDomain(String domain) {
        return IDN.toASCII(domain);
    }

    public static String getReadableDomain(String domain) {
        return IDN.toUnicode(domain);
    }

    public static String readStream(InputStream stream) throws IOException {
        StringBuilder result = new StringBuilder();
        readStreamTo(stream, result);
        return result.toString();
    }

    public static void readStreamTo(InputStream stream, StringBuilder buf) throws IOException {
        InputStreamReader ir = new InputStreamReader(stream, StandardCharsets.UTF_8);
        int charsRead;
        char[] buffer = new char[1024];
        while (true) {
            charsRead = ir.read(buffer);
            if (charsRead > 0) {
                buf.append(buffer, 0, charsRead);
            } else {
                break;
            }
        }
    }

    public static List<String> readStreamLines(InputStream stream) throws IOException {
        InputStreamReader ir = new InputStreamReader(stream, StandardCharsets.UTF_8);
        return readStreamLines(ir);
    }

    public static List<String> readStreamLines(InputStreamReader reader) throws IOException {
        BufferedReader linesReader = new BufferedReader(reader);
        return readStreamLines(linesReader);
    }

    public static List<String> readStreamLines(BufferedReader linesReader) throws IOException {
        String line;
        LinkedList<String> lines = new LinkedList<>();
        while (true) {
            line = linesReader.readLine();
            if (line == null) {
                break;
            } else {
                lines.add(line);
            }
        }
        return lines;
    }

    public static int intval(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Throwable e) {
            return 0;
        }
    }

    public static float floatval(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (Throwable e) {
            return 0;
        }
    }

    public static boolean isIPv4(String ip) {
        if (ip == null) {
            return false;
        }
        if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return (addr instanceof Inet4Address);
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isIPv6(String ip) {
        if (ip == null) {
            return false;
        }
        if (!ip.matches("^[a-fA-F0-9\\\\:]+$")) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return (addr instanceof Inet6Address);
        } catch (Throwable e) {
            return false;
        }
    }

    // TODO: delete
    @Deprecated
    public static String getURI(URI uri) {
        String result = uri.getPath();
        String query = uri.getQuery();
        if (query != null) {
            result += "?";
            result += query;
        }
        return result;
    }

    public static String[] explode(String str, String delimiter, int limit) {
        if (str == null) throw new NullPointerException("str == null");
        if (delimiter == null) throw new NullPointerException("delimiter == null");
        String pattern = Pattern.quote(delimiter);
        return str.split(pattern, limit);
    }

    public static String[] explode(String str, String delimiter) {
        if (str == null) throw new NullPointerException("str == null");
        if (delimiter == null) throw new NullPointerException("delimiter == null");
        String pattern = Pattern.quote(delimiter);
        return str.split(pattern, -1); // https://stackoverflow.com/questions/14602062/java-string-split-removed-empty-values
    }

    public static String ucwords(String str, String delimiter) {
        if (str == null) throw new NullPointerException("str == null");
        if (delimiter == null) throw new NullPointerException("delimiter == null");
        String[] pieces = explode(str.toLowerCase(), delimiter);

        LinkedList<String> ucPieces = new LinkedList<>();
        for (String piece : pieces) {
            if (piece.isEmpty()) {
                continue;
            }
            ucPieces.add(piece.substring(0, 1).toUpperCase() + piece.substring(1));
        }

        return String.join(delimiter, ucPieces);
    }

    public static String md5(String data) {
        MessageDigest hasher;
        try {
            hasher = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return toHex(hasher.digest(data.getBytes()));
    }

    public static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String toHex(String text) {
        return toHex(text.getBytes());
    }
}
