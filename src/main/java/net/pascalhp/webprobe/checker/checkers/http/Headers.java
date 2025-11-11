package net.pascalhp.webprobe.checker.checkers.http;

import net.pascalhp.webprobe.helpers.StringHelper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Headers extends HashMap<String, List<String>> {
    public void addHeader(String name, String value) {
        String headerL = name.toLowerCase().trim();
        if (headerL.isEmpty()) {
            return;
        }
        List<String> headerSet = this.get(headerL);
        if (headerSet == null) {
            headerSet = new LinkedList<>();
            this.put(headerL, headerSet);
        }
        headerSet.add(value);
    }

    public void setHeader(String name, String value) {
        String headerL = name.toLowerCase();
        this.put(headerL, List.of(value));
    }

    public String getOneHeader(String name) {
        List<String> values = this.get(name.toLowerCase());
        if (values == null) {
            return null;
        }
        return (values.size() == 1) ? values.get(0) : null;
    }

    public void addRawHeader(String header) {
        String[] pieces = header.split(":", 2);
        String name = pieces[0];
        String value = (pieces.length > 1) ? pieces[1].trim() : "";
        this.addHeader(name, value);
    }

    public static String normalizeHeader(String header) {
        return StringHelper.ucwords(header, "-");
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        this.forEach((name, values) -> {
            String normalizedHeader = normalizeHeader(name);
            for (String value : values) {
                result.append(normalizedHeader);
                result.append(": ");
                result.append(value);
                result.append("\n");
            }
        });
        return result.toString();
    }
}
