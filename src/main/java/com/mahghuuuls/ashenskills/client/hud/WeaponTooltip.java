package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.client.gui.ForgeScreen;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.AshItem;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Adds to an item's tooltip what the mod knows about it as a weapon: its types and its skill. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class WeaponTooltip {

    private WeaponTooltip() {
    }

    @SubscribeEvent
    public static void describe(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || Ash.isAsh(stack)) {
            return;
        }
        AttachedSkill attached = AttachedSkill.of(stack);
        boolean askedInDetail = event.getFlags().isAdvanced() || Minecraft.getMinecraft().currentScreen instanceof ForgeScreen;
        for (TooltipLines.Line line : TooltipLines.of(
                WeaponTypes.effectiveTypes(stack), attached.status(), attached.skillId(), askedInDetail,
                Settings.general().getBoolean(SettingKey.NO_TYPE_TOOLTIP))) {
            event.getToolTip().add("\u00a7" + line.kind.colorCode + I18n.format(line.kind.languageKey(), shown(line)));
        }
    }

    /** A skill is shown by its name; a skill that no installed mod provides has no name, so its id is shown. */
    private static String shown(TooltipLines.Line line) {
        boolean namesASkill = line.kind == TooltipLines.Kind.SKILL || line.kind == TooltipLines.Kind.SKILL_INCOMPATIBLE;
        return namesASkill ? AshItem.skillName(line.detail) : line.detail;
    }
}
