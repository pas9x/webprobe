package net.pascalhp.webprobe.checker;

import net.pascalhp.webprobe.Report;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.io.StringWriter;

public class CheckResult {
    public long startTime = 0;
    public long exitTime = 0;
    public Boolean success = null;
    public Report.Page reportPage;

    public CheckResult(String reportPageTitle) {
        this.reportPage = new Report.Page(reportPageTitle);
    }

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        StringWriter buf = new StringWriter();
        try {
            writer.writeValue(buf, this);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return buf.toString();
    }
}
