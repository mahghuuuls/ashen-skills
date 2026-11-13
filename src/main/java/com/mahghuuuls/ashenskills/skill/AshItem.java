package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

/** The Ash item. Use {@link Ash} to make and read Ashes. */
@SuppressWarnings("deprecation") // the game's own item names use this translation class on both sides
public final class AshItem extends Item {

    AshItem() {
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "ash"));
        setTranslationKey(Tags.MOD_ID + ".ash");
    }

    /** The Creative tab shows one Ash for every skill that is installed. */
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        for (SkillDefinition skill : SkillRegistry.all()) {
            items.add(Ash.of(skill.id().toString()));
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String skillId = Ash.skillOf(stack);
        if (skillId == null) {
            return super.getItemStackDisplayName(stack);
        }
        return I18n.translateToLocalFormatted("item." + Tags.MOD_ID + ".ash.of", skillName(skillId));
    }

    /** The skill's name in the player's language, or its id when no installed mod names it. */
    public static String skillName(String skillId) {
        // Only the exact registered spelling names a skill; any other text is shown as it is.
        if (!new ResourceLocation(skillId).toString().equals(skillId)) {
            return skillId;
        }
        String key = Ash.nameKey(skillId);
        return I18n.canTranslate(key) ? I18n.translateToLocal(key) : skillId;
    }
}
