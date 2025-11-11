package net.pascalhp.webprobe;

import java.util.Arrays;

public class FileName {
    public String name;
    public String extension;

    private FileName() {
    }

    public FileName(String filename) {
        if (filename == null) {
            throw new NullPointerException("filename == null");
        }
        String[] pieces = filename.split("\\.", -1);

        if (pieces.length < 2) {
            this.name = pieces[0];
            return;
        }

        String[] namePieces = Arrays.copyOfRange(pieces, 0, pieces.length - 1);
        this.name = String.join(".", namePieces);

        this.extension = pieces[pieces.length - 1];
    }

    public String toString() {
        String result = "";
        if (this.name != null) {
            result += this.name;
        }
        if (this.extension == null) {
            return result;
        }
        result += "." + this.extension;
        return result;
    }

    public FileName clone() {
        FileName result = new FileName();
        result.name = this.name;
        result.extension = this.extension;
        return result;
    }
}
