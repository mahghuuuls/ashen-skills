package com.mahghuuuls.ashenskills.weapon;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * The skill a weapon carries, stored on the item itself so it follows the item everywhere.
 *
 * <p>Layout in the item's NBT: {@code {ashenskills:{skill:"<namespace>:<name>"}}}. Packs and
 * commands may write this form directly. Nothing that changes over time is stored here.
 */
public final class SkillTag {

    public static final String COMPOUND_KEY = "ashenskills";
    public static final String SKILL_KEY = "skill";

    private SkillTag() {
    }

    /** The id text of the attached skill, or null when the item carries none. */
    public static String getSkillId(ItemStack stack) {
        return stack.isEmpty() ? null : readSkillId(stack.getTagCompound());
    }

    /** Attaches the skill, replacing any other. Does nothing for an empty stack. */
    public static void setSkillId(ItemStack stack, String skillId) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            stack.setTagCompound(root);
        }
        writeSkillId(root, skillId);
    }

    /** Removes the attached skill and leaves no empty data behind, so the item stacks as before. */
    public static void clear(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && removeSkillId(root)) {
            stack.setTagCompound(null);
        }
    }

    static String readSkillId(NBTTagCompound root) {
        if (root == null || !root.hasKey(COMPOUND_KEY, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        NBTTagCompound own = root.getCompoundTag(COMPOUND_KEY);
        if (!own.hasKey(SKILL_KEY, Constants.NBT.TAG_STRING)) {
            return null;
        }
        String id = own.getString(SKILL_KEY);
        return id.isEmpty() ? null : id;
    }

    static void writeSkillId(NBTTagCompound root, String skillId) {
        NBTTagCompound own = root.getCompoundTag(COMPOUND_KEY);
        own.setString(SKILL_KEY, skillId);
        root.setTag(COMPOUND_KEY, own);
    }

    /**
     * Removes the skill entry, and the mod's compound when nothing else is in it.
     *
     * @return true when the item's data is now empty and can be dropped altogether
     */
    static boolean removeSkillId(NBTTagCompound root) {
        if (root.hasKey(COMPOUND_KEY, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound own = root.getCompoundTag(COMPOUND_KEY);
            own.removeTag(SKILL_KEY);
            if (own.isEmpty()) {
                root.removeTag(COMPOUND_KEY);
            }
        }
        return root.isEmpty();
    }
}
