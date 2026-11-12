package com.mahghuuuls.ashenskills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The bundled defaults file: the single source of every default number, range and owning side.
 * Nothing else in the mod states a default.
 */
public final class DefaultsData {

    public static final String RESOURCE_PATH = "/ashenskills/skill-defaults.json";

    private final JsonObject root;

    private DefaultsData(JsonObject root) {
        this.root = root;
    }

    public static DefaultsData loadBundled() {
        try (InputStream in = DefaultsData.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled defaults file " + RESOURCE_PATH);
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bundled defaults file " + RESOURCE_PATH, e);
        }
    }

    public static DefaultsData parse(Reader json) {
        return new DefaultsData(new JsonParser().parse(json).getAsJsonObject());
    }

    public Set<String> generalNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("general").entrySet()) {
            names.add(entry.getKey());
        }
        return names;
    }

    /** @throws IllegalArgumentException when the file has no general setting of that name */
    public GeneralEntry general(String name) {
        JsonObject general = root.getAsJsonObject("general");
        if (!general.has(name)) {
            throw new IllegalArgumentException("Defaults file has no general setting '" + name + "'");
        }
        JsonObject entry = general.getAsJsonObject(name);
        return new GeneralEntry(
                entry.getAsJsonPrimitive("default"),
                entry.has("min") ? entry.get("min").getAsDouble() : null,
                entry.has("max") ? entry.get("max").getAsDouble() : null,
                entry.get("owner").getAsString());
    }

    /** @throws IllegalArgumentException when the file has no skill of that name */
    public SkillEntry skill(String name) {
        JsonObject skills = root.getAsJsonObject("skills");
        if (!skills.has(name)) {
            throw new IllegalArgumentException("Defaults file has no skill '" + name + "'");
        }
        return new SkillEntry(skills.getAsJsonObject(name), section("commonSkillRules"), section("enchantmentDefaults"));
    }

    private JsonObject section(String name) {
        return root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
    }

    /** Item registry name to the weapon types that item has without any pack configuration. */
    public Map<String, Set<String>> builtInItemTypes() {
        Map<String, Set<String>> types = new LinkedHashMap<>();
        JsonObject items = root.getAsJsonObject("builtInWeaponTypes").getAsJsonObject("vanillaItemTypes");
        for (Map.Entry<String, JsonElement> item : items.entrySet()) {
            types.put(item.getKey(), strings(item.getValue().getAsJsonArray()));
        }
        return Collections.unmodifiableMap(types);
    }

    private static Set<String> strings(JsonArray array) {
        Set<String> values = new LinkedHashSet<>();
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return Collections.unmodifiableSet(values);
    }

    /**
     * One skill as the defaults file states it. A value the skill does not state itself is taken
     * from the file's rules common to all skills, and then from its enchantment defaults.
     */
    public static final class SkillEntry {

        private final JsonObject[] sources;

        SkillEntry(JsonObject own, JsonObject common, JsonObject enchantmentDefaults) {
            this.sources = new JsonObject[] {own, common, enchantmentDefaults};
        }

        /** "instant", "stance" or "charged". */
        public String activation() {
            return sources[0].get("activation").getAsString();
        }

        public Set<String> weaponTypes() {
            return strings(sources[0].getAsJsonArray("weaponTypes"));
        }

        /** The player states in which the skill may start; empty when the file states none. */
        public Set<String> allowedStartStates() {
            JsonElement states = find("allowedStartStates");
            return states == null ? Collections.<String>emptySet() : strings(states.getAsJsonArray());
        }

        /** The number of that name, or null when the file has none for this skill. */
        public Double number(String name) {
            JsonElement value = find(name);
            return value == null ? null : value.getAsDouble();
        }

        /** The yes-or-no rule of that name, or null when the file has none for this skill. */
        public Boolean flag(String name) {
            JsonElement value = find(name);
            return value == null ? null : value.getAsBoolean();
        }

        /** When each hit window opens, in seconds from the start of execution; empty when none. */
        public List<Double> hitWindowSeconds() {
            List<Double> seconds = new ArrayList<>();
            JsonElement windows = find("hitWindows");
            if (windows != null) {
                for (JsonElement window : windows.getAsJsonArray()) {
                    seconds.add(window.getAsJsonObject().get("at").getAsDouble());
                }
            }
            return seconds;
        }

        private JsonElement find(String name) {
            for (JsonObject source : sources) {
                if (source.has(name)) {
                    return source.get(name);
                }
            }
            return null;
        }
    }

    /** One general setting as the defaults file states it. */
    public static final class GeneralEntry {

        private final JsonPrimitive defaultValue;
        private final Double min;
        private final Double max;
        private final String owner;

        GeneralEntry(JsonPrimitive defaultValue, Double min, Double max, String owner) {
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.owner = owner;
        }

        public JsonPrimitive defaultValue() {
            return defaultValue;
        }

        /** Null when the setting has no lower limit. */
        public Double min() {
            return min;
        }

        /** Null when the setting has no upper limit. */
        public Double max() {
            return max;
        }

        /** "server" or "client": who decides this value. Also the category in the configuration file. */
        public String owner() {
            return owner;
        }

        public boolean allows(double value) {
            return (min == null || value >= min) && (max == null || value <= max);
        }
    }
}
