package com.mahghuuuls.ashenskills.client;

import com.mahghuuuls.ashenskills.AshenSkillsMod;

/**
 * Java options that leave one client part of the mod out for a single launch, so the game can be
 * compared with and without it when a problem is looked into. A part is left out with
 * {@code -Dashenskills.<name>=false}; each use is written to the log.
 */
public final class ClientSwitches {

    /** The first-person skill view; without it skills show the game's own hand. */
    public static final String FIRST_PERSON_VIEW = "firstPersonView";
    /** The client's attack and jump lock; without it only the server refuses those actions. */
    public static final String INPUT_LOCK = "inputLock";

    private ClientSwitches() {
    }

    /** @return true when the launch asked for that part to be left out */
    public static boolean isLeftOut(String name) {
        String property = "ashenskills." + name;
        if (!"false".equalsIgnoreCase(System.getProperty(property))) {
            return false;
        }
        AshenSkillsMod.LOGGER.warn("-D{}=false: this part of the mod is left out for this launch", property);
        return true;
    }
}
