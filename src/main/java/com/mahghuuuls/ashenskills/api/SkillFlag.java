package com.mahghuuuls.ashenskills.api;

/** The yes-or-no rules every skill has. */
public enum SkillFlag {

    /** Whether the skill can start and continue while the player rides something. */
    MOUNTED_ALLOWED("mountedAllowed"),
    /** Whether taking damage interrupts the execution. */
    EXECUTION_INTERRUPTED_BY_DAMAGE("executionInterruptedByDamage"),
    /** Whether being knocked back interrupts the execution. */
    EXECUTION_INTERRUPTED_BY_KNOCKBACK("executionInterruptedByKnockback"),
    /** Whether the player may raise a shield during execution. */
    BLOCKING_DURING_EXECUTION("blockingDuringExecution"),
    /** Whether the player may use items during execution. */
    ITEM_USE_DURING_EXECUTION("itemUseDuringExecution"),
    /** Whether the player may raise a shield during recovery. */
    BLOCKING_DURING_RECOVERY("blockingDuringRecovery"),
    /** Whether the player may use items during recovery. */
    ITEM_USE_DURING_RECOVERY("itemUseDuringRecovery"),
    /** Whether solid blocks between the user and a target shield that target. */
    SOLID_BLOCKS_OBSTRUCT("solidBlocksObstruct"),
    /** Whether the weapon's damage enchantments (Sharpness, Smite, Bane of Arthropods) add to a hit. */
    DAMAGE_ENCHANTMENTS("damageEnchantments"),
    /** Whether Fire Aspect on the weapon sets hit targets on fire. */
    FIRE_ASPECT("fireAspect"),
    /** Whether the Knockback enchantment adds to the skill's own push. */
    KNOCKBACK_ENCHANTMENT("knockback"),
    /** Whether Looting on the weapon counts for kills made by the skill. */
    LOOTING("looting");

    private final String configName;

    SkillFlag(String configName) {
        this.configName = configName;
    }

    /** The name used in configuration and data files. */
    public String configName() {
        return configName;
    }
}
