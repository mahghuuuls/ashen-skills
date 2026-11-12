package com.mahghuuuls.ashenskills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
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
