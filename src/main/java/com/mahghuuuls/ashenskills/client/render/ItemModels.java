package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.AshenContent;
import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * How the mod's items look. Every Ash is the same item, so its icon is chosen from the skill it
 * holds: the skill {@code <namespace>:<name>} uses the item model {@code <namespace>:ash_<name>}.
 * An Ash of a skill that was not installed when the game loaded its models shows the unknown icon.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ItemModels {

    private static final ModelResourceLocation UNKNOWN_ASH =
            new ModelResourceLocation(new ResourceLocation(Tags.MOD_ID, "ash_unknown"), "inventory");

    /** Skill id text to icon, for the skills known when the models were loaded. */
    private static final Map<String, ModelResourceLocation> ASH_ICONS = new HashMap<>();

    private ItemModels() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(AshenContent.FORGE_ITEM, 0,
                new ModelResourceLocation(AshenContent.FORGE_ITEM.getRegistryName(), "inventory"));
        ModelBakery.registerItemVariants(Ash.item(), UNKNOWN_ASH);
        for (ResourceLocation id : SkillRegistry.registered().keySet()) {
            ModelResourceLocation icon = new ModelResourceLocation(
                    new ResourceLocation(id.getNamespace(), "ash_" + id.getPath()), "inventory");
            ASH_ICONS.put(id.toString(), icon);
            ModelBakery.registerItemVariants(Ash.item(), icon);
        }
        ModelLoader.setCustomMeshDefinition(Ash.item(), stack -> {
            ModelResourceLocation icon = ASH_ICONS.get(Ash.skillOf(stack));
            return icon == null ? UNKNOWN_ASH : icon;
        });
    }
}
