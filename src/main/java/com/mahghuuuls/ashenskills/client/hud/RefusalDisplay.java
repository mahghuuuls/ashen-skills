package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;

/** Shows why a skill did not start, as a short line above the hotbar, never in chat. */
public final class RefusalDisplay {

    private static final long NANOS_PER_MILLI = 1_000_000;

    private static final Map<String, Long> LAST_SHOWN_MILLIS = new HashMap<>();

    private RefusalDisplay() {
    }

    /** Shows the reason unless the same reason was shown within the configured interval. */
    public static void show(String messageKey) {
        // A clock that only runs forward: a change of the computer's time must not silence the messages.
        long now = System.nanoTime() / NANOS_PER_MILLI;
        long intervalMillis = Math.round(Settings.general().getDouble(SettingKey.FAILURE_MESSAGE_MIN_INTERVAL) * 1000);
        boolean shown = isDue(LAST_SHOWN_MILLIS.get(messageKey), now, intervalMillis);
        Diagnostics.log("CLIENT_REFUSAL", "message={} shown={}", messageKey, shown);
        if (!shown) {
            return;
        }
        LAST_SHOWN_MILLIS.put(messageKey, now);
        Minecraft.getMinecraft().ingameGUI.setOverlayMessage(new TextComponentTranslation(messageKey), false);
    }

    /**
     * @param lastShownMillis when this reason was last shown, or null when it never was
     * @return whether the reason may show again at {@code nowMillis}
     */
    static boolean isDue(Long lastShownMillis, long nowMillis, long intervalMillis) {
        return lastShownMillis == null || nowMillis - lastShownMillis >= intervalMillis;
    }
}
