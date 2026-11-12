package com.mahghuuuls.ashenskills.runtime;

import net.minecraft.entity.player.EntityPlayerMP;

/** How the skill runtime tells clients what happened. The runtime decides; this only reports. */
public interface SkillMessenger {

    /**
     * The player's skill phase changed.
     *
     * @param session the running skill use, or null when the player is now in no phase
     */
    void phaseChanged(EntityPlayerMP player, SkillSession session);

    /** One other player started to see this player and needs the current phase. */
    void phaseShownTo(EntityPlayerMP watcher, EntityPlayerMP player, SkillSession session);

    void refused(EntityPlayerMP player, RefusalReason reason);

    void cooldownChanged(EntityPlayerMP player, long remainingMillis, long lengthMillis);
}
