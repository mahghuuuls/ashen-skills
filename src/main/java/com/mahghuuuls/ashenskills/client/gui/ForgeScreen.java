package com.mahghuuuls.ashenskills.client.gui;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.ashenforge.ForgeContainer;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.ForgeActionMessage;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.AshItem;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.SkillTag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

/**
 * The two views of an open Ashen Forge. The weapon view shows the Forge's weapon slot, the skills
 * the Forge offers for that weapon, and two small buttons that put the chosen skill on the weapon
 * or take the weapon's skill off. What a skill does is its tooltip in the list. The storage view
 * shows the Ashes. Switching views moves no item: both views are one open container, and a slot
 * that a view does not show is only moved off the screen.
 */
public final class ForgeScreen extends GuiContainer {

    private static final ResourceLocation WEAPON_VIEW = new ResourceLocation(Tags.MOD_ID, "textures/gui/forge_customize.png");
    private static final ResourceLocation STORAGE_VIEW = new ResourceLocation(Tags.MOD_ID, "textures/gui/forge_storage.png");

    private static final int OFF_SCREEN = -10000;
    private static final int TEXT_COLOR = 0x404040;
    private static final int WINDOW_COLOR = 0xFFC6C6C6;
    private static final int SELECTED_ROW_COLOR = 0xFFE4E4E4;
    private static final int HOVERED_ROW_COLOR = 0xFFA6A6A6;

    // Places inside the window, as the two sheets are drawn.
    private static final int WEAPON_X = 8;
    private static final int WEAPON_Y = 18;
    private static final int LIST_X = 30;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 131;
    private static final int ROW_HEIGHT = 20;
    private static final int ROWS_SHOWN = 5;
    private static final int SCROLL_X = 164;
    private static final int SCROLL_TRACK_HEIGHT = 101;
    private static final int THUMB_HEIGHT = 15;
    private static final int TOOLTIP_WIDTH = 180;
    private static final int SWITCH_X = 152;
    private static final int SWITCH_Y = 4;
    private static final int SWITCH_WIDTH = 18;
    private static final int SWITCH_HEIGHT = 12;
    private static final int PAGE_BACK_X = 126;
    private static final int PAGE_NEXT_X = 138;
    private static final int PAGE_WIDTH = 10;
    // The two small buttons under the weapon slot; they have the size of the switch button.
    private static final int ACTION_X = 7;
    private static final int APPLY_Y = 38;
    private static final int REMOVE_Y = 54;
    // Sprites beside the window on the weapon sheet.
    private static final int APPLY_SPRITE_X = 176;
    private static final int REMOVE_SPRITE_X = 196;
    private static final int ACTION_SPRITE_Y = 100;
    private static final int ACTION_SPRITE_STEP = 14;
    private static final int OUTLINE_SPRITE_X = 176;
    private static final int MARK_SPRITE_X = 196;
    private static final int SMALL_SPRITE_Y = 144;
    private static final int MARK_SIZE = 7;
    private static final int NOTICE_COLOR = 0x7C1A16;
    private static final int NOTICE_TICKS = 80;

    private final ForgeContainer forge;
    private boolean storageView;
    private int page;
    private int firstRow;
    private String chosenSkill;
    private List<String> offered = Collections.emptyList();
    private boolean draggingThumb;
    private String notice;
    private int noticeTicksLeft;

    public ForgeScreen(ForgeContainer forge) {
        super(forge);
        this.forge = forge;
        xSize = 176;
        ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        placeSlots();
    }

