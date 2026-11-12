package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * What a player may not do while using a skill. Normal attacks are blocked in every phase; raising
 * a shield and using items follow the skill's own rules for the phase.
 */
final class ActionLock {

    private ActionLock() {
    }

    /** Normal attacks, on entities and on blocks, have no effect during any skill phase. */
    static boolean blocksAttack(SkillSession session) {
        return session != null;
    }

    /** Whether the player may not start, or go on with, using that item now. */
    static boolean blocksUse(SkillSession session, EntityPlayer player, ItemStack item) {
        return session != null
                && !item.isEmpty()
                && !allowsUse(session.skill(), session.phase(), item.getItem().isShield(item, player));
    }

    /** The skill's rule for the phase: one for raising a shield, one for every other item use. */
    static boolean allowsUse(SkillDefinition skill, SkillPhase phase, boolean isShield) {
        if (phase == SkillPhase.EXECUTION) {
            return skill.flag(isShield ? SkillFlag.BLOCKING_DURING_EXECUTION : SkillFlag.ITEM_USE_DURING_EXECUTION);
        }
        return skill.flag(isShield ? SkillFlag.BLOCKING_DURING_RECOVERY : SkillFlag.ITEM_USE_DURING_RECOVERY);
    }

    /**
     * Ends an item use that began before the phase and that the phase does not allow, such as a
     * shield raised before the skill key was pressed.
     */
    static void stopForbiddenUse(SkillSession session, EntityPlayerMP player) {
        if (player.isHandActive() && blocksUse(session, player, player.getActiveItemStack())) {
            player.stopActiveHand();
        }
    }
}
