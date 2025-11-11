package net.pascalhp.webprobe.checker;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.Event;
import net.pascalhp.webprobe.EventRouter;
import net.pascalhp.webprobe.OSType;
import net.pascalhp.webprobe.Report;
import net.pascalhp.webprobe.WaitGroup;
import net.pascalhp.webprobe.checker.checkers.dnsjava.DnsjavaChecker;
import net.pascalhp.webprobe.checker.checkers.gethostbyname.GethostbynameChecker;
import net.pascalhp.webprobe.checker.checkers.http.HttpCheckResult;
import net.pascalhp.webprobe.checker.checkers.http.HttpChecker;
import net.pascalhp.webprobe.checker.checkers.jndi_dns.JndiDnsChecker;
import net.pascalhp.webprobe.checker.checkers.mtr.MtrChecker;
import net.pascalhp.webprobe.checker.checkers.ping.PingChecker;
import net.pascalhp.webprobe.checker.events.OnCheckExit;
import net.pascalhp.webprobe.checker.events.OnCheckStart;
import net.pascalhp.webprobe.checker.events.OnExecutorExit;
import net.pascalhp.webprobe.checker.events.OnExecutorStart;
import net.pascalhp.webprobe.helpers.StringHelper;
import net.pascalhp.webprobe.tasks.TaskExitEvent;
import net.pascalhp.webprobe.tasks.TaskManager;
import net.pascalhp.webprobe.tasks.TaskResult;
import net.pascalhp.webprobe.tasks.TaskStartEvent;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static net.pascalhp.webprobe.Localization.lang;

public class CheckExecutor {
    public int pings;
    protected URL url;
    protected EventRouter events;
    protected OSType os;
    protected String basedir;
    protected long startTime;
    protected boolean started;
    protected boolean stopped;
    protected boolean exited;
    protected Report report;
    protected Report.Page reportPage;
    protected WaitGroup wg = new WaitGroup();
    protected TaskManager taskman;

    protected GethostbynameChecker gethostbynameChecker;
    protected JndiDnsChecker jndiChecker;
    protected DnsjavaChecker dnsjavaChecker;
    protected PingChecker pingChecker;
    protected MtrChecker mtrChecker;
    protected HttpChecker httpChecker;

    public CheckExecutor(URL url, OSType os, String basedir, int pings, EventRouter events) {
        this.url = url;
        this.events = events;
        this.os = os;
        this.basedir = basedir;
        this.pings = pings;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String reportTitle = df.format(System.currentTimeMillis()) + "_" + StringHelper.getReadableDomain(url.getHost());
        this.report = new Report(reportTitle);
        this.reportPage = new Report.Page("Summary");

        String rawDomain = StringHelper.getRawDomain(url.getHost());
        String readableDomain = StringHelper.getReadableDomain(url.getHost());
        this.reportPage.log(lang("check.main_page.domain_name", Map.of("domainName", rawDomain)));
        if (!readableDomain.equals(rawDomain)) {
            this.reportPage.log(lang("check.main_page.readable_domain_name", Map.of("readableDomainName", readableDomain)));
        }

        if (!rawDomain.matches("^[a-zA-Z0-9\\.\\-]+$")) {
            throw new IllegalArgumentException("Invalid domain name");
        }

        this.gethostbynameChecker = new GethostbynameChecker(rawDomain);
        this.jndiChecker = new JndiDnsChecker(rawDomain);
        // TODO: move resolvers to application settings.json
        this.dnsjavaChecker = new DnsjavaChecker(rawDomain, List.of("8.8.8.8", "1.1.1.1"));
        this.pingChecker = new PingChecker(this.os, rawDomain, this.pings);
        this.mtrChecker = new MtrChecker(this.os, this.basedir, rawDomain, this.pings);
        this.httpChecker = new HttpChecker(this.url);
    }

    public synchronized void start() {
        if (this.started) {
            throw new RuntimeException("CheckRunner has already been launched");
        }
        this.wg.enter();
        this.started = true;
        this.startTime = System.currentTimeMillis();
        Thread executionThread = new Thread(this::execute, "CheckExecutor-thread");
        executionThread.start();
    }

