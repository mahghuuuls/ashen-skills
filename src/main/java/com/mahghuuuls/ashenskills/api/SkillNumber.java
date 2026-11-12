package com.mahghuuuls.ashenskills.api;

/** The numeric values every skill has. Times are in seconds, speeds a fraction of normal speed. */
public enum SkillNumber {

    /** Length of the lockout of all skills that starts when this skill executes. */
    COOLDOWN("cooldown"),
    /** Length of the execution phase. */
    EXECUTION_LENGTH("executionLength"),
    /** Length of the recovery after a completed execution. */
    RECOVERY("recovery"),
    /** Length of the recovery after an interrupted execution. */
    INTERRUPTED_RECOVERY("interruptedRecovery"),
    /** Movement speed during execution. */
    EXECUTION_SPEED("executionSpeed"),
    /** Movement speed during both kinds of recovery. */
    RECOVERY_SPEED("recoverySpeed");

    private final String configName;

    SkillNumber(String configName) {
        this.configName = configName;
    }

    /** The name used in configuration and data files. */
    public String configName() {
        return configName;
    }
}
