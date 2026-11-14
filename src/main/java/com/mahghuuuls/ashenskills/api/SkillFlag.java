package com.mahghuuuls.ashenskills.api;

/** The yes-or-no rules of a skill. Every skill has all of them, except the two about preparation. */
public enum SkillFlag {

    /** Whether the skill can start and continue while the player rides something. */
    MOUNTED_ALLOWED("mountedAllowed", true),
    /** Whether taking damage interrupts the execution. */
    EXECUTION_INTERRUPTED_BY_DAMAGE("executionInterruptedByDamage", true),
    /** Whether being knocked back interrupts the execution. */
    EXECUTION_INTERRUPTED_BY_KNOCKBACK("executionInterruptedByKnockback", true),
    /** Whether taking damage ends the preparation. Only skills that are not instant have it. */
    PREPARATION_CANCELED_BY_DAMAGE("preparationCanceledByDamage", false),
    /** Whether being knocked back ends the preparation. Only skills that are not instant have it. */
    PREPARATION_CANCELED_BY_KNOCKBACK("preparationCanceledByKnockback", false),
    /** Whether the player may raise a shield during execution. */
    BLOCKING_DURING_EXECUTION("blockingDuringExecution", true),
    /** Whether the player may use items during execution. */
    ITEM_USE_DURING_EXECUTION("itemUseDuringExecution", true),
    /** Whether the player may raise a shield during recovery. */
    BLOCKING_DURING_RECOVERY("blockingDuringRecovery", true),
    /** Whether the player may use items during recovery. */
    ITEM_USE_DURING_RECOVERY("itemUseDuringRecovery", true),
    /** Whether the player may jump during execution. */
    JUMP_DURING_EXECUTION("jumpDuringExecution", true),
    /** Whether the player may sneak while the skill runs. When false, a sneaking player stands up for the skill. */
    SNEAKING_DURING_SKILL("sneakingDuringSkill", true),
    /** Whether solid blocks between the user and a target shield that target. */
    SOLID_BLOCKS_OBSTRUCT("solidBlocksObstruct", true),
    /** Whether the weapon's damage enchantments (Sharpness, Smite, Bane of Arthropods) add to a hit. */
    DAMAGE_ENCHANTMENTS("damageEnchantments", true),
    /** Whether Fire Aspect on the weapon sets hit targets on fire. */
    FIRE_ASPECT("fireAspect", true),
    /** Whether the Knockback enchantment adds to the skill's own push. */
    KNOCKBACK_ENCHANTMENT("knockback", true),
    /** Whether Looting on the weapon counts for kills made by the skill. */
    LOOTING("looting", true);

    private final String configName;
    private final boolean requiredForEverySkill;

    SkillFlag(String configName, boolean requiredForEverySkill) {
        this.configName = configName;
        this.requiredForEverySkill = requiredForEverySkill;
    }

    /** False for rules only some skills have; reading such a rule from a skill without it fails. */
    public boolean isRequiredForEverySkill() {
        return requiredForEverySkill;
    }

    /** The name used in configuration and data files. */
    public String configName() {
        return configName;
    }
}
