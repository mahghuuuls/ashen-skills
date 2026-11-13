package com.mahghuuuls.ashenskills.skill;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * The Ash of a skill: the item that lets an Ashen Forge attach that skill to weapons. There is one
 * Ash item for all skills; which skill an Ash holds is written on the stack.
 *
 * <p>Layout in the item's NBT: {@code {ashenskills:{ash:"<namespace>:<name>"}}}. Packs and commands
 * may write this form directly. An Ash keeps its skill id when no installed mod provides that skill.
 */
public final class Ash {

    private static final AshItem ITEM = new AshItem();

    private Ash() {
    }

    /** The one Ash item. */
    public static AshItem item() {
        return ITEM;
    }

    /** One Ash of the skill with that id. */
    public static ItemStack of(String skillId) {
        ItemStack stack = new ItemStack(ITEM);
        NBTTagCompound root = new NBTTagCompound();
        AshTag.writeSkillId(root, skillId);
        stack.setTagCompound(root);
        return stack;
    }

    public static boolean isAsh(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ITEM;
    }

    /** The id text of the skill this Ash holds; null when the stack is no Ash or names no skill. */
    public static String skillOf(ItemStack stack) {
        return isAsh(stack) ? AshTag.readSkillId(stack.getTagCompound()) : null;
    }

    /** The language key of a skill's name. A mod that adds a skill gives this key a text. */
    public static String nameKey(String skillId) {
        return languageKey(skillId, "name");
    }

    /** The language key of a skill's one-sentence description. */
    public static String descriptionKey(String skillId) {
        return languageKey(skillId, "description");
    }

    private static String languageKey(String skillId, String part) {
        ResourceLocation id = new ResourceLocation(skillId);
        return "skill." + id.getNamespace() + "." + id.getPath() + "." + part;
    }
}
