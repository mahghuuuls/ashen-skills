package com.mahghuuuls.ashenskills.api;

/** Where a running skill is. A player in no phase is not using a skill. */
public enum SkillPhase {

    /** The committed action. Costs are paid and the cooldown runs. */
    EXECUTION,
    /** After a completed execution; normal attacks stay unavailable. */
    RECOVERY,
    /** After an interrupted execution; normal attacks stay unavailable. */
    INTERRUPTED_RECOVERY
}
