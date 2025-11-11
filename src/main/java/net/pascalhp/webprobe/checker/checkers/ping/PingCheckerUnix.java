package net.pascalhp.webprobe.checker.checkers.ping;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;
import net.pascalhp.webprobe.helpers.RegexpHelper;
import net.pascalhp.webprobe.helpers.StringHelper;

import java.util.LinkedList;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

class PingCheckerUnix implements Checker {
    public final String host;
    public final int count;
    protected Process proc;
    protected String stopReason;

    public PingCheckerUnix(String host, int count) {
        this.host = host;
        this.count = count;
    }

    public CheckResult check() {
        LinkedList<String> cmd = new LinkedList<>();
        cmd.add("ping");
        cmd.add("-c");
        cmd.add(Integer.toString(this.count));
        cmd.add(this.host);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        builder.environment().put("LANG", "en_US");
        try {
            this.proc = builder.start();
        } catch (Throwable e) {
            PingResult result = new PingResult(this.getName());
            result.success = false;
            result.error = e;
            result.reportPage.log(lang("check.ping.start_failed", Map.of("error", ErrorHandler.formatException(e))));
            return result;
        }

        StringBuilder pingOutput = new StringBuilder();
        try {
            StringHelper.readStreamTo(proc.getInputStream(), pingOutput);
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }

        int pingExitCode = -2;
        try {
            pingExitCode = proc.waitFor();
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }

        return this.formatCheckResult(pingExitCode, pingOutput.toString());
    }

    protected PingResult formatCheckResult(int pingExitCode, String pingOutput) {
        PingResult result = new PingResult(this.getName());
        result.pingOutput = pingOutput;
        result.exitCode = pingExitCode;
        result.reportPage.log(lang("check.ping.output", Map.of("exitCode", String.valueOf(pingExitCode), "output", pingOutput)));

        if (this.stopReason == null) {
            LinkedList<String> matches = RegexpHelper.match("\\s(\\d{1,3}(\\.\\d{1,8})?)\\% packet loss", pingOutput);
            if (matches.size() > 1 && pingExitCode == 0) {
                float packetLoss = StringHelper.floatval(matches.get(1));
                result.success = (packetLoss == 0);
            }
        } else {
            result.reportPage.log(lang("check.stop_reason", Map.of("reason", this.stopReason)));
            result.success = false;
            return result;
        }

        return result;
    }

    public void stop(String reason) {
        this.stopReason = reason;
        if (this.proc == null) {
            return;
        }
        try {
            this.proc.destroyForcibly();
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }
    }

    public String getName() {
        return "Ping";
    }
}
