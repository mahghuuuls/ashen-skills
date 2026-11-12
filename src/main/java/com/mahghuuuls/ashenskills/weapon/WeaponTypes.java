package com.mahghuuuls.ashenskills.weapon;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Which weapon types an item has, and which skills fit it. An item with no entry has no type; types
 * are never guessed from an item's name, class or damage, and one type never implies another.
 */
public final class WeaponTypes {

    private static Map<String, Set<String>> typesByItem = Collections.emptyMap();

    private WeaponTypes() {
    }

    /** Loads the built-in classification. Called once while the game loads. */
    public static void load(DefaultsData defaults) {
        typesByItem = defaults.builtInItemTypes();
    }

    /** The weapon types of the item, after all classification rules. Empty when it has none. */
    public static Set<String> effectiveTypes(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptySet();
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        Set<String> types = name == null ? null : typesByItem.get(name.toString());
        return types == null ? Collections.<String>emptySet() : types;
    }

    /** Whether the skill can be used on this item. */
    public static boolean isCompatible(ItemStack stack, SkillDefinition skill) {
        return shareAType(effectiveTypes(stack), skill.weaponTypes());
    }

    /** The compatibility rule: a skill fits a weapon that has at least one of the skill's types. */
    static boolean shareAType(Set<String> typesOfWeapon, Set<String> typesOfSkill) {
        for (String type : typesOfWeapon) {
            if (typesOfSkill.contains(type)) {
                return true;
            }
        }
        return false;
    }
}
