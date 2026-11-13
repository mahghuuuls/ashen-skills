package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.skill.Ash;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

/**
 * What an Ashen Forge keeps: the Ashes all its users share. It holds nothing else; a weapon a
 * player works on is never stored here. The storage is not offered to hoppers or pipes: only a
 * player at the Forge can put Ashes in or take them out.
 */
public final class AshenForgeTile extends TileEntity {

    private static final String ASHES_KEY = "Ashes";
    private static final String SLOT_KEY = "Slot";
    /** Only in the data sent to clients: how many slots the storage has on the server. */
    private static final String SLOT_COUNT_KEY = "AshSlots";
    /** Saved data naming a slot at or above this is damaged; such an entry is not loaded. */
    private static final int MOST_SLOTS_EVER = 1024;

    /** One object for the whole life of this Forge, so every open screen works on the same Ashes. */
    private final Storage ashes = new Storage(Settings.general().getInt(SettingKey.FORGE_ASH_SLOTS));

    /** The shared Ash storage. It refuses every item that is not an Ash. */
    public IItemHandlerModifiable ashes() {
        return ashes;
    }

    /** The skill id of every stored Ash stack, in slot order; null for an Ash that names no skill. */
    public List<String> storedSkillIds() {
        List<String> ids = new ArrayList<>();
        for (int slot = 0; slot < ashes.getSlots(); slot++) {
            ItemStack stack = ashes.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ids.add(Ash.skillOf(stack));
            }
        }
        return ids;
    }

    public boolean holdsAshOf(String skillId) {
        return skillId != null && storedSkillIds().contains(skillId);
    }

    /** Drops every stored Ash at the block. */
    void dropAshes(World world, BlockPos at) {
        for (int slot = 0; slot < ashes.getSlots(); slot++) {
            ItemStack stack = ashes.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ashes.setStackInSlot(slot, ItemStack.EMPTY);
                InventoryHelper.spawnItemStack(world, at.getX(), at.getY(), at.getZ(), stack);
            }
        }
    }

    /** The Ashes stay when only the block's facing changes (a wrench, a command). */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (int slot = 0; slot < ashes.getSlots(); slot++) {
            ItemStack stack = ashes.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                NBTTagCompound entry = stack.writeToNBT(new NBTTagCompound());
                entry.setInteger(SLOT_KEY, slot);
                list.appendTag(entry);
            }
        }
        compound.setTag(ASHES_KEY, list);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagList list = compound.getTagList(ASHES_KEY, Constants.NBT.TAG_COMPOUND);
        // A server whose setting was lowered still keeps every Ash that was saved.
        int slots = Settings.general().getInt(SettingKey.FORGE_ASH_SLOTS);
        for (int i = 0; i < list.tagCount(); i++) {
            int slot = list.getCompoundTagAt(i).getInteger(SLOT_KEY);
            if (isUsable(slot)) {
                slots = Math.max(slots, slot + 1);
            }
        }
        ashes.setSize(slots);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            int slot = entry.getInteger(SLOT_KEY);
            if (isUsable(slot)) {
                ashes.setStackInSlot(slot, new ItemStack(entry));
            }
        }
    }

    private static boolean isUsable(int slot) {
        return slot >= 0 && slot < MOST_SLOTS_EVER;
    }

    /** What a client gets: where the Forge is and how many slots it has. The Ashes come through the open screen. */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        tag.setInteger(SLOT_COUNT_KEY, ashes.getSlots());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int slots = tag.getInteger(SLOT_COUNT_KEY);
        if (isUsable(slots - 1) && slots != ashes.getSlots()) {
            ashes.setSize(slots);
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }

    private final class Storage extends ItemStackHandler {

        Storage(int slots) {
            super(slots);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Ash.isAsh(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return Ash.isAsh(stack) ? super.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    }
}
