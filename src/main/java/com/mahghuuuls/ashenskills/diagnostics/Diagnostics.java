package com.mahghuuuls.ashenskills.diagnostics;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional log of what the mod decided and why. Off by default. Callers write at state changes
 * (activation, phase change, hit window), never per tick or per frame.
 */
public final class Diagnostics {

    private static final Logger LOGGER = LogManager.getLogger("Ashen Skills Diagnostics");

    private Diagnostics() {
    }

    /** False until the configuration has been read, so early callers are silent instead of failing. */
    public static boolean enabled() {
        return Settings.general() != null && Settings.general().getBoolean(SettingKey.DIAGNOSTIC_LOGGING);
    }

    /**
     * @param event short upper-case name of what happened, for example ACTIVATION
     * @param format Log4j message with {} placeholders
     */
    public static void log(String event, String format, Object... args) {
        if (enabled()) {
            LOGGER.info("[" + event + "] " + format, args);
        }
    }
}
