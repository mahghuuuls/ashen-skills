package com.mahghuuuls.ashenskills.proxy;

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
     * @param phaseCode 0 for no phase, otherwise the phase's ordinal plus one
     */
    public void onPhaseUpdate(int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks) {
    }

    /** The server refused to start the skill. Called on the network thread. */
    public void onRefusal(String messageKey) {
    }

    /** The own player's cooldown as the server reports it. Called on the network thread. */
    public void onCooldownState(long remainingMillis, long lengthMillis) {
    }
}
