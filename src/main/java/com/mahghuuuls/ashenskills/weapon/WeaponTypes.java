package com.mahghuuuls.ashenskills.weapon;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.integration.WeaponCategorySource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Which weapon types an item has, and which skills fit it. An item's types come from the mod's own
 * list, from another mod's weapon categories, and from the pack's rules, which can add to those or
 * stand in their place. An item none of these names has no type: types are never guessed from an
 * item's name or damage, and one type never implies another.
 */
public final class WeaponTypes {

    private static Map<String, Set<String>> typesByItemName = Collections.emptyMap();
    private static WeaponCategorySource categories = WeaponCategorySource.NONE;
    private static TypeRules localRules = TypeRules.NONE;
    private static volatile InForce inForce = new InForce(TypeRules.NONE);

    /**
     * The rules in force and the answers already worked out under them. Kept as one object, so
     * an answer worked out under old rules can never be stored next to new rules.
     */
    private static final class InForce {

        final TypeRules rules;
        final Map<Item, Set<String>> known = new ConcurrentHashMap<>();

        InForce(TypeRules rules) {
            this.rules = rules;
        }
    }

    private WeaponTypes() {
    }

    /**
     * Sets the classification that needs no pack. Called once while the game loads.
     *
     * @param builtInByItemName item registry name to the types the mod itself gives that item
     * @param weaponCategories another mod's categories
     */
    public static void load(Map<String, Set<String>> builtInByItemName, WeaponCategorySource weaponCategories) {
        typesByItemName = builtInByItemName;
        categories = weaponCategories;
        inForce = new InForce(inForce.rules);
    }

    /** Sets the rules of this installation's own pack files. Called once, when the game has loaded. */
    public static void useLocalRules(TypeRules packRules) {
        localRules = packRules;
        inForce = new InForce(packRules);
    }

    /** On a client: the rules of the server it plays on take the place of its own. */
    public static void useServerRules(TypeRules serverRules) {
        inForce = new InForce(serverRules);
    }

    /** On a client that left a server: back to its own rules. */
    public static void restoreLocalRules() {
        inForce = new InForce(localRules);
    }

    /** The weapon types of the item, after all classification rules. Empty when it has none. */
    public static Set<String> effectiveTypes(ItemStack stack) {
        if (stack.isEmpty()) {
            return Collections.emptySet();
        }
        Item item = stack.getItem();
        InForce now = inForce;
        Set<String> types = now.known.get(item);
        if (types == null) {
            types = classify(item, now.rules);
            now.known.put(item, types);
        }
        return types;
    }

    private static Set<String> classify(Item item, TypeRules rules) {
        ResourceLocation name = item.getRegistryName();
        if (name == null) {
            return Collections.emptySet();
        }
        Set<String> builtIn = builtInTypes(typesByItemName.get(name.toString()), categories.typesOf(item));
        return rules.effectiveTypes(name.toString(), builtIn);
    }

    /** The types an item has without a pack: the mod's own list and the other mod's category, together. */
    static Set<String> builtInTypes(Set<String> byName, Set<String> byCategory) {
        if (byName == null || byName.isEmpty()) {
            return byCategory;
        }
        if (byCategory.isEmpty()) {
            return byName;
        }
        Set<String> all = new LinkedHashSet<>(byName);
        all.addAll(byCategory);
        return Collections.unmodifiableSet(all);
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