    /** Puts the slots of the shown view at their places and the others off the screen. */
    private void placeSlots() {
        Slot weapon = forge.getSlot(ForgeContainer.WEAPON_SLOT);
        weapon.xPos = storageView ? OFF_SCREEN : WEAPON_X;
        weapon.yPos = storageView ? OFF_SCREEN : WEAPON_Y;
        for (int slot = 0; slot < forge.storageSlots(); slot++) {
            Slot storage = forge.getSlot(ForgeContainer.FIRST_STORAGE_SLOT + slot);
            boolean shown = storageView && slot / ForgeContainer.STORAGE_SLOTS_PER_PAGE == page;
            int onPage = slot % ForgeContainer.STORAGE_SLOTS_PER_PAGE;
            storage.xPos = shown ? 8 + (onPage % ForgeContainer.STORAGE_COLUMNS) * 18 : OFF_SCREEN;
            storage.yPos = shown ? 18 + (onPage / ForgeContainer.STORAGE_COLUMNS) * 18 : OFF_SCREEN;
        }
    }

    private int pages() {
        return Math.max(1, (forge.storageSlots() + ForgeContainer.STORAGE_SLOTS_PER_PAGE - 1) / ForgeContainer.STORAGE_SLOTS_PER_PAGE);
    }

    /** A skill is chosen and the weapon does not carry it already. */
    private boolean canApply() {
        return chosenSkill != null && !chosenSkill.equals(SkillTag.getSkillId(forge.weapon()));
    }

