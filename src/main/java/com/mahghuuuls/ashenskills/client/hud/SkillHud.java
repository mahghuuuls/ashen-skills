package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The skill HUD: beside the hotbar, the icon of the skill on the main-hand weapon in a frame that
 * tells whether the skill is ready, on cooldown, or cannot be used. It shows what the server
 * reported; it decides nothing. A player can switch it off, and the cooldown number apart from it.
 * It is drawn with the hotbar, so it is hidden whenever the hotbar is.
 */
public final class SkillHud {

    private static final ResourceLocation FRAMES = new ResourceLocation(Tags.MOD_ID, "textures/gui/hud_skill_frame.png");

    // The frame sheet: three frames side by side, the icon place inside each.
    private static final int FRAME_SIZE = 24;
    private static final int SHEET_WIDTH = 72;
    private static final int ICON_INSET = 4;
    private static final int ICON_SIZE = 16;

    // Where the frame sits: beside the hotbar, past the game's attack indicator.
    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int INDICATOR_GAP = 6;
    private static final int INDICATOR_WIDTH = 18;
    private static final int GAP_AFTER_INDICATOR = 4;
    private static final int OFFSET_FROM_HOTBAR = INDICATOR_GAP + INDICATOR_WIDTH + GAP_AFTER_INDICATOR;

    private static final int SHADE = 0xA0000000;
    private static final int NUMBER_COLOR = 0xFFFFFF;

    /** The Ash drawn as the icon, kept while the skill stays the same. */
    private String iconSkillId;
    private ItemStack icon = ItemStack.EMPTY;

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
            logOnce(HudState.HIDDEN, "switchedOff", null, 0);
            return;
        }
        AttachedSkill attached = AttachedSkill.of(game.player.getHeldItemMainhand());
        long length = ClientSkillState.ownCooldownLengthMillis();
        // Never more than the whole cooldown, whatever this computer's clock did since the report.
        long remaining = Math.min(ClientSkillState.ownCooldownRemainingMillis(), Math.max(0, length));
        HudState state = HudState.of(attached.status() != AttachedSkill.Status.NONE, attached.isUsable(), remaining);
        logOnce(state, state == HudState.HIDDEN ? "noSkill" : "shown", attached.skillId(), HudState.secondsShown(remaining));
        if (state == HudState.HIDDEN) {
            return;
        }
        boolean rightHanded = game.player.getPrimaryHand() == EnumHandSide.RIGHT;
        int centerX = event.getResolution().getScaledWidth() / 2;
        int x = rightHanded
                ? centerX + HOTBAR_HALF_WIDTH + OFFSET_FROM_HOTBAR
                : centerX - HOTBAR_HALF_WIDTH - OFFSET_FROM_HOTBAR - FRAME_SIZE;
        int y = event.getResolution().getScaledHeight() - FRAME_SIZE;
        draw(game, state, iconOf(attached.skillId()), remaining, length, x, y);
    }

    private ItemStack iconOf(String skillId) {
        if (!skillId.equals(iconSkillId)) {
            iconSkillId = skillId;
            icon = Ash.of(skillId);
        }
        return icon;
    }

    private static void draw(Minecraft game, HudState state, ItemStack icon, long remaining, long length, int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        game.getTextureManager().bindTexture(FRAMES);
        Gui.drawModalRectWithCustomSizedTexture(x, y, state.frame() * FRAME_SIZE, 0, FRAME_SIZE, FRAME_SIZE, SHEET_WIDTH, FRAME_SIZE);

        RenderHelper.enableGUIStandardItemLighting();
        game.getRenderItem().renderItemAndEffectIntoGUI(icon, x + ICON_INSET, y + ICON_INSET);
        RenderHelper.disableStandardItemLighting();

        if (state != HudState.READY) {
            // The shade and the number lie over the icon, which the game draws nearer to the viewer.
            GlStateManager.disableDepth();
            // A skill that cannot be used has its whole icon dimmed; a cooldown uncovers the icon from the bottom up.
            int rows = state == HudState.UNUSABLE ? ICON_SIZE : HudState.rowsCovered(remaining, length, ICON_SIZE);
            Gui.drawRect(x + ICON_INSET, y + ICON_INSET, x + ICON_INSET + ICON_SIZE, y + ICON_INSET + rows, SHADE);
            if (state == HudState.ON_COOLDOWN && Settings.general().getBoolean(SettingKey.COOLDOWN_NUMBER)) {
                FontRenderer font = game.fontRenderer;
                String seconds = String.valueOf(HudState.secondsShown(remaining));
                font.drawStringWithShadow(seconds, x + (FRAME_SIZE - font.getStringWidth(seconds)) / 2.0F,
                        y + (FRAME_SIZE - font.FONT_HEIGHT) / 2.0F + 1, NUMBER_COLOR);
            }
            GlStateManager.enableDepth();
        }
        // Leave the drawing state as the game's own hotbar leaves it, whichever state was drawn.
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void logOnce(HudState state, String reason, String skillId, long seconds) {
        if (!Diagnostics.enabled()) {
            return;
        }
        boolean numberShown = state == HudState.ON_COOLDOWN && Settings.general().getBoolean(SettingKey.COOLDOWN_NUMBER);
        String now = state + "|" + reason + "|" + skillId + "|" + (numberShown ? seconds : -1);
        if (!now.equals(lastLogged)) {
            lastLogged = now;
            Diagnostics.log("CLIENT_HUD", "state={} reason={} skill={} number={}",
                    state, reason, skillId, numberShown ? String.valueOf(seconds) : "none");
        }
    }
}
