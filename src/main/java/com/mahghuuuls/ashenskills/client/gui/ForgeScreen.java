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
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

/**
 * The two views of an open Ashen Forge. The weapon view shows the weapon input, the skill the
 * weapon carries, the skills the Forge offers for it, and what the chosen skill does. The storage
 * view shows the Ashes. Switching views moves no item: both views are one open container, and a
 * slot that a view does not show is only moved off the screen.
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
    private static final int LIST_X = 30;
    private static final int LIST_Y = 18;
    private static final int LIST_WIDTH = 131;
    private static final int ROW_HEIGHT = 20;
    private static final int ROWS_SHOWN = 3;
    private static final int SCROLL_X = 164;
    private static final int SCROLL_TRACK_HEIGHT = 61;
    private static final int THUMB_HEIGHT = 15;
    private static final int PREVIEW_X = 11;
    private static final int PREVIEW_Y = 85;
    private static final int PREVIEW_WIDTH = 155;
    private static final int CARRIED_X = 8;
    private static final int CARRIED_Y = 40;
    private static final int SWITCH_X = 152;
    private static final int SWITCH_Y = 4;
    private static final int SWITCH_WIDTH = 18;
    private static final int SWITCH_HEIGHT = 12;
    private static final int PAGE_BACK_X = 126;
    private static final int PAGE_NEXT_X = 138;
    private static final int PAGE_WIDTH = 10;

    private final ForgeContainer forge;
    private boolean storageView;
    private int page;
    private int firstRow;
    private String chosenSkill;
    private List<String> offered = Collections.emptyList();
    private GuiButton apply;
    private GuiButton remove;

    public ForgeScreen(ForgeContainer forge) {
        super(forge);
        this.forge = forge;
        xSize = 176;
        ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        apply = addButton(new GuiButton(0, guiLeft + 8, guiTop + 117, 60, 20, I18n.format("gui.ashenskills.forge.apply")));
        remove = addButton(new GuiButton(1, guiLeft + 70, guiTop + 117, 60, 20, I18n.format("gui.ashenskills.forge.remove")));
        placeSlots();
    }

    /** Puts the slots of the shown view at their places and the others off the screen. */
    private void placeSlots() {
        Slot weapon = forge.getSlot(ForgeContainer.WEAPON_SLOT);
        weapon.xPos = storageView ? OFF_SCREEN : 8;
        weapon.yPos = storageView ? OFF_SCREEN : 18;
        for (int slot = 0; slot < forge.storageSlots(); slot++) {
            Slot storage = forge.getSlot(ForgeContainer.FIRST_STORAGE_SLOT + slot);
            boolean shown = storageView && slot / ForgeContainer.STORAGE_SLOTS_PER_PAGE == page;
            int onPage = slot % ForgeContainer.STORAGE_SLOTS_PER_PAGE;
            storage.xPos = shown ? 8 + (onPage % ForgeContainer.STORAGE_COLUMNS) * 18 : OFF_SCREEN;
            storage.yPos = shown ? 18 + (onPage / ForgeContainer.STORAGE_COLUMNS) * 18 : OFF_SCREEN;
        }
        apply.visible = !storageView;
        remove.visible = !storageView;
    }

    private int pages() {
        return Math.max(1, (forge.storageSlots() + ForgeContainer.STORAGE_SLOTS_PER_PAGE - 1) / ForgeContainer.STORAGE_SLOTS_PER_PAGE);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Worked out again every tick: another player may change the storage at any moment.
        offered = forge.offeredSkills();
        if (chosenSkill != null && !offered.contains(chosenSkill)) {
            chosenSkill = null;
        }
        firstRow = Math.max(0, Math.min(firstRow, offered.size() - ROWS_SHOWN));
        apply.enabled = chosenSkill != null;
        remove.enabled = SkillTag.getSkillId(forge.weapon()) != null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == apply && chosenSkill != null) {
            AshenNetwork.channel().sendToServer(ForgeActionMessage.attach(forge.windowId, chosenSkill));
        } else if (button == remove) {
            AshenNetwork.channel().sendToServer(ForgeActionMessage.detach(forge.windowId));
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
        int maxFirstRow = Math.max(0, offered.size() - ROWS_SHOWN);
        int thumbY = maxFirstRow == 0 ? 0 : (SCROLL_TRACK_HEIGHT - THUMB_HEIGHT) * firstRow / maxFirstRow;
        drawTexturedModalRect(guiLeft + SCROLL_X, guiTop + LIST_Y + thumbY, maxFirstRow == 0 ? 184 : 176, 0, 5, THUMB_HEIGHT);
        int hovered = rowAt(mouseX, mouseY);
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
            fontRenderer.drawString(fontRenderer.trimStringToWidth(AshItem.skillName(skillId), LIST_WIDTH - 24), x + 22, y + 6, TEXT_COLOR);
        }
        if (offered.isEmpty()) {
            String hint = I18n.format(forge.weapon().isEmpty()
                    ? "gui.ashenskills.forge.insert_weapon"
                    : "gui.ashenskills.forge.nothing_offered");
            fontRenderer.drawSplitString(hint, guiLeft + LIST_X + 3, guiTop + LIST_Y + 4, LIST_WIDTH - 6, TEXT_COLOR);
        }
        String carried = SkillTag.getSkillId(forge.weapon());
        if (carried != null) {
            drawAsh(carried, guiLeft + CARRIED_X, guiTop + CARRIED_Y);
        }
        drawPreview();
    }

    private void drawAsh(String skillId, int x, int y) {
        RenderHelper.enableGUIStandardItemLighting();
        itemRender.renderItemAndEffectIntoGUI(Ash.of(skillId), x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Name, one line of description, and the costs of the chosen skill. */
    private void drawPreview() {
        SkillDefinition skill = chosenSkill == null ? null : SkillRegistry.get(new ResourceLocation(chosenSkill));
        if (skill == null) {
            return;
        }
        int x = guiLeft + PREVIEW_X;
        int y = guiTop + PREVIEW_Y;
        fontRenderer.drawString(fontRenderer.trimStringToWidth(AshItem.skillName(chosenSkill), PREVIEW_WIDTH), x, y, TEXT_COLOR);
        List<String> description = descriptionLines(chosenSkill);
        if (!description.isEmpty()) {
            String first = description.get(0) + (description.size() > 1 ? "..." : "");
            fontRenderer.drawString(first, x, y + 9, TEXT_COLOR);
        }
        fontRenderer.drawString(fontRenderer.trimStringToWidth(costLine(skill), PREVIEW_WIDTH), x, y + 18, TEXT_COLOR);
    }

    private List<String> descriptionLines(String skillId) {
        String key = Ash.descriptionKey(skillId);
        return I18n.hasKey(key)
                ? fontRenderer.listFormattedStringToWidth(I18n.format(key), PREVIEW_WIDTH - 10)
                : Collections.<String>emptyList();
    }

    private static String costLine(SkillDefinition skill) {
        List<String> parts = new ArrayList<>();
        parts.add(I18n.format("gui.ashenskills.forge.cooldown", number(skill.number(SkillNumber.COOLDOWN))));
        if (skill.number(SkillNumber.STAMINA_COST) > 0) {
            parts.add(I18n.format("gui.ashenskills.forge.stamina", number(skill.number(SkillNumber.STAMINA_COST))));
        }
        if (skill.number(SkillNumber.MANA_COST) > 0) {
            parts.add(I18n.format("gui.ashenskills.forge.mana", number(skill.number(SkillNumber.MANA_COST))));
        }
        return String.join("  ", parts);
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(storageView ? "container.ashenskills.forge.storage" : "container.ashenskills.forge");
        fontRenderer.drawString(title, 8, 6, TEXT_COLOR);
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
        if (storageView) {
            return;
        }
        String carried = SkillTag.getSkillId(forge.weapon());
        if (carried != null && isOver(mouseX, mouseY, CARRIED_X, CARRIED_Y, 16, 16)) {
            drawHoveringText(I18n.format("gui.ashenskills.forge.carried", AshItem.skillName(carried)), mouseX, mouseY);
        } else if (chosenSkill != null && isOver(mouseX, mouseY, PREVIEW_X - 3, PREVIEW_Y - 3, PREVIEW_WIDTH + 6, 31)) {
            List<String> lines = new ArrayList<>();
            lines.add(AshItem.skillName(chosenSkill));
            lines.addAll(fontRenderer.listFormattedStringToWidth(
                    I18n.hasKey(Ash.descriptionKey(chosenSkill)) ? I18n.format(Ash.descriptionKey(chosenSkill)) : "", 200));
            drawHoveringText(lines, mouseX, mouseY);
        }
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
            return true;
        }
        if (storageView && pages() > 1) {
            if (isOver(mouseX, mouseY, PAGE_BACK_X, SWITCH_Y, PAGE_WIDTH, SWITCH_HEIGHT) && page > 0) {
                page--;
                placeSlots();
                return true;
            }
            if (isOver(mouseX, mouseY, PAGE_NEXT_X, SWITCH_Y, PAGE_WIDTH, SWITCH_HEIGHT) && page < pages() - 1) {
                page++;
                placeSlots();
                return true;
            }
        }
        int row = storageView ? -1 : rowAt(mouseX, mouseY);
        if (row >= 0 && firstRow + row < offered.size()) {
            chosenSkill = offered.get(firstRow + row);
            return true;
        }
        return false;
    }

    /** In the storage view the weapon input is not shown, so a shift-click must not send an item there. */
    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (storageView && type == ClickType.QUICK_MOVE && slot != null
                && slotId >= ForgeContainer.FIRST_STORAGE_SLOT + forge.storageSlots() && !Ash.isAsh(slot.getStack())) {
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
