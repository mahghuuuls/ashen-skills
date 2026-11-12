package com.mahghuuuls.ashenskills.config;

import com.mahghuuuls.ashenskills.config.DefaultsData.GeneralEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The general settings in effect, after validation. Immutable. A value that is not valid is never
 * applied in part: the default is used and a problem is recorded for the log.
 */
public final class GeneralSettings {

    private final Map<SettingKey, Object> values;
    private final List<String> problems;

    private GeneralSettings(Map<SettingKey, Object> values, List<String> problems) {
        this.values = values;
        this.problems = Collections.unmodifiableList(problems);
    }

    /**
     * @param rawValues text of each setting as written in the configuration file; a missing key
     *     means the default
     */
    public static GeneralSettings resolve(DefaultsData defaults, Map<SettingKey, String> rawValues) {
        Map<SettingKey, Object> values = new EnumMap<>(SettingKey.class);
        List<String> problems = new ArrayList<>();
        for (SettingKey key : SettingKey.values()) {
            if (!key.isInConfigurationFile()) {
                continue;
            }
            GeneralEntry entry = defaults.general(key.jsonName());
            Object fallback = defaultOf(key, entry);
            String raw = rawValues.get(key);
            Object parsed = raw == null ? fallback : parse(key, entry, raw.trim());
            if (parsed == null) {
                problems.add("setting '" + key.jsonName() + "' has invalid value '" + raw + "'" + rangeText(key, entry)
                        + "; using default " + fallback);
                parsed = fallback;
            }
            values.put(key, parsed);
        }
        return new GeneralSettings(values, problems);
    }

    private static Object defaultOf(SettingKey key, GeneralEntry entry) {
        switch (key.kind()) {
            case BOOLEAN:
                return entry.defaultValue().getAsBoolean();
            case INTEGER:
                return entry.defaultValue().getAsInt();
            case DECIMAL:
                return entry.defaultValue().getAsDouble();
            default:
                throw new IllegalArgumentException(key + " is not stored in the configuration file");
        }
    }

    /** Returns null when the text is not a valid value for the setting. */
    private static Object parse(SettingKey key, GeneralEntry entry, String raw) {
        try {
            switch (key.kind()) {
                case BOOLEAN:
                    if (raw.equalsIgnoreCase("true")) {
                        return Boolean.TRUE;
                    }
                    return raw.equalsIgnoreCase("false") ? Boolean.FALSE : null;
                case INTEGER:
                    int whole = Integer.parseInt(raw);
                    return entry.allows(whole) ? Integer.valueOf(whole) : null;
                case DECIMAL:
                    double decimal = Double.parseDouble(raw);
                    return !Double.isNaN(decimal) && entry.allows(decimal) ? Double.valueOf(decimal) : null;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String rangeText(SettingKey key, GeneralEntry entry) {
        if (key.kind() == SettingKey.Kind.BOOLEAN) {
            return " (allowed: true or false)";
        }
        if (key.kind() == SettingKey.Kind.INTEGER) {
            return " (allowed: whole number from " + entry.min().intValue() + " to " + entry.max().intValue() + ")";
        }
        return " (allowed: " + entry.min() + " to " + entry.max() + ")";
    }

    public boolean getBoolean(SettingKey key) {
        return (Boolean) values.get(key);
    }

    public int getInt(SettingKey key) {
        return (Integer) values.get(key);
    }

    public double getDouble(SettingKey key) {
        return (Double) values.get(key);
    }

    /** One line per rejected value, naming the setting and the value. Empty when all were valid. */
    public List<String> problems() {
        return problems;
    }
}
