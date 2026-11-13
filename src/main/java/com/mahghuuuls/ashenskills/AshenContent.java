package com.mahghuuuls.ashenskills;

import com.mahghuuuls.ashenskills.ashenforge.AshenForgeBlock;
import com.mahghuuuls.ashenskills.ashenforge.AshenForgeTile;
import com.mahghuuuls.ashenskills.ashenforge.ForgeActions;
import com.mahghuuuls.ashenskills.ashenforge.ForgeGuiHandler;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.skill.Ash;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

/** The block and the items the mod adds to the game, and where they are registered. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class AshenContent {

    public static final AshenForgeBlock FORGE = new AshenForgeBlock();
    public static final Item FORGE_ITEM = new ItemBlock(FORGE).setRegistryName(FORGE.getRegistryName());

    /** The mod's Creative tab: the Forge, and one Ash for every installed skill. */
    public static final CreativeTabs TAB = new CreativeTabs(Tags.MOD_ID) {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(FORGE_ITEM);
        }
    };

    private AshenContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        FORGE.setCreativeTab(TAB);
        event.getRegistry().register(FORGE);
        GameRegistry.registerTileEntity(AshenForgeTile.class, new ResourceLocation(Tags.MOD_ID, "ashen_forge"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Ash.item().setCreativeTab(TAB);
        event.getRegistry().registerAll(FORGE_ITEM, Ash.item());
    }

    /**
     * The Forge's recipe. It is always registered, because a server and its clients must know the
     * same recipes; the server's setting decides whether the pattern crafts anything. No Ash has a
     * recipe.
     */
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(FORGE_ITEM),
                "IBI", "OAO", "SSS",
                'I', Items.IRON_INGOT,
                'B', Items.BLAZE_ROD,
                'O', Blocks.OBSIDIAN,
                // an anvil in any state of damage
                'A', new ItemStack(Blocks.ANVIL, 1, OreDictionary.WILDCARD_VALUE),
                // plain stone bricks, not mossy, cracked or chiseled
                'S', new ItemStack(Blocks.STONEBRICK, 1, 0)) {
            @Override
            public boolean matches(InventoryCrafting grid, World world) {
                return Settings.general().getBoolean(SettingKey.FORGE_RECIPE_ENABLED) && super.matches(grid, world);
            }
        }.setRegistryName(new ResourceLocation(Tags.MOD_ID, "ashen_forge")));
    }

    /** Called once while the game loads, after the registrations above. */
    static void init() {
        NetworkRegistry.INSTANCE.registerGuiHandler(AshenSkillsMod.instance, new ForgeGuiHandler());
        ForgeActions.install();
    }
}