    public synchronized boolean stop(String reason) {
        if (!this.started) {
            return false;
        }
        if (this.stopped) {
            return false;
        }
        this.stopped = true;
        if (this.exited) {
            return false;
        }
        if (this.taskman != null) {
            try {
                this.taskman.stop(reason);
            } catch (Throwable e) {
                ErrorHandler.logException(e);
            }
        }
        return true;
    }

    public void waitExit() {
        if (!this.started) {
            throw new RuntimeException("CheckExecutor didn't started yet");
        }
        try {
            this.wg.waitAllExit();
        } catch (InterruptedException e) {
            this.stop("CheckExecutor.waitExit() thread has been interrupted");
        }
    }

    public boolean isExited() {
        return this.exited;
    }

    public Report getReport() {
        return this.exited ? this.report : null;
    }

    protected void execute() {
        this.events.pushEvent(new OnExecutorStart());

        EventRouter taskmanEvents = new EventRouter();
        taskmanEvents.addEventListener(this::onTaskEvent);
        this.taskman = new TaskManager(taskmanEvents);

        CheckerTask gethostbynameTask = new CheckerTask(this.gethostbynameChecker);
        taskman.addTask("gethostbyname", gethostbynameTask);

        CheckerTask jndiCheckerTask = new CheckerTask(this.jndiChecker);
        taskman.addTask("jndi_dns", jndiCheckerTask, 40);

        CheckerTask dnsjavaTask = new CheckerTask(this.dnsjavaChecker);
        taskman.addTask("dnsjava", dnsjavaTask);

        CheckerTask pingTask = new CheckerTask(this.pingChecker);
        taskman.addTask("ping", pingTask, 300);

        CheckerTask mtrTask = new CheckerTask(this.mtrChecker);
        taskman.addTask("mtr", mtrTask, 300);

        CheckerTask httpTask = new CheckerTask(this.httpChecker);
        taskman.addTask("http", httpTask, 30);

        taskman.start();
        taskman.waitAllExit();
        this.formatReport(taskman.getResults());

        this.events.pushEvent(new OnExecutorExit());
        this.exited = true;
        this.wg.exit();
    }

    protected void formatReport(Map<String, TaskResult> results) {
        this.appendReportMainPage();
        this.report.pages.add(this.reportPage);
        results.forEach((taskId, result) -> {
            if (result.isError) {
                Report.Page page = new Report.Page(lang("report.checker_error", Map.of("checkerId", taskId)));
                page.log(ErrorHandler.formatException(result.error));
                this.report.pages.add(page);
            } else {
                CheckResult checkResult = (CheckResult)result.result;
                report.pages.add(checkResult.reportPage);
            }
        });
    }

    protected void appendReportMainPage() {
        long duration = System.currentTimeMillis() - this.startTime;
        this.reportPage.log(lang("check.main_page.duration", Map.of("durationSec", String.valueOf(duration / 1000))));
        boolean httpSuccess = false;
        TaskResult taskResult = this.taskman.getResults().get("http");
        if (taskResult != null) {
            if (taskResult.result != null) {
                HttpCheckResult httpResult = (HttpCheckResult)taskResult.result;
                if (httpResult.success != null && httpResult.success) {
                    httpSuccess = true;
                }
            }
        }
        String statusMsg = httpSuccess ? lang("check.main_page.http_ok") : lang("check.main_page.http_error");
        this.reportPage.log(lang("check.main_page.http_status", Map.of("httpStatus", statusMsg)));
    }

    protected void onTaskEvent(Event event) {
        if (event instanceof TaskStartEvent) {
            TaskStartEvent taskEvent = (TaskStartEvent)event;
            CheckerTask checkerTask = (CheckerTask)taskEvent.task;
            OnCheckStart startEvent = new OnCheckStart(taskEvent.id, checkerTask.checker);
            this.events.pushEvent(startEvent);
            return;
        }

        if (event instanceof TaskExitEvent) {
            TaskExitEvent taskEvent = (TaskExitEvent)event;
            OnCheckExit exitEvent = new OnCheckExit(taskEvent.id, (CheckResult)taskEvent.result.result, taskEvent.result.error);
            this.events.pushEvent(exitEvent);
        }
    }
}
