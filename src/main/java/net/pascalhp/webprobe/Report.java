package net.pascalhp.webprobe;

import net.pascalhp.webprobe.exceptions.BugException;
import net.pascalhp.webprobe.helpers.DateHelper;
import net.pascalhp.webprobe.helpers.StringHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Report {
    public final LinkedList<Page> pages = new LinkedList<>();
    public final String title;

    public Report(String title) {
        this.title = title;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Page page : this.pages) {
            result.append("---------- ");
            result.append(page.title);
            result.append(" ----------\n");
            result.append(page.toString());
            result.append("\n");
        }
        return result.toString();
    }

    public Map<String, Page.File> getFiles() {
        HashMap<String, Page.File> files = new HashMap<>();
        for (Page page : this.pages) {
            page.files.forEach((name, file) -> {
                if (files.containsKey(name)) {
                    throw new BugException("The Report contains files with same name: " + name);
                } else {
                    files.put(name, file);
                }
            });
        }
        return files;
    }

    public void saveToZip(File file) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file)) {
            saveToZip(stream);
        }
    }

    public void saveToZip(String fileName) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(fileName)) {
            saveToZip(stream);
        }
    }

    public void saveToZip(OutputStream stream) throws IOException {
        ZipOutputStream archive = new ZipOutputStream(stream, StandardCharsets.UTF_8);
        archive.setLevel(1);

        String folder = this.title + "/";
        ZipEntry zipEntry = new ZipEntry(folder + "report.txt");
        archive.putNextEntry(zipEntry);
        archive.write(this.toString().getBytes());
        archive.closeEntry();

        for (Map.Entry<String, Page.File> file : this.getFiles().entrySet()) {
            zipEntry = new ZipEntry(folder + file.getKey());
            archive.putNextEntry(zipEntry);
            archive.write(file.getValue().getContentBytes());
            archive.closeEntry();
        }

        archive.close();
    }

    public static class Page {
        public String title;
        public final LinkedList<Entry> messages = new LinkedList<>();
        public final HashMap<String, File> files = new HashMap<>();

        public Page(String title) {
            this.title = title;
        }

        public Entry log(String message) {
            if (message == null) {
                return null;
            }
            Entry e = new Entry();
            e.timestamp = DateHelper.time();
            e.message = message.trim();
            this.messages.add(e);
            return e;
        }

        public File addFile(String name, String content) {
            File file = new File(name, content);
            return this.addFile(file);
        }

        public File addFile(String name, byte[] content) {
            File file = new File(name, content);
            return this.addFile(file);
        }

        public File addFile(File file) {
            file.name = this.getUniqueName(file.name);
            this.files.put(file.name, file);
            return file;
        }

        public void append(Page reportPage) {
            this.messages.addAll(reportPage.messages);
            reportPage.files.forEach((name, file) -> {
                this.addFile(file);
            });
        }

        private String getUniqueName(String name) {
            if (!this.files.containsKey(name)) {
                return name;
            }
            FileName fn = new FileName(name);
            FileName fnTest = fn.clone();
            for (int j = 0; j < 1000; j++) {
                fnTest.name = fn.name + " (" + j + ")";
                String testName = fnTest.toString();
                if (!this.files.containsKey(testName)) {
                    return name;
                }
            }
            throw new RuntimeException("Can't getUniqueName(" + name + ")");
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            for (Entry e : this.messages) {
                result.append("[");
                result.append(DateHelper.formatHms(e.timestamp));
                result.append("] ");
                result.append(e.message);
                result.append("\n");
            }
            return result.toString();
        }

        public static class Entry {
            public long timestamp;
            public String message;
        }

        public static class File {
            public String name;
            protected String strContent;
            protected byte[] contentBytes;

            public File(String name, String content) {
                this.name = name;
                this.strContent = content;
            }

            public File(String name, byte[] content) {
                this.name = name;
                this.contentBytes = content;
            }

            public String getContentString() {
                if (this.strContent == null) {
                    ByteArrayInputStream stream = new ByteArrayInputStream(this.contentBytes);
                    try {
                        return StringHelper.readStream(stream);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return this.strContent;
                }
            }

            public byte[] getContentBytes() {
                return (this.contentBytes == null) ? this.strContent.getBytes() : this.contentBytes;
            }
        }
    }
}
