package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.ChargeCurve;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.animation.clip.ClipSampler;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState.PhaseView;
import com.mahghuuuls.ashenskills.config.GeneralSettings;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The charge bar: under the crosshair while the own player charges a skill, a bar that fills on
 * the way to the full charge, with a marker at the minimum charge and one at the full charge. It
 * shows how long the server said the charge has lasted, carried on by this client's clock between
 * reports; the server alone decides what a release is worth. A player can switch it off, and can
 * move and resize it.
 */
public final class ChargeBar {

    private static final byte PREPARATION = PhaseUpdateMessage.codeOf(SkillPhase.PREPARATION);
    private static final int WIDTH = 62;
    private static final int HEIGHT = 5;
    /** Below the game's own attack indicator, which sits right under the crosshair. */
    private static final int BELOW_CROSSHAIR = 28;
    private static final int MARKER_OVERHANG = 2;
    private static final int BORDER = 0xC0000000;
    private static final int EMPTY = 0x90303030;
    /** Before the minimum: a release does nothing yet. */
    private static final int FILL_TOO_EARLY = 0xFF8A8A8A;
    private static final int FILL_CHARGING = 0xFFE0A030;
    private static final int FILL_FULL = 0xFFFFE070;
    private static final int MARKER = 0xFFFFFFFF;

    /** What was last written to the diagnostic log, so each step is logged once. */
    private String lastLogged = "";

    private ChargeBar() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ChargeBar());
    }

    @SubscribeEvent
    public void afterCrosshair(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            return;
        }
        Minecraft game = Minecraft.getMinecraft();
        // Asked once: a report can go stale between two questions.
        PhaseView phase = game.player == null ? null : ownPreparation(game.player.getEntityId());
        ChargeCurve curve = phase == null ? null : curveOf(phase);
        if (curve == null) {
            logOnce("hidden", "noCharge", 0, 0, null);
            return;
        }
        if (!Settings.general().getBoolean(SettingKey.CHARGE_BAR)) {
            logOnce("hidden", "switchedOff", 0, 0, null);
            return;
        }
        double seconds = ClipSampler.secondsIntoPhase(
                phase.elapsedTicksAtReport, System.currentTimeMillis() - phase.reportedAtMillis);
        int inner = WIDTH - 2;
        int filled = (int) Math.round(curve.progress(seconds) * inner);
        int minimumAt = (int) Math.round(curve.minimumProgress() * inner);
        int color = curve.isFull(seconds) ? FILL_FULL : curve.reachesMinimum(seconds) ? FILL_CHARGING : FILL_TOO_EARLY;
        int x = (event.getResolution().getScaledWidth() - WIDTH) / 2;
        int y = event.getResolution().getScaledHeight() / 2 + BELOW_CROSSHAIR;
        // The markers stand out above and below the bar, and stay on the screen with it.
        GeneralSettings settings = Settings.general();
        HudPlacement place = HudPlacement.of(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(),
                x, y - MARKER_OVERHANG, WIDTH, HEIGHT + 2 * MARKER_OVERHANG,
                settings.getInt(SettingKey.CHARGE_BAR_OFFSET_X), settings.getInt(SettingKey.CHARGE_BAR_OFFSET_Y),
                settings.getDouble(SettingKey.CHARGE_BAR_SCALE));
        GlStateManager.pushMatrix();
        GlStateManager.translate(place.x, place.y, 0);
        GlStateManager.scale(place.scale, place.scale, 1);
        draw(inner, filled, minimumAt, color);
        GlStateManager.popMatrix();
        logOnce("shown", curve.isFull(seconds) ? "full" : curve.reachesMinimum(seconds) ? "charging" : "tooEarly",
                filled, minimumAt, place);
    }

    /** The own player's preparation as last reported, or null when the player prepares no skill. */
    private static PhaseView ownPreparation(int ownEntityId) {
        if (!ClientSkillState.hasAnyPhase()) {
            return null;
        }
        PhaseView phase = ClientSkillState.phaseOf(ownEntityId);
        return phase == null || phase.phaseCode != PREPARATION ? null : phase;
    }

    /** The curve of the skill that preparation belongs to, or null when it is not a charged skill. */
    private static ChargeCurve curveOf(PhaseView preparation) {
        SkillDefinition skill = SkillRegistry.get(new ResourceLocation(preparation.skillId));
        return skill == null || skill.activation() != ActivationKind.CHARGED ? null : ChargeCurve.of(skill);
    }

    /**
     * Draws the bar with the top of its markers at the origin; the caller has moved and scaled
     * the origin.
     */
    private static void draw(int inner, int filled, int minimumAt, int fillColor) {
        int x = 0;
        int y = MARKER_OVERHANG;
        // Each rectangle sets its own blending and switches blending off when it is done.
        Gui.drawRect(x, y, x + WIDTH, y + HEIGHT, BORDER);
        Gui.drawRect(x + 1, y + 1, x + 1 + inner, y + HEIGHT - 1, EMPTY);
        Gui.drawRect(x + 1, y + 1, x + 1 + filled, y + HEIGHT - 1, fillColor);
        // The minimum marker stands inside the bar; the full marker is the bar's own right end.
        Gui.drawRect(x + minimumAt, y - MARKER_OVERHANG, x + minimumAt + 1, y + HEIGHT + MARKER_OVERHANG, MARKER);
        Gui.drawRect(x + WIDTH - 1, y - MARKER_OVERHANG, x + WIDTH, y + HEIGHT + MARKER_OVERHANG, MARKER);
        // What is drawn after the crosshair expects blending on, white, and the game's icons sheet bound.
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.ICONS);
    }

    /** One line per tenth of the bar and per change of state, not one per frame. */
    private void logOnce(String state, String reason, int filled, int minimumAt, HudPlacement place) {
        if (!Diagnostics.enabled()) {
            return;
        }
        int tenth = filled * 10 / (WIDTH - 2);
        String where = place == null ? "none" : place.describe();
        String now = state + "|" + reason + "|" + tenth + "|" + where;
        if (!now.equals(lastLogged)) {
            lastLogged = now;
            Diagnostics.log("CLIENT_CHARGE_BAR", "state={} reason={} filledPixels={} ofPixels={} minimumAtPixel={} drawnAt={}",
                    state, reason, filled, WIDTH - 2, minimumAt, where);
        }
    }
}
