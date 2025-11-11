package net.pascalhp.webprobe.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SystemHelper {
    public static final boolean isWindows;

    static {
        String os = System.getProperty("os.name");
        if (os == null) {
            isWindows = false;
        } else {
            isWindows = os.toLowerCase().matches("^win.*");
        }
    }

    public static ExecResult exec(String command) throws IOException, InterruptedException {
        return exec(command, (byte[])null);
    }

    public static ExecResult exec(String command, String stdin) throws IOException, InterruptedException {
        byte[] stdinBytes = (stdin == null) ? null : stdin.getBytes();
        return exec(command, stdinBytes);
    }

    public static ExecResult exec(String command, byte[] stdin) throws IOException, InterruptedException {
        ProcessBuilder builder;
        if (isWindows) {
            builder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            builder = new ProcessBuilder("sh", "-c", command);
        }

        Process proc = builder.start();
        if (stdin != null) {
            OutputStream stdinStream = proc.getOutputStream();
            stdinStream.write(stdin);
            stdinStream.close();
        }

        InputStream stdoutStream = proc.getInputStream();
        InputStream stderrStream = proc.getErrorStream();
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();

        Runnable stdoutReader = () -> {
            try {
                stdoutStream.transferTo(stdoutBuffer);
            } catch (IOException e) {
            }
        };

        Runnable stderrReader = () -> {
            try {
                stderrStream.transferTo(stderrBuffer);
            } catch (IOException e) {
            }
        };

        Thread stdoutReaderThread = new Thread(stdoutReader);
        stdoutReaderThread.start();
        Thread stderrReaderThread = new Thread(stderrReader);
        stderrReaderThread.start();

        int exitCode = proc.waitFor();
        stdoutReaderThread.join();
        stderrReaderThread.join();

        stdoutStream.close();
        stderrStream.close();

        return new ExecResult(exitCode, stdoutBuffer.toByteArray(), stderrBuffer.toByteArray());
    }

    public static class ExecResult {
        public final int exitCode;
        public final byte[] stdout;
        public final byte[] stderr;

        public ExecResult(int exitCode, byte[] stdout, byte[] stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String stdoutString() {
            return new String(this.stdout, StandardCharsets.UTF_8);
        }

        public String stderrString() {
            return new String(this.stderr, StandardCharsets.UTF_8);
        }

        public String toString() {
            return "Process exitCode: " + this.exitCode
                    + ", stdoutLength: " + this.stdout.length
                    + ", stderrLength: " + this.stderr.length;
        }
    }
}
