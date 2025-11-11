package net.pascalhp.webprobe.helpers;

import org.xbill.DNS.ZoneMDRecord;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.TimeZone;

import static net.pascalhp.webprobe.Localization.lang;

public class DateHelper {
    protected static HashMap<String, SimpleDateFormat> formats = new HashMap<>();

    public static long time() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String formatHms(long time) {
        SimpleDateFormat df = getFormat(lang("date.date_hms"));
        return df.format(time * 1000);
    }

    public static String formatHms() {
        return formatHms(time());
    }

    protected static SimpleDateFormat getFormat(String format) {
        SimpleDateFormat df = formats.get(format);
        if (df == null) {
            df = new SimpleDateFormat(format);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            formats.put(format, df);
        }
        return df;
    }
}
