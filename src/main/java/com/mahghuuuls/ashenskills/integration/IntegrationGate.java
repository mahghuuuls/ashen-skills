package com.mahghuuuls.ashenskills.integration;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.Logger;

/**
 * Decides, once while the game loads, what stands behind each gateway: the code for an installed
 * mod, or the behavior for its absence. Code that talks to another mod must be named here only as
 * text and loaded with {@link #implementationOr} after that mod was found, so this mod runs
 * without any of them.
 */
public final class IntegrationGate {

    private static final String CRITWORKS_MOD_ID = "critworks";
    private static final String CRITWORKS_CRITS = "com.mahghuuuls.ashenskills.integration.critworks.CritworksCrits";
    private static final String JASS_MOD_ID = "jass";
    private static final String JASS_STAMINA = "com.mahghuuuls.ashenskills.integration.jass.JassStamina";
    private static final String JAWMS_MOD_ID = "jawms";
    private static final String JAWMS_MANA = "com.mahghuuuls.ashenskills.integration.jawms.JawmsMana";
    private static final String SPARTAN_MOD_ID = "spartanweaponry";
    private static final String SPARTAN_CATEGORIES = "com.mahghuuuls.ashenskills.integration.spartan.SpartanCategories";

    private static ResourceGateway resources;
    private static PreparationRegen preparationRegen;
    private static WeaponCategorySource weaponCategories;
    private static CritGateway crits;

    private IntegrationGate() {
    }

    /** Called once while the game loads, after the settings. */
    public static void decide(Logger logger) {
        ResourcePool stamina = implementationOr(
                Loader.isModLoaded(JASS_MOD_ID), JASS_STAMINA, ResourcePool.class, ResourcePool.ABSENT, logger);
        ResourcePool mana = implementationOr(
                Loader.isModLoaded(JAWMS_MOD_ID), JAWMS_MANA, ResourcePool.class, ResourcePool.ABSENT, logger);
        resources = new ResourceGateway(
                stamina, mana, () -> Settings.general().getBoolean(SettingKey.WAIVE_ABSENT_RESOURCE_COSTS));
        // The stamina mod is also the one that knows about stamina regeneration.
        preparationRegen = stamina instanceof PreparationRegen ? (PreparationRegen) stamina : PreparationRegen.NONE;
        weaponCategories = implementationOr(
                Loader.isModLoaded(SPARTAN_MOD_ID), SPARTAN_CATEGORIES, WeaponCategorySource.class,
                WeaponCategorySource.NONE, logger);
        crits = implementationOr(
                Loader.isModLoaded(CRITWORKS_MOD_ID), CRITWORKS_CRITS, CritGateway.class, CritGateway.NONE, logger);
        logger.info("Other mods: stamina={} mana={} spartanWeaponry={} critworks={}",
                stamina == ResourcePool.ABSENT ? "absent" : "present",
                mana == ResourcePool.ABSENT ? "absent" : "present",
                weaponCategories == WeaponCategorySource.NONE ? "absent" : "present",
                crits == CritGateway.NONE ? "absent" : "present");
    }

    public static ResourceGateway resources() {
        return resources;
    }

    public static PreparationRegen preparationRegen() {
        return preparationRegen;
    }

    public static WeaponCategorySource weaponCategories() {
        return weaponCategories;
    }

    public static CritGateway crits() {
        return crits;
    }

    /**
     * The implementation for an installed mod, or the absent one.
     *
     * @param modPresent whether the other mod is installed
     * @param className the class in this mod that talks to the other mod; it has a constructor
     *     without parameters
     * @param absent what to use when the mod is not installed or its code cannot be loaded
     */
    static <T> T implementationOr(boolean modPresent, String className, Class<T> kind, T absent, Logger logger) {
        if (!modPresent) {
            return absent;
        }
        try {
            return kind.cast(Class.forName(className).getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException | LinkageError | ClassCastException problem) {
            // The other mod is there but differs from the version this code was written for.
            logger.error("Could not load {}; continuing as if the mod it needs were not installed", className, problem);
            return absent;
        }
    }
}
