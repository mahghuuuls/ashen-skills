package com.mahghuuuls.ashenskills.api;

/**
 * The numeric values of a skill. Times are in seconds, distances in blocks, angles in degrees,
 * speeds a fraction of normal speed.
 */
public enum SkillNumber {

    /** Length of the lockout of all skills that starts when this skill executes. */
    COOLDOWN("cooldown", true),
    /** Stamina one use costs, before the user's Stamina Efficiency. 0 for none. */
    STAMINA_COST("staminaCost", true),
    /** Mana one use costs, before the user's Mana Efficiency. 0 for none. */
    MANA_COST("manaCost", true),
    /** Length of the execution phase. */
    EXECUTION_LENGTH("executionLength", true),
    /** Length of the recovery after a completed execution. */
    RECOVERY("recovery", true),
    /** Length of the recovery after an interrupted execution. */
    INTERRUPTED_RECOVERY("interruptedRecovery", true),
    /** Movement speed during execution. */
    EXECUTION_SPEED("executionSpeed", true),
    /** Movement speed during both kinds of recovery. */
    RECOVERY_SPEED("recoverySpeed", true),
    /** Movement speed during preparation. Only skills that are not instant have it. */
    PREPARATION_SPEED("preparationSpeed", false),
    /**
     * What the user's stamina regeneration is multiplied by during preparation: 0 stops it, 1
     * leaves it. Only skills that are not instant have it.
     */
    PREPARATION_STAMINA_REGEN("preparationStaminaRegen", false),
    /** The shortest charge, in seconds, that executes when the key is released. Only charged skills have it. */
    MIN_CHARGE("minCharge", false),
    /**
     * The charge, in seconds, that gives the whole charge bonus; a value below the minimum counts as
     * the minimum. Only charged skills have it. A charged skill also needs a sound named
     * {@code <namespace>:skill.<name>.charged} for the moment its charge is full.
     */
    FULL_CHARGE("fullCharge", false),
    /** The extra share of damage at full charge: 0.8 for 80 percent more. Only charged skills have it. */
    MAX_CHARGE_BONUS("maxChargeBonus", false),
    /** Weapon damage is multiplied by this for each hit of the skill. */
    DAMAGE_MULTIPLIER("damageMultiplier", true),
    /** How far the skill's strike reaches. */
    REACH("reach", true),
    /** How far below the user's feet a target's box may end and still be reached. */
    REACH_BELOW("reachBelow", true),
    /** How far above the user's feet a target's box may begin and still be reached. */
    REACH_ABOVE("reachAbove", true),
    /** Sparks shown at each damaged target, before each player's own particle limit. */
    IMPACT_PARTICLES("impactParticles", true),
    /**
     * Strength of the push a hit gives, away from the user. The game also gives its own small push
     * for any damage a creature takes from another; this one comes in addition.
     */
    KNOCKBACK_STRENGTH("knockbackStrength", true),
    /** Full opening angle of an arc-shaped strike. Only skills with such a strike have it. */
    ARC("arc", false),
    /** Full width of a line-shaped strike. Only skills with such a strike have it. */
    WIDTH("width", false);

    private final String configName;
    private final boolean requiredForEverySkill;

    SkillNumber(String configName, boolean requiredForEverySkill) {
        this.configName = configName;
        this.requiredForEverySkill = requiredForEverySkill;
    }

    /** The name used in configuration and data files. */
    public String configName() {
        return configName;
    }

    /** False for values only some skills use; reading such a value from a skill without it fails. */
    public boolean isRequiredForEverySkill() {
        return requiredForEverySkill;
    }
}
