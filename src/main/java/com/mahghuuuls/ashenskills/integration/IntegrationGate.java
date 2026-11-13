package com.mahghuuuls.ashenskills.integration;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import org.apache.logging.log4j.Logger;

/**
 * Decides, once while the game loads, what stands behind each gateway: the code for an installed
 * mod, or the behavior for its absence. Code that talks to another mod must be named here only as
 * text and loaded with {@link #implementationOr} after that mod was found, so this mod runs
 * without any of them. No other mod is connected yet: every gateway gets the absent behavior.
 */
public final class IntegrationGate {

    private static ResourceGateway resources;

    private IntegrationGate() {
    }

    /** Called once while the game loads, after the settings. */
    public static void decide(Logger logger) {
        resources = new ResourceGateway(
                ResourcePool.ABSENT,
                ResourcePool.ABSENT,
                () -> Settings.general().getBoolean(SettingKey.WAIVE_ABSENT_RESOURCE_COSTS));
        logger.info("Resource mods: stamina=absent mana=absent");
    }

    public static ResourceGateway resources() {
        return resources;
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
