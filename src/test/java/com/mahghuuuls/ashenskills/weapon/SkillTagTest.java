package com.mahghuuuls.ashenskills.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

class SkillTagTest {

    @Test
    void anItemWithoutDataCarriesNoSkill() {
        assertNull(SkillTag.readSkillId(null));
        assertNull(SkillTag.readSkillId(new NBTTagCompound()));
    }

    @Test
    void aWrittenSkillIsReadBackInTheDocumentedLayout() {
        NBTTagCompound root = new NBTTagCompound();
        SkillTag.writeSkillId(root, "ashenskills:powerful_slash");

        assertEquals("ashenskills:powerful_slash", SkillTag.readSkillId(root));
        assertEquals("ashenskills:powerful_slash", root.getCompoundTag("ashenskills").getString("skill"));
    }

    @Test
    void writingReplacesTheEarlierSkillAndKeepsOtherData() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("RepairCost", 3);
        SkillTag.writeSkillId(root, "a:first");
        SkillTag.writeSkillId(root, "b:second");

        assertEquals("b:second", SkillTag.readSkillId(root));
        assertEquals(3, root.getInteger("RepairCost"));
    }

    @Test
    void removingLeavesNoEmptyCompoundAndKeepsOtherData() {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("RepairCost", 3);
        SkillTag.writeSkillId(root, "a:first");

        boolean nothingLeft = SkillTag.removeSkillId(root);

        assertFalse(nothingLeft);
        assertNull(SkillTag.readSkillId(root));
        assertFalse(root.hasKey("ashenskills"));
        assertEquals(3, root.getInteger("RepairCost"));
    }

    @Test
    void removingTheOnlyDataReportsThatNothingIsLeft() {
        NBTTagCompound root = new NBTTagCompound();
        SkillTag.writeSkillId(root, "a:first");

        assertTrue(SkillTag.removeSkillId(root));
        assertTrue(root.isEmpty());
        assertTrue(SkillTag.removeSkillId(new NBTTagCompound()));
    }

    @Test
    void removingKeepsOtherEntriesOfTheModsCompound() {
        NBTTagCompound root = new NBTTagCompound();
        SkillTag.writeSkillId(root, "a:first");
        root.getCompoundTag("ashenskills").setInteger("other", 1);

        SkillTag.removeSkillId(root);

        assertTrue(root.hasKey("ashenskills"));
        assertEquals(1, root.getCompoundTag("ashenskills").getInteger("other"));
    }

    @Test
    void dataOfTheWrongKindOrAnEmptyIdCountsAsNoSkill() {
        NBTTagCompound wrongKind = new NBTTagCompound();
        NBTTagCompound own = new NBTTagCompound();
        own.setInteger("skill", 7);
        wrongKind.setTag("ashenskills", own);
        assertNull(SkillTag.readSkillId(wrongKind));

        NBTTagCompound notACompound = new NBTTagCompound();
        notACompound.setString("ashenskills", "ashenskills:powerful_slash");
        assertNull(SkillTag.readSkillId(notACompound));

        NBTTagCompound empty = new NBTTagCompound();
        SkillTag.writeSkillId(empty, "");
        assertNull(SkillTag.readSkillId(empty));
    }
}
