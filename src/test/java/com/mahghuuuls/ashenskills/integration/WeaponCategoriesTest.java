package com.mahghuuuls.ashenskills.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.integration.spartan.SpartanCategories;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeaponCategoriesTest {

    /** Stands for another mod's weapon built on a category class. */
    private static final class AddonList extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void anItemHasTheTypeOfItsClassAndOfNoOtherClass() {
        Map<String, String> typeByClassName = new LinkedHashMap<>();
        typeByClassName.put("java.util.ArrayList", "katana");
        typeByClassName.put("java.util.LinkedList", "greatsword");
        typeByClassName.put("no.such.Class", "rapier");
        ClassTable table = new ClassTable(typeByClassName);

        assertEquals(Collections.singleton("katana"), table.typesOf(new ArrayList<String>()));
        assertEquals(Collections.singleton("katana"), table.typesOf(new AddonList()));
        assertEquals(Collections.singleton("greatsword"), table.typesOf(new LinkedList<String>()));
        assertEquals(Collections.emptySet(), table.typesOf("an item of another kind"));
        assertEquals(Collections.singletonList("no.such.Class"), table.missing());
    }

    @Test
    void theSpartanTableCoversExactlyTheTypesOfTheDefaultsFile() {
        assertEquals(DefaultsData.loadBundled().spartanTypeIds(), new LinkedHashSet<>(SpartanCategories.types()));
    }

}
