package com.mahghuuuls.ashenskills.client.state;

import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * What this client was told by the server: which skill phase each visible player is in, and the
 * own cooldown. The client shows this; it never decides it.
 */
public final class ClientSkillState {

    /** One player's phase as last reported. */
    public static final class PhaseView {

        public final String skillId;
        /** The phase as the server's message codes it. */
        public final byte phaseCode;
        public final int elapsedTicksAtReport;
        public final int lengthTicks;
        /** Whether a normal attack by this player has no effect in this phase, as the server says. */
        public final boolean attackLocked;
        /** Whether this player may not jump in this phase, as the server says. */
        public final boolean jumpLocked;
        /** Whether this player may not sneak in this phase, as the server says. */
        public final boolean sneakLocked;
        /** This client's clock when the report arrived. */
        public final long reportedAtMillis;
        private final long staleAfterMillis;

        PhaseView(
                String skillId, byte phaseCode, int elapsedTicksAtReport, int lengthTicks,
                boolean attackLocked, boolean jumpLocked, boolean sneakLocked) {
            this.skillId = skillId;
            this.phaseCode = phaseCode;
            this.elapsedTicksAtReport = elapsedTicksAtReport;
            this.lengthTicks = lengthTicks;
            this.attackLocked = attackLocked;
            this.jumpLocked = jumpLocked;
            this.sneakLocked = sneakLocked;
            this.reportedAtMillis = System.currentTimeMillis();
            long remainingMillis = Math.max(0, lengthTicks - elapsedTicksAtReport) * MILLIS_PER_TICK;
            this.staleAfterMillis = reportedAtMillis + remainingMillis + STALE_GRACE_MILLIS;
        }
    }

    private static final long MILLIS_PER_TICK = 50;
    /** How long after a phase's own end a report is still trusted when no newer one arrived. */
    private static final long STALE_GRACE_MILLIS = 1000;

    private static final Map<Integer, PhaseView> PHASES = new HashMap<>();
    private static long cooldownEndsAtMillis;
    private static long cooldownLengthMillis;

    private ClientSkillState() {
    }

    /** @param phaseCode the "no phase" code removes the player's phase */
    public static void setPhase(
            int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks,
            boolean attackLocked, boolean jumpLocked, boolean sneakLocked) {
        if (phaseCode == 0) {
            PHASES.remove(entityId);
        } else {
            PHASES.put(entityId, new PhaseView(
                    skillId, phaseCode, elapsedTicks, lengthTicks, attackLocked, jumpLocked, sneakLocked));
        }
        Diagnostics.log("CLIENT_PHASE",
                "entity={} skill={} phaseCode={} elapsedTicks={} lengthTicks={} attackLocked={} jumpLocked={}"
                        + " sneakLocked={}",
                entityId, skillId, phaseCode, elapsedTicks, lengthTicks, attackLocked, jumpLocked, sneakLocked);
    }

    /**
     * The phase of that entity, or null when it is in none. A report that outlived its phase is
     * dropped: the server sends no update for a player this client stopped seeing, so without this
     * an entity could keep a phase that ended long ago.
     */
    public static PhaseView phaseOf(int entityId) {
        PhaseView view = PHASES.get(entityId);
        if (view != null && System.currentTimeMillis() > view.staleAfterMillis) {
            PHASES.remove(entityId);
            return null;
        }
        return view;
    }

    /** Whether that player is in a phase whose skill makes a sneaking player stand up. */
    public static boolean isSneakLocked(int entityId) {
        if (PHASES.isEmpty()) {
            return false;
        }
        PhaseView view = phaseOf(entityId);
        return view != null && view.sneakLocked;
    }

    /** Drops every report that outlived its phase, including those of players no one asks about. */
    public static void dropEndedPhases() {
        long now = System.currentTimeMillis();
        for (Iterator<PhaseView> views = PHASES.values().iterator(); views.hasNext();) {
            if (now > views.next().staleAfterMillis) {
                views.remove();
            }
        }
    }

    /** False when no player is known to be in a phase: lets renderers skip all further work. */
    public static boolean hasAnyPhase() {
        return !PHASES.isEmpty();
    }

    public static void setOwnCooldown(long remainingMillis, long lengthMillis) {
        cooldownEndsAtMillis = System.currentTimeMillis() + remainingMillis;
        cooldownLengthMillis = lengthMillis;
        Diagnostics.log("CLIENT_COOLDOWN", "remainingMs={} lengthMs={}", remainingMillis, lengthMillis);
    }

    public static long ownCooldownRemainingMillis() {
        return Math.max(0, cooldownEndsAtMillis - System.currentTimeMillis());
    }

    public static long ownCooldownLengthMillis() {
        return cooldownLengthMillis;
    }

    /** Forgets everything; for leaving a world. */
    public static void clear() {
        PHASES.clear();
        cooldownEndsAtMillis = 0;
        cooldownLengthMillis = 0;
    }
}
