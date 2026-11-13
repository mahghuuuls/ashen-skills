package com.mahghuuuls.ashenskills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

/**
 * Applies a pack's {@code skills.json} to the registered skills. The file names skills by id and,
 * for each, any of the values a skill has. Each value stands alone: a wrong one is reported and
 * the skill keeps its own value for it, while the other values of the same skill still apply.
 */
public final class SkillOverrides {

    private static final String WEAPON_TYPES_KEY = "weaponTypes";
    private static final String STATES_KEY = "allowedStartStates";
    private static final String HIT_WINDOWS_KEY = "hitWindows";
    private static final String HIT_WINDOW_TIME_KEY = "at";
    /** More cannot be told to a client; far above what the per-use limit of the server lets open anyway. */
    private static final int MOST_HIT_WINDOWS = 255;
    private static final Set<String> STATES = new LinkedHashSet<>(Arrays.asList(
            SkillDefinition.STATE_GROUNDED, SkillDefinition.STATE_AIRBORNE, SkillDefinition.STATE_SWIMMING));

    private SkillOverrides() {
    }

    /**
     * @param root the file's content
     * @param registered the skills as the mods registered them, by id
     * @param defaults gives the allowed range of each number that has one
     * @param knownTypes every weapon type id that exists (built-in and the pack's own)
     * @param problems receives one sentence per wrong entry, starting with the entry's key
     * @return every registered skill, with the file's valid values in place
     */
    public static Map<ResourceLocation, SkillDefinition> apply(
            JsonObject root, Map<ResourceLocation, SkillDefinition> registered, DefaultsData defaults,
            Set<String> knownTypes, List<String> problems) {
        Map<ResourceLocation, SkillDefinition> effective = new LinkedHashMap<>(registered);
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String skillId = entry.getKey();
            if (skillId.startsWith("_")) {
                continue;
            }
            ResourceLocation id = new ResourceLocation(skillId);
            SkillDefinition skill = id.toString().equals(skillId) ? registered.get(id) : null;
            if (skill == null) {
                problems.add("key '" + skillId + "': no skill with the id " + skillId + " is registered");
            } else if (!entry.getValue().isJsonObject()) {
                problems.add("key '" + skillId + "': value " + entry.getValue() + ": must be an object of values");
            } else {
                effective.put(id, override(skill, entry.getValue().getAsJsonObject(), defaults, knownTypes, problems));
            }
        }
        return effective;
    }

    private static SkillDefinition override(
            SkillDefinition skill, JsonObject entries, DefaultsData defaults, Set<String> knownTypes,
            List<String> problems) {
        SkillValues values = SkillValues.of(skill);
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("_")) {
                continue;
            }
            String key = "key '" + skill.id() + "." + name + "': value " + entry.getValue() + ": ";
            String problem = applyOne(values, name, entry.getValue(), defaults, knownTypes);
            if (problem != null) {
                problems.add(key + problem);
            }
        }
        // Checked last, because either of the two values can come from the file.
        double executionLength = values.numbers.get(SkillNumber.EXECUTION_LENGTH);
        for (double window : values.hitWindowSeconds) {
            if (window > executionLength) {
                problems.add("key '" + skill.id() + "." + HIT_WINDOWS_KEY + "': value " + window
                        + ": this hit window lies after the end of the execution (" + executionLength + " s); '"
                        + HIT_WINDOWS_KEY + "' and '" + SkillNumber.EXECUTION_LENGTH.configName()
                        + "' both keep the skill's own values");
                values.hitWindowSeconds.clear();
                values.hitWindowSeconds.addAll(skill.hitWindowSeconds());
                values.numbers.put(SkillNumber.EXECUTION_LENGTH, skill.number(SkillNumber.EXECUTION_LENGTH));
                break;
            }
        }
        return values.appliedTo(skill);
    }

    /** @return null when the value was applied, otherwise why it was not */
    private static String applyOne(
            SkillValues values, String name, JsonElement value, DefaultsData defaults, Set<String> knownTypes) {
        for (SkillNumber number : SkillNumber.values()) {
            if (number.configName().equals(name)) {
                return applyNumber(values, number, value, defaults.skillRange(name));
            }
        }
        for (SkillFlag flag : SkillFlag.values()) {
            if (flag.configName().equals(name)) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                    return "must be true or false";
                }
                values.flags.put(flag, value.getAsBoolean());
                return null;
            }
        }
        if (WEAPON_TYPES_KEY.equals(name)) {
            Set<String> types = TypeRulesFile.idList(value, knownTypes);
            if (types == null || types.isEmpty()) {
                return "must be a list of at least one known weapon type id";
            }
            values.weaponTypes.clear();
            values.weaponTypes.addAll(types);
            return null;
        }
        if (STATES_KEY.equals(name)) {
            Set<String> states = TypeRulesFile.idList(value, STATES);
            if (states == null || states.isEmpty()) {
                return "must be a list of at least one of " + STATES;
            }
            values.allowedStates.clear();
            values.allowedStates.addAll(states);
            return null;
        }
        if (HIT_WINDOWS_KEY.equals(name)) {
            List<Double> windows = hitWindows(value);
            if (windows == null) {
                return "must be a list of at most " + MOST_HIT_WINDOWS + " objects like {\"" + HIT_WINDOW_TIME_KEY
                        + "\": 0.3} with times of 0 or more";
            }
            values.hitWindowSeconds.clear();
            values.hitWindowSeconds.addAll(windows);
            return null;
        }
        return "unknown value name";
    }

    private static String applyNumber(SkillValues values, SkillNumber number, JsonElement value, double[] range) {
        if (!values.numbers.containsKey(number)) {
            return "this skill does not use this value";
        }
        Double amount = numberOf(value);
        if (amount == null) {
            return "must be a number";
        }
        if (range != null && (amount < range[0] || amount > range[1])) {
            return "allowed is " + range[0] + " to " + range[1];
        }
        if (amount < 0) {
            return "must not be below 0";
        }
        if (number == SkillNumber.EXECUTION_LENGTH && amount == 0) {
            return "an execution needs a length above 0";
        }
        values.numbers.put(number, amount);
        return null;
    }

    private static List<Double> hitWindows(JsonElement value) {
        if (!value.isJsonArray()) {
            return null;
        }
        List<Double> windows = new ArrayList<>();
        for (JsonElement element : (JsonArray) value) {
            Double at = element.isJsonObject() && element.getAsJsonObject().has(HIT_WINDOW_TIME_KEY)
                    ? numberOf(element.getAsJsonObject().get(HIT_WINDOW_TIME_KEY))
                    : null;
            if (at == null || at < 0) {
                return null;
            }
            windows.add(at);
        }
        return windows.size() > MOST_HIT_WINDOWS ? null : windows;
    }

    /** The finite number in a JSON value, or null when it is not one. */
    private static Double numberOf(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        double amount = value.getAsDouble();
        return Double.isNaN(amount) || Double.isInfinite(amount) ? null : amount;
    }
}
