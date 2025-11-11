package net.pascalhp.webprobe;

import net.pascalhp.webprobe.exceptions.BugException;
import net.pascalhp.webprobe.exceptions.NotFoundException;
import net.pascalhp.webprobe.helpers.MiscHelper;
import net.pascalhp.webprobe.helpers.RegexpHelper;
import net.pascalhp.webprobe.helpers.StringHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

public class Localization {
    public static final String DEFAULT_LANGUAGE_CODE = "en";

    protected static Localization currentLanguage;
    protected static List<String> allLanguageCodes;
    protected static Map<String, Localization> allLanguages;

    public final String code;
    public final String name;
    public final String nameEn;
    public final String menuitem;
    protected Properties translationTable;

    static {
        try {
            init();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load localization system", e);
        }
    }

    public Localization(Properties translationTable) {
        this.translationTable = translationTable;
        this.code = this.get("language_code");
        this.name = this.get("language_name");
        this.nameEn = this.get("language_name_en");
        this.menuitem = this.get("language_menuitem");
    }

    protected static void init() throws IOException, NotFoundException {
        allLanguageCodes = loadAllLanguageCodes();
        allLanguages = loadAllLanguages();
    }

    public static Localization autodetectLanguage() throws IOException {
        Locale systemLocale = Locale.getDefault();
        if (systemLocale == null) {
            return getDefaultLanguage();
        }
        String systemLanguageCode = systemLocale.getLanguage();
        try {
            return Localization.getLanguageByCode(systemLanguageCode);
        } catch (NotFoundException e) {
            return getDefaultLanguage();
        }
    }

    public String get(String key) {
        if (key == null) {
            throw new NullPointerException("Localization key == null");
        }
        String template = this.translationTable.getProperty(key);

        if (template == null) {
            throw new BugException("No such localization key: " + key);
        }
        return template;
    }

    public String get(String key, Map<String, String> values) {
        String template = this.get(key);
        if (values == null) {
            return template;
        }
        Replacer replacer = new Replacer(values);
        return RegexpHelper.replaceCallback(template, "\\{([a-z_]{2,50})\\}", replacer);
    }

    public static String lang(String key) {
        return getInstance().get(key);
    }

    public static String lang(String key, Map<String, String> values) {
        return getInstance().get(key, values);
    }

    public static Localization getDefaultLanguage() throws IOException {
        try {
            return getLanguageByCode(DEFAULT_LANGUAGE_CODE);
        } catch (NotFoundException e) {
            throw new BugException("Default language `" + DEFAULT_LANGUAGE_CODE + "` not found", e);
        }
    }

    public static Localization getInstance() {
        if (currentLanguage == null) {
            try {
                currentLanguage = autodetectLanguage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return currentLanguage;
    }

    public static void setInstance(Localization localization) {
        if (localization == null) {
            throw new NullPointerException("localization == null");
        }
        currentLanguage = localization;
    }

    public static void switchTo(String code) throws NotFoundException {
        Localization loc = getLanguageByCode(code);
        setInstance(loc);
    }

    protected static Localization loadLanguageByCode(String code) throws NotFoundException, IOException {
        validateLanguageCode(code);
        String langFilePath = "lang/lang_" + code + ".properties";
        InputStream stream = MiscHelper.getResourceStream(langFilePath);
        if (stream == null) {
            throw new NotFoundException("Language code `" + code+ "` not found");
        }
        Properties table = new Properties();
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        table.load(reader);
        return new Localization(table);
    }

    protected static List<String> loadAllLanguageCodes() throws IOException {
        InputStream listStream = MiscHelper.getResourceStream("lang/list.txt");
        if (listStream == null) {
            throw new BugException("Unable to load language list: file lang/list.txt not found");
        }
        List<String> codes = StringHelper.readStreamLines(listStream);

        LinkedList<String> result = new LinkedList<>();
        for (String code : codes) {
            if (checkLanguageCode(code)) {
                result.add(code);
            }
        }

        return result;
    }

    protected static Map<String, Localization> loadAllLanguages() throws IOException, NotFoundException {
        HashMap<String, Localization> result = new HashMap<>();
        for (String code : getAllLanguageCodes()) {
            Localization loc = Localization.loadLanguageByCode(code);
            result.put(code, loc);
        }
        return result;
    }

    public static List<String> getAllLanguageCodes() {
        return allLanguageCodes;
    }

    public static Map<String, Localization> getAllLanguages() {
        return allLanguages;
    }

    public static Localization getLanguageByCode(String code) throws NotFoundException {
        validateLanguageCode(code);
        Localization lan = getAllLanguages().get(code);
        if (lan == null) {
            throw new NotFoundException("Language `" + code + "` not found");
        }
        return lan;
    }

    private static boolean checkLanguageCode(String code) {
        if (code == null) {
            return false;
        }
        return code.matches("^[a-z]{2}$");
    }

    protected static void validateLanguageCode(String code) {
        if (code == null) {
            throw new NullPointerException("Language code == null");
        }
        if (!checkLanguageCode(code)) {
            throw new IllegalArgumentException("Invalid language code");
        }
    }

    private static class Replacer implements RegexpHelper.ReplaceCallback {
        public Map<String, String> values;

        public Replacer(Map<String, String> values) {
            this.values = values;
        }

        public String replace(List<String> matches) {
            String match = matches.get(0);
            String key = matches.get(1);
            if (match == null || key == null) {
                throw new BugException("key == null");
            }
            String value = this.values.get(key);
            return (value == null) ? match : value;
        }
    }

    public Bundle getBundle() {
        return new Bundle();
    }

    public LangPreview getPreview() {
        return new LangPreview(this.name, this.nameEn, this.menuitem, this.code);
    }

    public static Map<String, LangPreview> getLangPreviews() {
        HashMap<String, LangPreview> result = new HashMap<>();
        for (Localization loc : getAllLanguages().values()) {
            result.put(loc.code, loc.getPreview());
        }
        return result;
    }

    public class Bundle extends ResourceBundle {
        protected Object handleGetObject(String s) {
            return get(s);
        }

        public Enumeration<String> getKeys() {
            Iterator<Object> it = translationTable.keys().asIterator();
            LinkedList<String> keysList = new LinkedList<>();
            while (it.hasNext()) {
                keysList.add(it.next().toString());
            }
            return Collections.enumeration(keysList);
        }
    }

    public static class LangPreview {
        public final String name;
        public final String nameEn;
        public final String menuitem;
        public final String code;

        public LangPreview(String name, String nameEn, String menuitem, String code) {
            this.name = name;
            this.nameEn = nameEn;
            this.menuitem = menuitem;
            this.code = code;
        }
    }
}
