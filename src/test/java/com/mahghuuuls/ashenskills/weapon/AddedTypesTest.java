package com.mahghuuuls.ashenskills.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AddedTypesTest {

    private static Set<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test
    void aModAddsATypeAndGivesItToAnItem() {
        AddedTypes added = new AddedTypes(set("sword"));

        added.addType("whip");
        added.addItemType("mymod:whip", "whip");
        added.addItemType("mymod:whip", "sword");

        assertEquals(set("whip"), added.types());
        assertEquals(set("whip", "sword"), added.typesOf("mymod:whip"));
        assertEquals(Collections.emptySet(), added.typesOf("mymod:other"));
    }

    @Test
    void aTypeTheModAlreadyHasIsNotAddedAgain() {
        AddedTypes added = new AddedTypes(set("sword"));

        added.addType("sword");
        added.addType("whip");
        added.addType("whip");

        assertEquals(set("whip"), added.types());
    }

    @Test
    void aTypeIdWithOtherCharactersIsRefused() {
        AddedTypes added = new AddedTypes(set("sword"));

        assertThrows(IllegalArgumentException.class, () -> added.addType("Whip"));
        assertThrows(IllegalArgumentException.class, () -> added.addType("long whip"));
        assertThrows(IllegalArgumentException.class, () -> added.addType(""));
        assertThrows(IllegalArgumentException.class, () -> added.addType(null));
        assertTrue(added.types().isEmpty());
    }

    @Test
    void anItemCannotGetATypeThatDoesNotExist() {
        AddedTypes added = new AddedTypes(set("sword"));

        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> added.addItemType("mymod:whip", "whip"));

        assertTrue(refused.getMessage().contains("whip"), refused.getMessage());
        assertEquals(Collections.emptySet(), added.typesOf("mymod:whip"));
    }

    @Test
    void afterTheGameHasLoadedNothingCanBeAdded() {
        AddedTypes added = new AddedTypes(set("sword"));
        added.addType("whip");
        added.close();

        IllegalStateException late = assertThrows(IllegalStateException.class, () -> added.addType("flail"));
        assertTrue(late.getMessage().contains("before the game finishes loading"), late.getMessage());
        assertThrows(IllegalStateException.class, () -> added.addItemType("mymod:whip", "whip"));
        assertEquals(set("whip"), added.types());
    }
}
