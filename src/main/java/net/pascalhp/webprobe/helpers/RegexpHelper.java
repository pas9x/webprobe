package net.pascalhp.webprobe.helpers;

import net.pascalhp.webprobe.exceptions.BugException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpHelper {
    public static final int CACHE_SIZE_LIMIT = 10000;
    public static int DEFAULT_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;

    protected static HashMap<String, Pattern> cache = new HashMap<>();

    protected static Pattern getPattren(String regexp, int flags) {
        String cacheKey = regexp + "_" + flags;
        Pattern result = cache.get(cacheKey);
        if (result == null) {
            if (cache.size() >= CACHE_SIZE_LIMIT) {
                throw new BugException("Too big regexp cache size. Limit = " + CACHE_SIZE_LIMIT);
            }
            result = Pattern.compile(regexp, flags);
            cache.put(regexp, result);
        }
        return result;
    }

    public static LinkedList<String> match(String regexp, String str) {
        return match(regexp, str, DEFAULT_FLAGS);
    }

    public static LinkedList<String> match(String regexp, String str, int flags) {
        Pattern pattern = getPattren(regexp, flags);
        Matcher matcher = pattern.matcher(str);
        LinkedList<String> result = new LinkedList<>();
        if (matcher.find()) {
            result.add(matcher.group(0));
            for (int group = 1; group < matcher.groupCount() + 1; group++) {
                result.add(matcher.group(group));
            }
        }
        return result;
    }

    public static String replaceCallback(String str, String regexp, int flags, ReplaceCallback callback) {
        Pattern pattern = getPattren(regexp, flags);
        Matcher matcher = pattern.matcher(str);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            LinkedList<String> matches = new LinkedList<>();
            for (int j = matcher.groupCount(); j >=0 ; j--) {
                matches.push(matcher.group(j));
            }
            String replacement = callback.replace(matches);
            replacement = Matcher.quoteReplacement(replacement);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String replaceCallback(String str, String regexp, ReplaceCallback callback) {
        return replaceCallback(str, regexp, DEFAULT_FLAGS, callback);
    }

    @FunctionalInterface
    public static interface ReplaceCallback {
        public String replace(List<String> matches);
    }
}
