package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.CooldownStateMessage;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import com.mahghuuuls.ashenskills.network.RefusalMessage;
import net.minecraft.entity.player.EntityPlayerMP;

/** Reports skill events over the mod's network channel. */
final class NetworkSkillMessenger implements SkillMessenger {

    @Override
    public void phaseChanged(EntityPlayerMP player, SkillSession session) {
        PhaseUpdateMessage message = phaseMessage(player, session);
        AshenNetwork.channel().sendTo(message, player);
        // "Tracking" never includes the player themself, hence the separate send above.
        AshenNetwork.channel().sendToAllTracking(message, player);
    }

    @Override
    public void phaseShownTo(EntityPlayerMP watcher, EntityPlayerMP player, SkillSession session) {
        AshenNetwork.channel().sendTo(phaseMessage(player, session), watcher);
    }

    @Override
    public void refused(EntityPlayerMP player, RefusalReason reason) {
        AshenNetwork.channel().sendTo(new RefusalMessage(reason.messageKey()), player);
    }

    @Override
    public void cooldownChanged(EntityPlayerMP player, long remainingMillis, long lengthMillis) {
        AshenNetwork.channel().sendTo(new CooldownStateMessage(remainingMillis, lengthMillis), player);
    }

    private static PhaseUpdateMessage phaseMessage(EntityPlayerMP player, SkillSession session) {
        if (session == null) {
            return new PhaseUpdateMessage(player.getEntityId(), "", PhaseUpdateMessage.NO_PHASE, 0, 0);
        }
        return new PhaseUpdateMessage(
                player.getEntityId(),
                session.skill().id().toString(),
                (byte) (session.phase().ordinal() + 1),
                session.ticksInPhase(),
                session.phaseLengthTicks());
    }
}
