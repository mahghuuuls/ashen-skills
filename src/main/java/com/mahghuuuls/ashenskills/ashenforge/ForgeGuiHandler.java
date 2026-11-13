package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** Opens the Forge for a player: the container on the server, the container and its screens on the client. */
public final class ForgeGuiHandler implements IGuiHandler {

    public static final int FORGE_SCREENS = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AshenForgeTile forge = forgeAt(world, x, y, z);
        return id == FORGE_SCREENS && forge != null ? new ForgeContainer(player.inventory, forge) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        AshenForgeTile forge = forgeAt(world, x, y, z);
        return id == FORGE_SCREENS && forge != null
                ? AshenSkillsMod.proxy.forgeScreen(new ForgeContainer(player.inventory, forge))
                : null;
    }

    private static AshenForgeTile forgeAt(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        return tile instanceof AshenForgeTile ? (AshenForgeTile) tile : null;
    }
}
