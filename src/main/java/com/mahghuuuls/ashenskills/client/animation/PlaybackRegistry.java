package com.mahghuuuls.ashenskills.client.animation;

import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.animation.clip.Clip;
import com.mahghuuuls.ashenskills.client.animation.clip.ClipSampler;
import com.mahghuuuls.ashenskills.client.animation.clip.Pose;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState.PhaseView;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import java.util.Locale;
import net.minecraft.client.Minecraft;

/**
 * Which clip plays for which player right now, and at what time. Nothing is started or stored
 * here: what plays is read from the skill phases the server reported ({@link ClientSkillState}),
 * so there is one record of a player's phase on the client, not two that could disagree. The one
 * thing kept here is the pin: a check control that holds one clip at one time for the own player.
 */
public final class PlaybackRegistry {

    private static final double SECONDS_PER_TICK = 0.05;

    /** One clip at one moment for one player. */
    private static final class Moment {

        final String skillId;
        final SkillPhase phase;
        /** Time in the clip, when pinned; otherwise time since the phase began. */
        final double seconds;
        /** Length of the phase as the server runs it; unused when pinned. */
        final double phaseSeconds;
        final boolean pinned;

        Moment(String skillId, SkillPhase phase, double seconds, double phaseSeconds, boolean pinned) {
            this.skillId = skillId;
            this.phase = phase;
            this.seconds = seconds;
            this.phaseSeconds = phaseSeconds;
            this.pinned = pinned;
        }

        Pose pose(View view) {
            Clip clip = ClipLibrary.find(skillId, phase, view);
            if (clip == null) {
                return null;
            }
            return ClipSampler.pose(clip, pinned ? seconds : ClipSampler.clipTime(clip, seconds, phaseSeconds));
        }
    }

    private static Moment pin;
    private static final long[] POSES_APPLIED = new long[View.values().length];

    private PlaybackRegistry() {
    }

    /**
     * The pose a player's clip gives now. Cheap when nothing plays: renderers call this for every
     * player every frame.
     *
     * @return null when no clip plays for that player in that view
     */
    public static Pose poseFor(int entityId, View view) {
        Moment moment = momentOf(entityId);
        return moment == null ? null : moment.pose(view);
    }

    /**
     * Whether that player is pinned or in a skill phase now, whether or not a clip file exists for
     * it. As cheap as {@link #poseFor}.
     */
    public static boolean isPlaying(int entityId) {
        return momentOf(entityId) != null;
    }

    private static Moment momentOf(int entityId) {
        if (pin != null && isOwnPlayer(entityId)) {
            return pin;
        }
        if (!ClientSkillState.hasAnyPhase()) {
            return null;
        }
        PhaseView reported = ClientSkillState.phaseOf(entityId);
        SkillPhase phase = reported == null ? null : PhaseUpdateMessage.phaseOf(reported.phaseCode);
        if (phase == null) {
            return null;
        }
        double seconds = ClipSampler.secondsIntoPhase(
                reported.elapsedTicksAtReport, System.currentTimeMillis() - reported.reportedAtMillis);
        return new Moment(reported.skillId, phase, seconds, reported.lengthTicks * SECONDS_PER_TICK, false);
    }

    private static boolean isOwnPlayer(int entityId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player != null && minecraft.player.getEntityId() == entityId;
    }

    /** Holds one clip at one time (seconds in the clip) for the own player until {@link #unpin()}. */
    static void pin(String skillId, SkillPhase phase, double seconds) {
        pin = new Moment(skillId, phase, seconds, 0, true);
    }

    /** Ends a pin. Also called when the player leaves a world. */
    public static void unpin() {
        pin = null;
    }

    /** Renderers report each pose they draw, so a check can tell each of them is at work. */
    public static void notePoseApplied(View view) {
        POSES_APPLIED[view.ordinal()]++;
    }

    /** What plays for a player in a view, with every pose value: for the pose dump. */
    static String describe(int entityId, View view) {
        Moment moment = momentOf(entityId);
        if (moment == null) {
            return "playback=none posesApplied=" + POSES_APPLIED[view.ordinal()];
        }
        Pose pose = moment.pose(view);
        return String.format(Locale.ROOT, "playback=%s phase=%s seconds=%.3f pinned=%s posesApplied=%d pose: %s",
                moment.skillId, moment.phase, moment.seconds, moment.pinned, POSES_APPLIED[view.ordinal()],
                pose == null ? "no clip" : pose.describe());
    }
}
