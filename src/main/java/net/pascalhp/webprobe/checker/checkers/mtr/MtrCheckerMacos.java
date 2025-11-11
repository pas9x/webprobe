package net.pascalhp.webprobe.checker.checkers.mtr;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;
import net.pascalhp.webprobe.helpers.StringHelper;
import net.pascalhp.webprobe.helpers.SystemHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

class MtrCheckerMacos implements Checker {
    public final String basedir;
    public final String mtrPath;
    public final String host;
    public final int count;
    protected Process proc;
    protected String stopReason;

    public MtrCheckerMacos(String basedir, String host, int count) {
        this.basedir = basedir;
        this.host = host;
        this.count = count;
        this.mtrPath = this.findMtrPath(basedir);
    }

    public static String findMtrPath(String basedir) {
        String[] searchPaths = new String[] {
            basedir + "/../Frameworks/mtr",
            "/usr/local/Cellar/mtr/0.96/sbin/mtr",
        };

        for (String path : searchPaths) {
            File mtrFile = new File(path);
            if (mtrFile.isFile()) {
                return path;
            }
        }

        SystemHelper.ExecResult execResult;
        try {
            execResult = SystemHelper.exec("which mtr");
        } catch (IOException|InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (execResult.exitCode == 0) {
            String mtrPath = execResult.stdoutString().trim();
            if (!mtrPath.isEmpty()) {
                File mtrFile = new File(mtrPath);
                if (mtrFile.isFile()) {
                    return mtrPath;
                }
            }
        }

        throw new RuntimeException("MTR binary file not found");
    }

    public CheckResult check() {
        String script = "do shell script \"" +
            this.mtrPath + " --report-wide --report-cycles " + this.count + " " + this.host
            + "\" with administrator privileges\n";

        ProcessBuilder builder = new ProcessBuilder("osascript");
        builder.redirectErrorStream(true);
        builder.environment().put("LANG", "en_US");

        MtrCheckResult result = new MtrCheckResult(this.getName());

        try {
            this.proc = builder.start();
            OutputStream procStdin = this.proc.getOutputStream();
            procStdin.write(script.getBytes());
            procStdin.close();
        } catch (Throwable e) {
            result.success = false;
            result.error = e;
            result.reportPage.log(lang("check.mtr.start_failed", Map.of("error", ErrorHandler.formatException(e))));
            return result;
        }

        StringBuilder mtrOutput = new StringBuilder();
        try {
            InputStream procStdout = proc.getInputStream();
            StringHelper.readStreamTo(procStdout, mtrOutput);
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }

        int exitCode = -2;
        try {
            exitCode = proc.waitFor();
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }

        result.exitCode = exitCode;
        result.mtrOutput = mtrOutput.toString();
        result.reportPage.log(lang("check.mtr.output", Map.of("exitCode", String.valueOf(exitCode), "output", result.mtrOutput)));

        if (this.stopReason != null) {
            result.reportPage.log(lang("check.stop_reason", Map.of("reason", this.stopReason)));
            result.success = false;
        }

        return result;
    }

    public void stop(String reason) {
        this.stopReason = reason;
        if (this.proc != null) {
            try {this.proc.destroyForcibly();}
            catch (Throwable e) {ErrorHandler.logException(e);}
        }
    }

    public String getName() {
        return "MTR";
    }
}
