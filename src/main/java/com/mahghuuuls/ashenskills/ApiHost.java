package com.mahghuuuls.ashenskills;

import com.mahghuuuls.ashenskills.api.AshenSkillsApi;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Takes what other mods add through {@link AshenSkillsApi} and hands it to the parts that own it. */
final class ApiHost implements AshenSkillsApi.Host {

    @Override
    public void registerSkill(SkillDefinition definition, SkillBehavior behavior) {
        if (definition == null) {
            throw new IllegalArgumentException("A skill needs a definition");
        }
        SkillRegistry.register(definition, behavior);
    }

    @Override
    public void registerWeaponType(String typeId) {
        WeaponTypes.added().addType(typeId);
    }

    @Override
    public void addItemType(ResourceLocation itemName, String typeId) {
        if (itemName == null) {
            throw new IllegalArgumentException("An item name is needed");
        }
        WeaponTypes.added().addItemType(itemName.toString(), typeId);
    }

    @Override
    public ItemStack ashOf(ResourceLocation skillId) {
        return Ash.of(skillId.toString());
    }
}
