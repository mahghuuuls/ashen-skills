package com.mahghuuuls.ashenskills.client.animation.clip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a clip file. The format is described in {@code docs/clip-format.md} of the repository.
 * Every problem is reported with the bone and key it is in; nothing is guessed or repaired.
 */
public final class ClipParser {

    private static final String LENGTH = "length";
    private static final String BONES = "bones";
    private static final String ROTATION = "rotation";
    private static final String POSITION = "position";
    private static final String TIME = "t";
    private static final String VALUE = "v";
    private static final String EASE = "ease";

    private ClipParser() {
    }

    public static Clip parse(String json) throws ClipFormatException {
        JsonObject root;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) {
                throw new ClipFormatException("the file is not a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ClipFormatException("the file is not valid JSON: " + e.getMessage());
        }
        double length = number(root.get(LENGTH), "'" + LENGTH + "'");
        if (length <= 0) {
            throw new ClipFormatException("'" + LENGTH + "' must be above 0");
        }
        if (!root.has(BONES) || !root.get(BONES).isJsonObject()) {
            throw new ClipFormatException("'" + BONES + "' is missing or is not an object");
        }
        Map<Bone, Track> rotations = new EnumMap<>(Bone.class);
        Map<Bone, Track> positions = new EnumMap<>(Bone.class);
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(BONES).entrySet()) {
            Bone bone = Bone.byFileName(entry.getKey());
            if (bone == null) {
                throw new ClipFormatException("unknown bone '" + entry.getKey() + "'");
            }
            if (!entry.getValue().isJsonObject()) {
                throw new ClipFormatException("bone '" + entry.getKey() + "' is not an object");
            }
            JsonObject tracks = entry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> track : tracks.entrySet()) {
                String where = entry.getKey() + "." + track.getKey();
                if (ROTATION.equals(track.getKey())) {
                    rotations.put(bone, track(track.getValue(), where, length));
                } else if (POSITION.equals(track.getKey())) {
                    positions.put(bone, track(track.getValue(), where, length));
                } else {
                    throw new ClipFormatException("unknown track '" + where + "'");
                }
            }
        }
        if (rotations.isEmpty() && positions.isEmpty()) {
            throw new ClipFormatException("the clip moves no bone");
        }
        return new Clip(length, rotations, positions);
    }

    private static Track track(JsonElement element, String where, double length) throws ClipFormatException {
        if (!element.isJsonArray() || element.getAsJsonArray().size() == 0) {
            throw new ClipFormatException(where + " must be a list with at least one key");
        }
        List<Track.Key> keys = new ArrayList<>();
        double previous = -1;
        int index = 0;
        for (JsonElement keyElement : element.getAsJsonArray()) {
            String keyWhere = where + "[" + index++ + "]";
            if (!keyElement.isJsonObject()) {
                throw new ClipFormatException(keyWhere + " is not an object");
            }
            JsonObject key = keyElement.getAsJsonObject();
            double seconds = number(key.get(TIME), keyWhere + "." + TIME);
            if (seconds < 0 || seconds > length) {
                throw new ClipFormatException(keyWhere + "." + TIME + " is outside 0 to the clip length");
            }
            if (seconds <= previous) {
                throw new ClipFormatException(keyWhere + "." + TIME + " is not after the key before it");
            }
            previous = seconds;
            JsonElement value = key.get(VALUE);
            if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != 3) {
                throw new ClipFormatException(keyWhere + "." + VALUE + " must be a list of three numbers");
            }
            JsonArray xyz = value.getAsJsonArray();
            Easing easing = Easing.LINEAR;
            if (key.has(EASE)) {
                JsonElement easeElement = key.get(EASE);
                String name = easeElement.isJsonPrimitive() ? easeElement.getAsString() : "";
                easing = Easing.byFileName(name);
                if (easing == null) {
                    throw new ClipFormatException(keyWhere + "." + EASE + " has unknown easing '" + name + "'");
                }
            }
            keys.add(new Track.Key(
                    seconds,
                    number(xyz.get(0), keyWhere + "." + VALUE),
                    number(xyz.get(1), keyWhere + "." + VALUE),
                    number(xyz.get(2), keyWhere + "." + VALUE),
                    easing));
        }
        return new Track(keys);
    }

    private static double number(JsonElement element, String where) throws ClipFormatException {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new ClipFormatException(where + " is missing or is not a number");
        }
        double value = element.getAsDouble();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ClipFormatException(where + " is not a finite number");
        }
        return value;
    }
}
