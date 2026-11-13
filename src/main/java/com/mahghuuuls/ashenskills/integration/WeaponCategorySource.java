package com.mahghuuuls.ashenskills.integration;

import java.util.Collections;
import java.util.Set;

/** Another mod's own sorting of its weapons into categories, read as weapon types. */
public interface WeaponCategorySource {

    /** No such mod is installed: no item gets a type this way. */
    WeaponCategorySource NONE = new WeaponCategorySource() {
        @Override
        public Set<String> typesOf(Object item) {
            return Collections.emptySet();
        }
    };

    /**
     * @param item an item of the game
     * @return the weapon types the other mod's categories give it; empty for an item that mod does
     *     not know
     */
    Set<String> typesOf(Object item);
}
