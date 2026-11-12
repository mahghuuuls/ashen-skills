package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentTranslation;

/** Shows why a skill did not start, as a short line above the hotbar, never in chat. */
public final class RefusalDisplay {

    private static final Map<String, Long> LAST_SHOWN_MILLIS = new HashMap<>();

    private RefusalDisplay() {
    }

    /** Shows the reason unless the same reason was shown within the configured interval. */
    public static void show(String messageKey) {
        long now = System.currentTimeMillis();
        long intervalMillis = Math.round(Settings.general().getDouble(SettingKey.FAILURE_MESSAGE_MIN_INTERVAL) * 1000);
        Long last = LAST_SHOWN_MILLIS.get(messageKey);
        if (last != null && now - last < intervalMillis) {
            return;
        }
        LAST_SHOWN_MILLIS.put(messageKey, now);
        Minecraft.getMinecraft().ingameGUI.setOverlayMessage(new TextComponentTranslation(messageKey), false);
    }
}
