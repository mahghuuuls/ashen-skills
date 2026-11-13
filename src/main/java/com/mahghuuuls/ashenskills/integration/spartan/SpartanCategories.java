package com.mahghuuuls.ashenskills.integration.spartan;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.integration.ClassTable;
import com.mahghuuuls.ashenskills.integration.WeaponCategorySource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;

/**
 * The melee categories of Spartan Weaponry as weapon types. Spartan Weaponry has no question
 * "which category is this item"; each category is its own item class, and none of these classes
 * extends another, so the class of an item says its category exactly. Its ranged and thrown
 * weapons are not listed and get no type.
 */
public final class SpartanCategories implements WeaponCategorySource {

    private static final String ITEM_PACKAGE = "com.oblivioussp.spartanweaponry.item.";

    /** Weapon type to the simple name of the Spartan Weaponry class of that category. */
    private static final Map<String, String> CLASS_BY_TYPE = new LinkedHashMap<>();

    static {
        CLASS_BY_TYPE.put("dagger", "ItemDagger");
        CLASS_BY_TYPE.put("parrying_dagger", "ItemParryingDagger");
        CLASS_BY_TYPE.put("longsword", "ItemLongsword");
        CLASS_BY_TYPE.put("katana", "ItemKatana");
        CLASS_BY_TYPE.put("saber", "ItemSaber");
        CLASS_BY_TYPE.put("rapier", "ItemRapier");
        CLASS_BY_TYPE.put("greatsword", "ItemGreatsword");
        CLASS_BY_TYPE.put("hammer", "ItemHammer");
        CLASS_BY_TYPE.put("warhammer", "ItemWarhammer");
        CLASS_BY_TYPE.put("spear", "ItemSpear");
        CLASS_BY_TYPE.put("halberd", "ItemHalberd");
        CLASS_BY_TYPE.put("pike", "ItemPike");
        CLASS_BY_TYPE.put("lance", "ItemLance");
        CLASS_BY_TYPE.put("battleaxe", "ItemBattleaxe");
        CLASS_BY_TYPE.put("mace", "ItemMace");
        CLASS_BY_TYPE.put("glaive", "ItemGlaive");
        CLASS_BY_TYPE.put("quarterstaff", "ItemQuarterstaff");
        CLASS_BY_TYPE.put("club", "ItemClub");
        CLASS_BY_TYPE.put("caestus", "ItemCaestus");
        CLASS_BY_TYPE.put("scythe", "ItemScythe");
    }

    private final ClassTable table;

    public SpartanCategories() {
        Map<String, String> typeByClassName = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : CLASS_BY_TYPE.entrySet()) {
            typeByClassName.put(ITEM_PACKAGE + entry.getValue(), entry.getKey());
        }
        table = new ClassTable(typeByClassName);
        if (!table.missing().isEmpty()) {
            // A version of Spartan Weaponry without some category; the others still work.
            LogManager.getLogger(Tags.MOD_NAME).warn(
                    "Spartan Weaponry has no class {}; its weapon type is given to no item", table.missing());
        }
    }

    /** The weapon types this class can give; they must be the ones the defaults file lists. */
    public static Set<String> types() {
        return Collections.unmodifiableSet(CLASS_BY_TYPE.keySet());
    }

    @Override
    public Set<String> typesOf(Object item) {
        return table.typesOf(item);
    }
}
