package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.CooldownStateMessage;
import com.mahghuuuls.ashenskills.network.EffectCueMessage;
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
        logSent(player, session, "selfAndTracking");
    }

    @Override
    public void phaseShownTo(EntityPlayerMP watcher, EntityPlayerMP player, SkillSession session) {
        AshenNetwork.channel().sendTo(phaseMessage(player, session), watcher);
        logSent(player, session, watcher.getName());
    }

    /** The phase message is what starts and stops animations on clients. */
    private static void logSent(EntityPlayerMP player, SkillSession session, String to) {
        if (session == null) {
            Diagnostics.log("ANIMATION_MESSAGE", "player={} phase=NONE to={}", player.getName(), to);
        } else {
            Diagnostics.log("ANIMATION_MESSAGE", "player={} skill={} phase={} elapsedTicks={} lengthTicks={} to={}",
                    player.getName(), session.skill().id(), session.phase(), session.ticksInPhase(),
                    session.phaseLengthTicks(), to);
        }
    }

    @Override
    public void swingShown(EntityPlayerMP player, SkillSession session, double x, double y, double z) {
        sendCue(player, session, EffectCueMessage.SWING, "SWING", x, y, z, 0);
    }

    @Override
    public void impactShown(EntityPlayerMP player, SkillSession session, double x, double y, double z, int particles) {
        sendCue(player, session, EffectCueMessage.IMPACT, "IMPACT", x, y, z, particles);
    }

    /** The user is not among those "tracking" the user, hence the two sends. */
    private static void sendCue(
            EntityPlayerMP player, SkillSession session, byte kind, String kindName,
            double x, double y, double z, int particles) {
        EffectCueMessage message = new EffectCueMessage(
                kind, player.getEntityId(), session.useId(), session.skill().id().toString(), x, y, z, particles);
        AshenNetwork.channel().sendTo(message, player);
        AshenNetwork.channel().sendToAllTracking(message, player);
        Diagnostics.log("EFFECT_CUE", "player={} skill={} use={} kind={} to=selfAndTracking",
                player.getName(), session.skill().id(), session.useId(), kindName);
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
            return new PhaseUpdateMessage(player.getEntityId(), "", PhaseUpdateMessage.NO_PHASE, 0, 0, false, false, false);
        }
        return new PhaseUpdateMessage(
                player.getEntityId(),
                session.skill().id().toString(),
                PhaseUpdateMessage.codeOf(session.phase()),
                session.ticksInPhase(),
                session.phaseLengthTicks(),
                ActionLock.blocksAttack(session),
                ActionLock.blocksJump(session),
                ActionLock.blocksSneak(session));
    }
}
