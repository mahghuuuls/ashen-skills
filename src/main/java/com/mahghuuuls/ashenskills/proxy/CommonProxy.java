package com.mahghuuuls.ashenskills.proxy;

import com.mahghuuuls.ashenskills.api.SkillPhase;

/**
 * The only door from shared code to client-only code. Shared code calls methods declared here; the
 * dedicated server gets this class, whose methods do nothing, and the client gets the subclass in
 * the client package. Shared code must never name a client class directly.
 */
public class CommonProxy {

    /** Sets up client-only parts (key bindings, client event handlers). */
    public void init() {
    }

    /**
     * A player's skill phase as the server reports it. Called on the network thread.
     *
     * @param phaseCode the phase as the phase message codes it
     * @param attackLocked whether a normal attack by that player has no effect in this phase
     * @param jumpLocked whether that player may not jump in this phase
     * @param sneakLocked whether that player may not sneak in this phase
     */
    public void onPhaseUpdate(
            int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks,
            boolean attackLocked, boolean jumpLocked, boolean sneakLocked) {
    }

    /**
     * A moment of a player's skill to show and play. Called on the network thread.
     *
     * @param kind a kind code of the effect cue message
     */
    public void onEffectCue(
            byte kind, int userEntityId, int useId, String skillId, double x, double y, double z, int particles) {
    }

    /** The server refused to start the skill. Called on the network thread. */
    public void onRefusal(String messageKey) {
    }

    /**
     * An operator asks this client to use an animation check control. Called on the network thread.
     *
     * @param action an action code of the pose control message
     * @param phase for a pin, the phase whose clip to hold; otherwise null
     */
    public void onPoseControl(byte action, String skillId, SkillPhase phase, double seconds, int cameraView) {
    }

    /** The own player's cooldown as the server reports it. Called on the network thread. */
    public void onCooldownState(long remainingMillis, long lengthMillis) {
    }
}
