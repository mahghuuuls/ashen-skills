package com.mahghuuuls.ashenskills.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The weapon types other mods add in code, and the items they give a type to. Filled while the game
 * loads and closed after that, so a type id means the same thing for the whole run.
 */
public final class AddedTypes {

    /** The characters of a type id; the same as in a pack's files. */
    public static final Pattern TYPE_ID = Pattern.compile("[a-z0-9_.:-]+");

    private final Set<String> existingBefore;
    private final Set<String> types = new LinkedHashSet<>();
    private final Map<String, Set<String>> typesByItemName = new LinkedHashMap<>();
    private boolean closed;

    /** @param existingBefore the type ids that exist without any other mod: the mod's own */
    public AddedTypes(Set<String> existingBefore) {
        this.existingBefore = existingBefore;
    }

    /** @throws IllegalStateException when closed; IllegalArgumentException for a bad id */
    public synchronized void addType(String typeId) {
        requireOpen("weapon type " + typeId);
        if (typeId == null || !TYPE_ID.matcher(typeId).matches()) {
            throw new IllegalArgumentException("A weapon type id uses lower-case letters, digits and _ . : - only, "
                    + "but got: " + typeId);
        }
        if (!existingBefore.contains(typeId)) {
            types.add(typeId);
        }
    }

    /** @throws IllegalStateException when closed; IllegalArgumentException for a type that does not exist */
    public synchronized void addItemType(String itemName, String typeId) {
        requireOpen("the type " + typeId + " of item " + itemName);
        if (itemName == null || itemName.isEmpty()) {
            throw new IllegalArgumentException("An item name is needed");
        }
        if (!existingBefore.contains(typeId) && !types.contains(typeId)) {
            throw new IllegalArgumentException("The weapon type " + typeId + " does not exist; register it first");
        }
        Set<String> ofItem = typesByItemName.get(itemName);
        if (ofItem == null) {
            ofItem = new LinkedHashSet<>();
            typesByItemName.put(itemName, ofItem);
        }
        ofItem.add(typeId);
    }

    /** Ends the adding. Called once, when the game has finished loading mods. */
    public synchronized void close() {
        closed = true;
    }

    /** The type ids other mods added, without the ones that existed before. */
    public synchronized Set<String> types() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(types));
    }

    /** The types other mods gave this item, empty when none. */
    public synchronized Set<String> typesOf(String itemName) {
        Set<String> ofItem = typesByItemName.get(itemName);
        return ofItem == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(ofItem));
    }

    private void requireOpen(String what) {
        if (closed) {
            throw new IllegalStateException(
                    "Too late to add " + what + "; add weapon types before the game finishes loading");
        }
    }
}
