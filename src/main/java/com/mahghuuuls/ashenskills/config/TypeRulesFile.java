package com.mahghuuuls.ashenskills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mahghuuuls.ashenskills.weapon.AddedTypes;
import com.mahghuuuls.ashenskills.weapon.TypeRules;
import com.mahghuuuls.ashenskills.weapon.TypeRules.ItemRule;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Reads a pack's {@code weapon_types.json}. Each wrong entry is reported and left out; the rest
 * applies.
 */
final class TypeRulesFile {

    private static final Pattern TYPE_ID = AddedTypes.TYPE_ID;
    private static final String TYPES_KEY = "types";
    private static final String ITEMS_KEY = "items";
    private static final String ADD_KEY = "add";
    private static final String REPLACE_KEY = "replace";

    private TypeRulesFile() {
    }

    /**
     * @param builtInTypes the type ids the mod knows without a pack
     * @param itemExists whether an item with that registry name is registered
     * @param problems receives one sentence per wrong entry, with the entry's key and its value
     */
    static TypeRules parse(
            JsonObject root, Set<String> builtInTypes, Predicate<String> itemExists, List<String> problems) {
        Set<String> packTypes = new LinkedHashSet<>();
        if (root.has(TYPES_KEY)) {
            JsonElement types = root.get(TYPES_KEY);
            if (!types.isJsonArray()) {
                problems.add("key '" + TYPES_KEY + "': value " + types + ": must be a list of type ids");
            } else {
                for (JsonElement element : types.getAsJsonArray()) {
                    String id = textOf(element);
                    if (id == null || !TYPE_ID.matcher(id).matches()) {
                        problems.add("key '" + TYPES_KEY + "': value " + element
                                + ": a type id uses only lower-case letters, digits and _ . : -");
                    } else {
                        packTypes.add(id);
                    }
                }
            }
        }
        Set<String> knownTypes = new LinkedHashSet<>(builtInTypes);
        knownTypes.addAll(packTypes);

        Map<String, ItemRule> items = new LinkedHashMap<>();
        if (root.has(ITEMS_KEY)) {
            JsonElement entries = root.get(ITEMS_KEY);
            if (!entries.isJsonObject()) {
                problems.add("key '" + ITEMS_KEY + "': value " + entries + ": must be an object of item names");
            } else {
                for (Map.Entry<String, JsonElement> entry : entries.getAsJsonObject().entrySet()) {
                    if (entry.getKey().startsWith("_")) {
                        continue;
                    }
                    ItemRule rule = parseItem(entry.getKey(), entry.getValue(), knownTypes, itemExists, problems);
                    if (rule != null) {
                        items.put(entry.getKey(), rule);
                    }
                }
            }
        }
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (!TYPES_KEY.equals(key) && !ITEMS_KEY.equals(key) && !key.startsWith("_")) {
                problems.add("key '" + key + "': value " + entry.getValue() + ": unknown key; the file has '"
                        + TYPES_KEY + "' and '" + ITEMS_KEY + "'");
            }
        }
        return new TypeRules(packTypes, items);
    }

    private static ItemRule parseItem(
            String itemName, JsonElement value, Set<String> knownTypes, Predicate<String> itemExists,
            List<String> problems) {
        String key = "key '" + ITEMS_KEY + "." + itemName + "': ";
        if (!itemExists.test(itemName)) {
            problems.add(key + "no item with the name " + itemName
                    + " is registered (the full lower-case name is needed, like minecraft:stick)");
            return null;
        }
        if (!value.isJsonObject()) {
            problems.add(key + "value " + value + ": must be an object with '" + ADD_KEY + "' or '" + REPLACE_KEY + "'");
            return null;
        }
        JsonObject rule = value.getAsJsonObject();
        boolean hasAdd = rule.has(ADD_KEY);
        boolean hasReplace = rule.has(REPLACE_KEY);
        if (hasAdd == hasReplace || rule.entrySet().size() != 1) {
            problems.add(key + "value " + value + ": must have exactly one of '" + ADD_KEY + "' and '" + REPLACE_KEY
                    + "' and nothing else");
            return null;
        }
        JsonElement list = rule.get(hasAdd ? ADD_KEY : REPLACE_KEY);
        Set<String> types = idList(list, knownTypes);
        if (types == null) {
            problems.add(key + "value " + list + ": must be a list of known type ids (built-in, or listed under '"
                    + TYPES_KEY + "')");
            return null;
        }
        return new ItemRule(hasReplace, types);
    }

    /** The ids in a JSON list, or null when it is not a list of ids that are all known. */
    static Set<String> idList(JsonElement list, Set<String> known) {
        if (!list.isJsonArray()) {
            return null;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (JsonElement element : (JsonArray) list) {
            String id = textOf(element);
            if (id == null || !known.contains(id)) {
                return null;
            }
            ids.add(id);
        }
        return ids;
    }

    private static String textOf(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : null;
    }
}
