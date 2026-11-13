package com.mahghuuuls.ashenskills.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mahghuuuls.ashenskills.weapon.SkillTag;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class AshTagTest {

    @Test
    void aWrittenSkillIsReadBackInTheDocumentedLayout() {
        NBTTagCompound root = new NBTTagCompound();
        AshTag.writeSkillId(root, "ashenskills:powerful_slash");

        assertEquals("ashenskills:powerful_slash", AshTag.readSkillId(root));
        assertEquals("ashenskills:powerful_slash", root.getCompoundTag("ashenskills").getString("ash"));
    }

    @Test
    void dataWithoutAnAshEntryNamesNoSkill() {
        assertNull(AshTag.readSkillId(null));
        assertNull(AshTag.readSkillId(new NBTTagCompound()));

        NBTTagCompound emptyId = new NBTTagCompound();
        AshTag.writeSkillId(emptyId, "");
        assertNull(AshTag.readSkillId(emptyId));
    }

    @Test
    void theSkillOfAWeaponIsNotTheSkillOfAnAsh() {
        // A weapon names its skill under "skill"; an Ash under "ash". One must not be read as the other.
        NBTTagCompound weaponData = new NBTTagCompound();
        NBTTagCompound own = new NBTTagCompound();
        own.setString(SkillTag.SKILL_KEY, "ashenskills:powerful_slash");
        weaponData.setTag(SkillTag.COMPOUND_KEY, own);

        assertNotEquals(SkillTag.SKILL_KEY, AshTag.ASH_KEY);
        assertNull(AshTag.readSkillId(weaponData));
    }

    @Test
    void anAshKeepsTheIdOfASkillThatNoModProvides() {
        NBTTagCompound root = new NBTTagCompound();
        AshTag.writeSkillId(root, "gone_addon:lost_skill");

        assertEquals("gone_addon:lost_skill", AshTag.readSkillId(root));
    }
}
