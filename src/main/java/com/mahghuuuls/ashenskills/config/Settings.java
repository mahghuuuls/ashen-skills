package com.mahghuuuls.ashenskills.config;

import com.mahghuuuls.ashenskills.config.DefaultsData.GeneralEntry;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.apache.logging.log4j.Logger;

/** Entry point to the mod's configuration. Read once at start; changes need a restart. */
public final class Settings {

    private static DefaultsData defaults;
    private static GeneralSettings general;

    private Settings() {
    }

    /** Reads (and creates when missing) the general configuration file and reports invalid values. */
    public static void load(File configurationFile, Logger logger) {
        defaults = DefaultsData.loadBundled();
        Configuration file = new Configuration(configurationFile);
        file.load();
        Map<SettingKey, String> raw = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            if (!key.isInConfigurationFile()) {
                continue;
            }
            GeneralEntry entry = defaults.general(key.jsonName());
            // Forge replaces text of the wrong type with the default as soon as a typed property is
            // requested, without a word. Read what the user wrote first, so it can be reported.
            String written = textAsWritten(file, entry.owner(), key.jsonName());
            Property property = property(file, key, entry);
            raw.put(key, written != null ? written : property.getString());
        }
        if (file.hasChanged()) {
            file.save();
        }
        general = GeneralSettings.resolve(defaults, raw);
        for (String problem : general.problems()) {
            logger.error("Configuration file {}: {}", configurationFile.getName(), problem);
        }
    }

    /** The setting's text exactly as the file holds it, or null when the file does not have it yet. */
    private static String textAsWritten(Configuration file, String category, String name) {
        return file.hasKey(category, name) ? file.getCategory(category).get(name).getString() : null;
    }

    private static Property property(Configuration file, SettingKey key, GeneralEntry entry) {
        String category = entry.owner();
        String name = key.jsonName();
        switch (key.kind()) {
            case BOOLEAN:
                return file.get(category, name, entry.defaultValue().getAsBoolean(),
                        key.comment() + " Default: " + entry.defaultValue().getAsBoolean() + ".");
            case INTEGER:
                return file.get(category, name, entry.defaultValue().getAsInt(),
                        key.comment() + " Allowed: " + entry.min().intValue() + " to " + entry.max().intValue()
                                + ". Default: " + entry.defaultValue().getAsInt() + ".",
                        entry.min().intValue(), entry.max().intValue());
            case DECIMAL:
                return file.get(category, name, entry.defaultValue().getAsDouble(),
                        key.comment() + " Allowed: " + entry.min() + " to " + entry.max()
                                + ". Default: " + entry.defaultValue().getAsDouble() + ".",
                        entry.min(), entry.max());
            default:
                throw new IllegalArgumentException(key + " is not stored in the configuration file");
        }
    }

    public static GeneralSettings general() {
        return general;
    }

    public static DefaultsData defaults() {
        return defaults;
    }
}