    private boolean storageIsEmpty() {
        for (int slot = 0; slot < forge.storageSlots(); slot++) {
            if (forge.getSlot(ForgeContainer.FIRST_STORAGE_SLOT + slot).getHasStack()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows a short message inside the window for a few seconds. The server's refusal of an apply
     * arrives as a line above the hotbar, which this window covers.
     */
    public void showNotice(String text) {
        notice = text;
        noticeTicksLeft = NOTICE_TICKS;
    }

    private boolean canRemove() {
        return SkillTag.getSkillId(forge.weapon()) != null;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Worked out again every tick: another player may change the storage or the weapon at any moment.
        offered = forge.offeredSkills();
        if (chosenSkill != null && !offered.contains(chosenSkill)) {
            chosenSkill = null;
        }
        firstRow = Math.max(0, Math.min(firstRow, offered.size() - ROWS_SHOWN));
        if (noticeTicksLeft > 0 && --noticeTicksLeft == 0) {
            notice = null;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        drawOwnTooltips(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(storageView ? STORAGE_VIEW : WEAPON_VIEW);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        boolean overSwitch = isOver(mouseX, mouseY, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT);
        drawTexturedModalRect(guiLeft + SWITCH_X, guiTop + SWITCH_Y, storageView ? 196 : 176, overSwitch ? 36 : 20,
                SWITCH_WIDTH, SWITCH_HEIGHT);
        if (storageView) {
            drawStorageBackground(mouseX, mouseY);
        } else {
            drawWeaponBackground(mouseX, mouseY);
        }
    }

    private void drawStorageBackground(int mouseX, int mouseY) {
        if (pages() > 1) {
            drawPageArrow(PAGE_BACK_X, 176, page > 0, mouseX, mouseY);
            drawPageArrow(PAGE_NEXT_X, 188, page < pages() - 1, mouseX, mouseY);
        }
        // The sheet shows a full page; cover the places that have no slot on this page.
        int onThisPage = Math.min(ForgeContainer.STORAGE_SLOTS_PER_PAGE,
                forge.storageSlots() - page * ForgeContainer.STORAGE_SLOTS_PER_PAGE);
        for (int place = onThisPage; place < ForgeContainer.STORAGE_SLOTS_PER_PAGE; place++) {
            int x = guiLeft + 7 + (place % ForgeContainer.STORAGE_COLUMNS) * 18;
            int y = guiTop + 17 + (place / ForgeContainer.STORAGE_COLUMNS) * 18;
            drawRect(x, y, x + 18, y + 18, WINDOW_COLOR);
        }
    }

    private void drawPageArrow(int x, int textureX, boolean usable, int mouseX, int mouseY) {
        int textureY = !usable ? 84 : isOver(mouseX, mouseY, x, SWITCH_Y, PAGE_WIDTH, SWITCH_HEIGHT) ? 70 : 56;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(STORAGE_VIEW);
        drawTexturedModalRect(guiLeft + x, guiTop + SWITCH_Y, textureX, textureY, PAGE_WIDTH, SWITCH_HEIGHT);
    }

    private void drawWeaponBackground(int mouseX, int mouseY) {
        if (forge.weapon().isEmpty()) {
            // A faint weapon shape says what the empty slot is for.
            drawTexturedModalRect(guiLeft + WEAPON_X, guiTop + WEAPON_Y, OUTLINE_SPRITE_X, SMALL_SPRITE_Y, 16, 16);
        }
        drawAction(APPLY_SPRITE_X, APPLY_Y, canApply(), mouseX, mouseY);
        drawAction(REMOVE_SPRITE_X, REMOVE_Y, canRemove(), mouseX, mouseY);
        int maxFirstRow = Math.max(0, offered.size() - ROWS_SHOWN);
        int thumbY = maxFirstRow == 0 ? 0 : (SCROLL_TRACK_HEIGHT - THUMB_HEIGHT) * firstRow / maxFirstRow;
        drawTexturedModalRect(guiLeft + SCROLL_X, guiTop + LIST_Y + thumbY, maxFirstRow == 0 ? 184 : 176, 0, 5, THUMB_HEIGHT);
        int hovered = rowAt(mouseX, mouseY);
        String carried = SkillTag.getSkillId(forge.weapon());
        for (int row = 0; row < ROWS_SHOWN && firstRow + row < offered.size(); row++) {
            String skillId = offered.get(firstRow + row);
            int x = guiLeft + LIST_X;
            int y = guiTop + LIST_Y + row * ROW_HEIGHT;
            if (skillId.equals(chosenSkill)) {
                drawRect(x, y, x + LIST_WIDTH, y + ROW_HEIGHT, SELECTED_ROW_COLOR);
            } else if (row == hovered) {
                drawRect(x, y, x + LIST_WIDTH, y + ROW_HEIGHT, HOVERED_ROW_COLOR);
            }
            drawAsh(skillId, x + 2, y + 2);
            boolean onWeapon = skillId.equals(carried);
            int nameWidth = LIST_WIDTH - 24 - (onWeapon ? MARK_SIZE + 4 : 0);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(AshItem.skillName(skillId), nameWidth), x + 22, y + 6, TEXT_COLOR);
            if (onWeapon) {
                // A small weapon mark: this is the skill the weapon carries now.
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(WEAPON_VIEW);
                drawTexturedModalRect(x + LIST_WIDTH - MARK_SIZE - 3, y + 6, MARK_SPRITE_X, SMALL_SPRITE_Y, MARK_SIZE, MARK_SIZE);
            }
        }
        if (offered.isEmpty()) {
            // Two different reasons for an empty list get two different texts.
            String hint = storageIsEmpty() ? "gui.ashenskills.forge.nothing_stored"
                    : forge.weapon().isEmpty() ? null : "gui.ashenskills.forge.nothing_offered";
            if (hint != null) {
                fontRenderer.drawSplitString(I18n.format(hint), guiLeft + LIST_X + 3, guiTop + LIST_Y + 4, LIST_WIDTH - 6, TEXT_COLOR);
            }
        }
        if (notice != null) {
            // On its own ground over the last row, so a row under it does not show through.
            int top = guiTop + LIST_Y + (ROWS_SHOWN - 1) * ROW_HEIGHT;
            drawRect(guiLeft + LIST_X, top, guiLeft + LIST_X + LIST_WIDTH, top + ROW_HEIGHT, WINDOW_COLOR);
            List<String> lines = fontRenderer.listFormattedStringToWidth(notice, LIST_WIDTH - 6);
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                fontRenderer.drawString(lines.get(line), guiLeft + LIST_X + 3, top + 1 + line * 9, NOTICE_COLOR);
            }
        }
    }

    /** One of the two small buttons: plain, lit under the mouse, or greyed when it can do nothing now. */
    private void drawAction(int spriteX, int y, boolean usable, int mouseX, int mouseY) {
        int state = !usable ? 2 : isOver(mouseX, mouseY, ACTION_X, y, SWITCH_WIDTH, SWITCH_HEIGHT) ? 1 : 0;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(WEAPON_VIEW);
        drawTexturedModalRect(guiLeft + ACTION_X, guiTop + y, spriteX, ACTION_SPRITE_Y + state * ACTION_SPRITE_STEP,
                SWITCH_WIDTH, SWITCH_HEIGHT);
    }

    private void drawAsh(String skillId, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(Ash.of(skillId), x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(storageView ? "container.ashenskills.forge.storage" : "container.ashenskills.forge");
        fontRenderer.drawString(title, 8, 6, TEXT_COLOR);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 128, TEXT_COLOR);
        if (storageView && pages() > 1) {
            String pageText = (page + 1) + "/" + pages();
            fontRenderer.drawString(pageText, PAGE_BACK_X - 3 - fontRenderer.getStringWidth(pageText), 6, TEXT_COLOR);
        }
    }

    private void drawOwnTooltips(int mouseX, int mouseY) {
        if (isOver(mouseX, mouseY, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            drawHoveringText(I18n.format(storageView ? "gui.ashenskills.forge.to_weapon" : "gui.ashenskills.forge.to_storage"), mouseX, mouseY);
            return;
        }
        if (storageView || !mc.player.inventory.getItemStack().isEmpty()) {
            return;
        }
        if (isOver(mouseX, mouseY, ACTION_X, APPLY_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            drawHoveringText(I18n.format(forge.weapon().isEmpty() ? "gui.ashenskills.forge.no_weapon"
                    : canApply() ? "gui.ashenskills.forge.apply"
                    : chosenSkill != null ? "gui.ashenskills.forge.apply_already"
                    : "gui.ashenskills.forge.apply_unavailable"), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, ACTION_X, REMOVE_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            String carried = SkillTag.getSkillId(forge.weapon());
            drawHoveringText(forge.weapon().isEmpty() ? I18n.format("gui.ashenskills.forge.no_weapon")
                    : carried == null ? I18n.format("gui.ashenskills.forge.remove_unavailable")
                    : I18n.format("gui.ashenskills.forge.remove", AshItem.skillName(carried)), mouseX, mouseY);
        } else {
            int row = rowAt(mouseX, mouseY);
            if (row >= 0 && firstRow + row < offered.size()) {
                drawHoveringText(skillTooltip(offered.get(firstRow + row)), mouseX, mouseY);
            }
        }
    }

    /** Name, description, cooldown and costs of a skill in the list, each kind in its own color. */
    private List<String> skillTooltip(String skillId) {
        SkillDefinition skill = SkillRegistry.get(new ResourceLocation(skillId));
        String descriptionKey = Ash.descriptionKey(skillId);
        boolean described = I18n.hasKey(descriptionKey);
        List<String> text = new ArrayList<>();
        if (skill == null) {
            text.add("\u00a7" + SkillRowTooltip.Kind.NAME.colorCode + AshItem.skillName(skillId));
            return text;
        }
        List<SkillRowTooltip.Line> lines = SkillRowTooltip.of(
                described,
                skill.number(SkillNumber.COOLDOWN),
                skill.number(SkillNumber.STAMINA_COST),
                skill.number(SkillNumber.MANA_COST));
        for (SkillRowTooltip.Line line : lines) {
            String color = "\u00a7" + line.kind.colorCode;
            if (line.kind == SkillRowTooltip.Kind.NAME) {
                text.add(color + AshItem.skillName(skillId));
            } else if (line.kind == SkillRowTooltip.Kind.DESCRIPTION) {
                for (String part : fontRenderer.listFormattedStringToWidth(I18n.format(descriptionKey), TOOLTIP_WIDTH)) {
                    text.add(color + part);
                }
            } else {
                text.add(color + I18n.format(line.kind.languageKey(), SkillRowTooltip.shown(line.number)));
            }
        }
        return text;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && clickedOwnControl(mouseX, mouseY)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean clickedOwnControl(int mouseX, int mouseY) {
        if (isOver(mouseX, mouseY, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            storageView = !storageView;
            placeSlots();
            click();
            return true;
        }
        if (storageView) {
            return clickedPageArrow(mouseX, mouseY);
        }
        if (isOver(mouseX, mouseY, SCROLL_X - 1, LIST_Y, 7, SCROLL_TRACK_HEIGHT)) {
            draggingThumb = true;
            scrollToMouse(mouseY);
            return true;
        }
        if (isOver(mouseX, mouseY, ACTION_X, APPLY_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            if (canApply()) {
                AshenNetwork.channel().sendToServer(ForgeActionMessage.attach(forge.windowId, chosenSkill));
                click();
            }
            return true;
        }
        if (isOver(mouseX, mouseY, ACTION_X, REMOVE_Y, SWITCH_WIDTH, SWITCH_HEIGHT)) {
            if (canRemove()) {
                AshenNetwork.channel().sendToServer(ForgeActionMessage.detach(forge.windowId));
                click();
            }
            return true;
        }
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && firstRow + row < offered.size()) {
            // No button sound: choosing a row changes nothing yet. The apply button does.
            chosenSkill = offered.get(firstRow + row);
            return true;
        }
        return false;
    }

    /** Moves the list so that the thumb's middle is at the mouse, as far as the list allows. */
    private void scrollToMouse(int mouseY) {
        int maxFirstRow = Math.max(0, offered.size() - ROWS_SHOWN);
        int travel = SCROLL_TRACK_HEIGHT - THUMB_HEIGHT;
        int along = mouseY - guiTop - LIST_Y - THUMB_HEIGHT / 2;
        firstRow = maxFirstRow == 0 ? 0 : Math.max(0, Math.min(maxFirstRow, (along * maxFirstRow + travel / 2) / travel));
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingThumb) {
            scrollToMouse(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggingThumb) {
            draggingThumb = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean clickedPageArrow(int mouseX, int mouseY) {
        if (pages() <= 1) {
            return false;
        }
        if (isOver(mouseX, mouseY, PAGE_BACK_X, SWITCH_Y, PAGE_WIDTH, SWITCH_HEIGHT) && page > 0) {
            page--;
            placeSlots();
            click();
            return true;
        }
        if (isOver(mouseX, mouseY, PAGE_NEXT_X, SWITCH_Y, PAGE_WIDTH, SWITCH_HEIGHT) && page < pages() - 1) {
            page++;
            placeSlots();
            click();
            return true;
        }
        return false;
    }

    /** The game's own button click, so the small buttons answer like every other button. */
    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /**
     * A shift-click from the inventory goes only into a slot this view shows: an Ash into the
     * storage in the storage view, any other item into the weapon slot in the weapon view.
     */
    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (type == ClickType.QUICK_MOVE && slot != null
                && slotId >= ForgeContainer.FIRST_STORAGE_SLOT + forge.storageSlots()
                && storageView != Ash.isAsh(slot.getStack())) {
            return;
        }
        super.handleMouseClick(slot, slotId, mouseButton, type);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !storageView) {
            firstRow = Math.max(0, Math.min(firstRow + (wheel < 0 ? 1 : -1), Math.max(0, offered.size() - ROWS_SHOWN)));
        }
    }

    /** The list row under the mouse, or -1. */
    private int rowAt(int mouseX, int mouseY) {
        if (!isOver(mouseX, mouseY, LIST_X, LIST_Y, LIST_WIDTH, ROWS_SHOWN * ROW_HEIGHT)) {
            return -1;
        }
        return (mouseY - guiTop - LIST_Y) / ROW_HEIGHT;
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }
}
