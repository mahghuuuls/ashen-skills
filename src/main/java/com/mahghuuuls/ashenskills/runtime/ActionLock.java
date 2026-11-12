package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * What a player may not do while using a skill. Normal attacks are blocked in every phase; raising
 * a shield, using items, and jumping follow the skill's own rules for the phase; sneaking follows
 * one rule of the skill for all its phases. The
 * client is told the answers for attack, jump, and sneak with each phase report, so it never works
 * them out itself.
 */
final class ActionLock {

    private ActionLock() {
    }

    /** Normal attacks, on entities and on blocks, have no effect during any skill phase. */
    static boolean blocksAttack(SkillSession session) {
        return session != null;
    }

    /**
     * Whether the player may not jump now. Jumping is the client's own action, so this rule is kept
     * by the player's client; the server only says what the rule is.
     */
    static boolean blocksJump(SkillSession session) {
        return session != null && locksJump(session.skill(), session.phase());
    }

    /** The skill's jump rule: it holds during execution only. */
    static boolean locksJump(SkillDefinition skill, SkillPhase phase) {
        return phase == SkillPhase.EXECUTION && !skill.flag(SkillFlag.JUMP_DURING_EXECUTION);
    }

    /**
     * Whether the player may not sneak now. A skill that does not allow sneaking makes a sneaking
     * player stand up for all of its phases, so its animations need the standing pose only. Like the
     * jump, this is kept by the player's client.
     */
    static boolean blocksSneak(SkillSession session) {
        return session != null && locksSneak(session.skill());
    }

    /** The skill's sneak rule: it holds in every phase of the skill. */
    static boolean locksSneak(SkillDefinition skill) {
        return !skill.flag(SkillFlag.SNEAKING_DURING_SKILL);
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
