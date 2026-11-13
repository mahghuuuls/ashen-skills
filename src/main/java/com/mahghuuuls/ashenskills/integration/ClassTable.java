package com.mahghuuuls.ashenskills.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Weapon types by the class of an item. The classes are known only by name, so this mod needs
 * nothing of the other mod to compile; each name is looked up once, here, and later questions are
 * plain instance checks. An item of a subclass counts too, which is how another mod's weapon built
 * on one of these classes gets the same type.
 */
public final class ClassTable {

    private final Map<Class<?>, String> typeByClass = new LinkedHashMap<>();
    private final List<String> missing = new ArrayList<>();

    /** @param typeByClassName full class name to the weapon type its items have */
    public ClassTable(Map<String, String> typeByClassName) {
        for (Map.Entry<String, String> entry : typeByClassName.entrySet()) {
            try {
                typeByClass.put(Class.forName(entry.getKey()), entry.getValue());
            } catch (ClassNotFoundException | LinkageError notThere) {
                missing.add(entry.getKey());
            }
        }
    }

    /** The class names that could not be found; their types are given to no item. */
    public List<String> missing() {
        return Collections.unmodifiableList(missing);
    }

    public Set<String> typesOf(Object item) {
        Set<String> types = null;
        for (Map.Entry<Class<?>, String> entry : typeByClass.entrySet()) {
            if (entry.getKey().isInstance(item)) {
                if (types == null) {
                    types = new LinkedHashSet<>();
                }
                types.add(entry.getValue());
            }
        }
        return types == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(types);
    }
}
