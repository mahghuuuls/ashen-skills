package com.mahghuuuls.ashenskills.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * What a pack says about weapon types: the type ids it adds, and for single items the types to add
 * to the built-in ones or to use in their place.
 */
public final class TypeRules {

    /** A pack that says nothing. */
    public static final TypeRules NONE =
            new TypeRules(Collections.<String>emptySet(), Collections.<String, ItemRule>emptyMap());

    private final Set<String> packTypes;
    private final Map<String, ItemRule> items;

    public TypeRules(Set<String> packTypes, Map<String, ItemRule> items) {
        this.packTypes = Collections.unmodifiableSet(new LinkedHashSet<>(packTypes));
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    /** What the pack says about one item. */
    public static final class ItemRule {

        private final boolean replace;
        private final Set<String> types;

        /** @param replace true to use these types in place of the built-in ones, false to add them */
        public ItemRule(boolean replace, Set<String> types) {
            this.replace = replace;
            this.types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
        }

        public boolean replaces() {
            return replace;
        }

        public Set<String> types() {
            return types;
        }
    }

    /** The type ids the pack defines, beyond the built-in ones. */
    public Set<String> packTypes() {
        return packTypes;
    }

    /** The pack's rule for each item it names, by item registry name. */
    public Map<String, ItemRule> items() {
        return items;
    }

    /**
     * The types an item has under these rules.
     *
     * @param itemName the item's registry name
     * @param builtIn the types the item has without any pack
     */
    public Set<String> effectiveTypes(String itemName, Set<String> builtIn) {
        ItemRule rule = items.get(itemName);
        if (rule == null) {
            return builtIn;
        }
        if (rule.replaces()) {
            return rule.types();
        }
        Set<String> all = new LinkedHashSet<>(builtIn);
        all.addAll(rule.types());
        return Collections.unmodifiableSet(all);
    }
}
