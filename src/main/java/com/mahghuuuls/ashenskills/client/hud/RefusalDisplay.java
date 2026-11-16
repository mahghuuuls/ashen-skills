package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Shows why a skill did not start, as a short line above the hotbar, never in chat. The line looks
 * and fades like the game's own line in that place, but the mod draws it, because it follows the
 * player's skill HUD offset and scale and the game's own line cannot be moved.
 */
public final class RefusalDisplay {

    private static final long NANOS_PER_MILLI = 1_000_000;

    // As the game's own line above the hotbar: how long it stays, how it fades, where it sits.
    private static final int TICKS_SHOWN = 60;
    private static final float FADE_TICKS = 20.0F;
    private static final int ABOVE_BOTTOM = 72;
    private static final int TEXT_HEIGHT = 8;
    private static final int WHITE = 0xFFFFFF;

    private static final Map<String, Long> LAST_SHOWN_MILLIS = new HashMap<>();

    private static String message = "";
    private static int ticksLeft;

    private RefusalDisplay() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new RefusalDisplay());
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
        message = I18n.format(messageKey);
        ticksLeft = TICKS_SHOWN;
    }

    /**
     * @param lastShownMillis when this reason was last shown, or null when it never was
     * @return whether the reason may show again at {@code nowMillis}
     */
    static boolean isDue(Long lastShownMillis, long nowMillis, long intervalMillis) {
        return lastShownMillis == null || nowMillis - lastShownMillis >= intervalMillis;
    }

    /**
     * @return how opaque the line is, 0 to 255, with the given time left; it fades over its last second
     */
    static int opacity(float ticksLeft) {
        return Math.max(0, Math.min(255, (int) (ticksLeft * 256.0F / FADE_TICKS)));
    }

    @SubscribeEvent
    public void afterTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksLeft > 0 && !Minecraft.getMinecraft().isGamePaused()) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    public void afterHudText(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT || ticksLeft <= 0) {
            return;
        }
        int opacity = opacity(ticksLeft - event.getPartialTicks());
        if (opacity <= 0) {
            return;
        }
        Minecraft game = Minecraft.getMinecraft();
        if (game.player == null) {
            return;
        }
        FontRenderer font = game.fontRenderer;
        int width = font.getStringWidth(message);
        int normalX = event.getResolution().getScaledWidth() / 2 + -width / 2;
        int normalY = event.getResolution().getScaledHeight() - ABOVE_BOTTOM;
        // Pushed to the same screen edge as the frame, or much enlarged, the line would lie on the frame.
        // It keeps clear of the frame's place also while the frame is not shown.
        HudPlacement place = SkillHud.placed(event.getResolution(), normalX, normalY, width, TEXT_HEIGHT)
                .clearOf(SkillHud.frame(event.getResolution(), game.player.getPrimaryHand()),
                        event.getResolution().getScaledHeight());
        GlStateManager.pushMatrix();
        GlStateManager.translate(place.x, place.y, 0);
        GlStateManager.scale(place.scale, place.scale, 1);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        font.drawString(message, 0, 0, WHITE | opacity << 24);
        // The text left its fading color set; what is drawn next must not fade with it.
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
