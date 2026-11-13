package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** The tooltip of an Ash: what its skill does, or that no installed mod provides its skill. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class AshTooltip {

    private AshTooltip() {
    }

    @SubscribeEvent
    public static void describe(ItemTooltipEvent event) {
        String skillId = Ash.skillOf(event.getItemStack());
        if (skillId == null) {
            return;
        }
        ResourceLocation id = new ResourceLocation(skillId);
        if (!id.toString().equals(skillId) || SkillRegistry.get(id) == null) {
            event.getToolTip().add(TextFormatting.RED + I18n.format(Tags.MOD_ID + ".ash.unknown", skillId));
        } else if (I18n.hasKey(Ash.descriptionKey(skillId))) {
            event.getToolTip().add(TextFormatting.GRAY + I18n.format(Ash.descriptionKey(skillId)));
        }
    }
}
