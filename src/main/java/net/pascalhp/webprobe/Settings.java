package net.pascalhp.webprobe;

import net.pascalhp.webprobe.dto.SettingsData;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.FileNotFoundException;

public class Settings {
    public String lang;
    public int pings = 20;

    protected static Settings loadFromDto(SettingsData data) {
        Settings cfg = new Settings();
        cfg.lang = data.lang;
        cfg.pings = data.pings;
        return cfg;
    }

    public static Settings loadFromFile(String file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File f = new File(file);
        if (!f.isFile()) {
            throw new FileNotFoundException("Settings file " + file + " not found");
        }
        SettingsData data = mapper.readValue(f, SettingsData.class);
        Settings cfg = loadFromDto(data);
        return cfg;
    }

    protected SettingsData saveToDto() {
        SettingsData data = new SettingsData();
        data.lang = this.lang;
        data.pings = this.pings;
        return data;
    }

    public void saveToFile(String file) {
        SettingsData data = this.saveToDto();
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(file), data);
    }

    public static Settings getDefaultSettings() {
        Settings settings = new Settings();
        settings.lang = Localization.getInstance().code;
        return settings;
    }
}
