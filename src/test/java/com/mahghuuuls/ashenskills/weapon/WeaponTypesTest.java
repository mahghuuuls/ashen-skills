package com.mahghuuuls.ashenskills.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mahghuuuls.ashenskills.weapon.TypeRules.ItemRule;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeaponTypesTest {

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Test
    void anItemsTypesWithoutAPackAreTheModsOwnAndTheOtherModsCategoryTogether() {
        Set<String> none = Collections.emptySet();

        assertEquals(none, WeaponTypes.builtInTypes(null, none));
        assertEquals(set("sword"), WeaponTypes.builtInTypes(set("sword"), none));
        assertEquals(set("katana"), WeaponTypes.builtInTypes(null, set("katana")));
        assertEquals(set("sword", "katana"), WeaponTypes.builtInTypes(set("sword"), set("katana")));
    }

    @Test
    void aPackRuleAddsToThoseTypesOrStandsInTheirPlace() {
        Map<String, ItemRule> items = new LinkedHashMap<>();
        items.put("a:added", new ItemRule(false, set("club_heavy")));
        items.put("a:replaced", new ItemRule(true, set("sword")));
        items.put("a:stripped", new ItemRule(true, Collections.<String>emptySet()));
        TypeRules rules = new TypeRules(set("club_heavy"), items);

        assertEquals(set("katana", "club_heavy"), rules.effectiveTypes("a:added", set("katana")));
        assertEquals(set("sword"), rules.effectiveTypes("a:replaced", set("katana")));
        assertEquals(Collections.emptySet(), rules.effectiveTypes("a:stripped", set("katana")));
        assertEquals(set("katana"), rules.effectiveTypes("a:unnamed", set("katana")));
        assertEquals(set("katana"), TypeRules.NONE.effectiveTypes("a:added", set("katana")));
    }
}
