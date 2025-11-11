package net.pascalhp.webprobe.checker.checkers.mtr;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;
import net.pascalhp.webprobe.helpers.StringHelper;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

class MtrCheckerWindows implements Checker {
    public final String basedir;
    public final String host;
    public final int count;
    protected Process proc;
    protected String stopReason;
    protected String mtrPath;

    public MtrCheckerWindows(String basedir, String host, int count) {
        this.basedir = basedir;
        this.host = host;
        this.count = count;
        this.mtrPath = basedir + "/mtr.exe";
        File exe = new File(this.mtrPath);
        if (!exe.isFile()) {
            throw new RuntimeException("MTR file " + this.mtrPath + " not found");
        }
    }

    public CheckResult check() {
        LinkedList<String> cmd = new LinkedList<>();
        cmd.add(this.mtrPath);
        cmd.add("-c");
        cmd.add(String.valueOf(this.count));
        cmd.add("--report");
        cmd.add("--wide");
        cmd.add(this.host);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);

        MtrCheckResult result = new MtrCheckResult(this.getName());

        try {
            this.proc = builder.start();
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
