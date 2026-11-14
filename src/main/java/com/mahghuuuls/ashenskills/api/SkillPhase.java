package com.mahghuuuls.ashenskills.api;

/** Where a running skill is. A player in no phase is not using a skill. */
public enum SkillPhase {

    /** The committed action. Costs are paid and the cooldown runs. */
    EXECUTION,
    /** After a completed execution; normal attacks stay unavailable. */
    RECOVERY,
    /** After an interrupted execution; normal attacks stay unavailable. */
    INTERRUPTED_RECOVERY,
    /**
     * Before the execution of a skill that is not instant: the stance held, or the blow charged.
     * Nothing is paid and no cooldown runs; it lasts until the player executes or leaves it.
     */
    PREPARATION
}
