package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.client.render.SkillIcons;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.config.GeneralSettings;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The skill HUD: beside the hotbar, the icon of the skill on the main-hand weapon in a frame that
 * tells whether the skill is ready, on cooldown, or cannot be used. It shows what the server
 * reported; it decides nothing. A player can switch it off, and the cooldown number apart from it,
 * and can move and resize it. It is drawn with the hotbar, so it is hidden whenever the hotbar is.
 */
public final class SkillHud {

    private static final ResourceLocation FRAMES = new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_skill_frame.png");

    // The frame sheet: three frames side by side, the icon place inside each.
    private static final int FRAME_SIZE = 24;
    private static final int SHEET_WIDTH = 72;
    private static final int ICON_INSET = 4;
    private static final int ICON_SIZE = SkillIcons.SIZE;

    // Where the frame sits: beside the hotbar, past the game's attack indicator.
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int INDICATOR_GAP = 6;
    private static final int INDICATOR_WIDTH = 18;
    private static final int GAP_AFTER_INDICATOR = 4;
    private static final int OFFSET_FROM_HOTBAR = INDICATOR_GAP + INDICATOR_WIDTH + GAP_AFTER_INDICATOR;

    private static final int SHADE = 0xA0000000;
    private static final int NUMBER_COLOR = 0xFFFFFF;

    /** What was last written to the diagnostic log, so each change is logged once. */
    private String lastLogged = "";

    private SkillHud() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SkillHud());
    }

    @SubscribeEvent
    public void afterHotbar(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }
        Minecraft game = Minecraft.getMinecraft();
        if (game.player == null || game.playerController == null || game.playerController.isSpectator()) {
            return;
        }
        if (!Settings.general().getBoolean(SettingKey.SKILL_HUD)) {
            logOnce(HudState.HIDDEN, "switchedOff", null, 0, null);
            return;
        }
        AttachedSkill attached = AttachedSkill.of(game.player.getHeldItemMainhand());
        long length = ClientSkillState.ownCooldownLengthMillis();
        // Never more than the whole cooldown, whatever this computer's clock did since the report.
        long remaining = Math.min(ClientSkillState.ownCooldownRemainingMillis(), Math.max(0, length));
        HudState state = HudState.of(attached.status() != AttachedSkill.Status.NONE, attached.isUsable(), remaining);
        if (state == HudState.HIDDEN) {
            logOnce(state, "noSkill", attached.skillId(), 0, null);
            return;
        }
        HudPlacement place = frame(event.getResolution(), game.player.getPrimaryHand());
        logOnce(state, "shown", attached.skillId(), HudState.secondsShown(remaining), place);
        GlStateManager.pushMatrix();
        GlStateManager.translate(place.x, place.y, 0);
        GlStateManager.scale(place.scale, place.scale, 1);
        draw(game, state, attached.skillId(), remaining, length);
        GlStateManager.popMatrix();
    }

    /** Where the frame is drawn: beside the hotbar on the side of the main hand, then as the player set it. */
    static HudPlacement frame(ScaledResolution screen, EnumHandSide mainHand) {
        int centerX = screen.getScaledWidth() / 2;
        int x = mainHand == EnumHandSide.RIGHT
                ? centerX + HOTBAR_HALF_WIDTH + OFFSET_FROM_HOTBAR
                : centerX - HOTBAR_HALF_WIDTH - OFFSET_FROM_HOTBAR - FRAME_SIZE;
        return placed(screen, x, screen.getScaledHeight() - FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
    }

    /**
     * Where something that belongs to the skill HUD is drawn: the frame itself, and the message
     * that says why a skill did not start. Both follow the player's skill HUD offset and scale.
     */
    static HudPlacement placed(ScaledResolution screen, double normalX, double normalY, int width, int height) {
        GeneralSettings settings = Settings.general();
        return HudPlacement.of(screen.getScaledWidth(), screen.getScaledHeight(), normalX, normalY, width, height,
                settings.getInt(SettingKey.SKILL_HUD_OFFSET_X), settings.getInt(SettingKey.SKILL_HUD_OFFSET_Y),
                settings.getDouble(SettingKey.SKILL_HUD_SCALE));
    }

    /** Draws the frame with its top left corner at the origin; the caller has moved and scaled the origin. */
    private static void draw(Minecraft game, HudState state, String skillId, long remaining, long length) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        game.getTextureManager().bindTexture(FRAMES);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, state.frame() * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE, SHEET_WIDTH, FRAME_SIZE);

        SkillIcons.draw(skillId, ICON_INSET, ICON_INSET);

        if (state != HudState.READY) {
            // A skill that cannot be used has its whole icon dimmed; a cooldown uncovers the icon from the bottom up.
            int rows = state == HudState.UNUSABLE ? ICON_SIZE : HudState.rowsCovered(remaining, length, ICON_SIZE);
            Gui.drawRect(ICON_INSET, ICON_INSET, ICON_INSET + ICON_SIZE, ICON_INSET + rows, SHADE);
            if (state == HudState.ON_COOLDOWN && Settings.general().getBoolean(SettingKey.COOLDOWN_NUMBER)) {
                FontRenderer font = game.fontRenderer;
                String seconds = String.valueOf(HudState.secondsShown(remaining));
                font.drawStringWithShadow(seconds, (FRAME_SIZE - font.getStringWidth(seconds)) / 2.0F,
                        (FRAME_SIZE - font.FONT_HEIGHT) / 2.0F + 1, NUMBER_COLOR);
            }
        }
        // Leave the drawing state as the game's own hotbar leaves it, whichever state was drawn.
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void logOnce(HudState state, String reason, String skillId, long seconds, HudPlacement place) {
        if (!Diagnostics.enabled()) {
            return;
        }
        boolean numberShown = state == HudState.ON_COOLDOWN && Settings.general().getBoolean(SettingKey.COOLDOWN_NUMBER);
        String where = place == null ? "none" : place.describe();
        String now = state + "|" + reason + "|" + skillId + "|" + (numberShown ? seconds : -1) + "|" + where;
        if (!now.equals(lastLogged)) {
            lastLogged = now;
            Diagnostics.log("CLIENT_HUD", "state={} reason={} skill={} number={} drawnAt={}",
                    state, reason, skillId, numberShown ? String.valueOf(seconds) : "none", where);
        }
    }
}
