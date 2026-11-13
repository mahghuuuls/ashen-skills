package com.mahghuuuls.ashenskills.skill;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** How an Ash stack names its skill in its NBT: {@code {ashenskills:{ash:"<namespace>:<name>"}}}. */
final class AshTag {

    static final String COMPOUND_KEY = "ashenskills";
    static final String ASH_KEY = "ash";

    private AshTag() {
    }

    static String readSkillId(NBTTagCompound root) {
        if (root == null || !root.hasKey(COMPOUND_KEY, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        NBTTagCompound own = root.getCompoundTag(COMPOUND_KEY);
        if (!own.hasKey(ASH_KEY, Constants.NBT.TAG_STRING)) {
            return null;
        }
        String id = own.getString(ASH_KEY);
        return id.isEmpty() ? null : id;
    }

    static void writeSkillId(NBTTagCompound root, String skillId) {
        NBTTagCompound own = root.getCompoundTag(COMPOUND_KEY);
        own.setString(ASH_KEY, skillId);
        root.setTag(COMPOUND_KEY, own);
    }
}
