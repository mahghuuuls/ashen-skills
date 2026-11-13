package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.weapon.SkillTag;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

/**
 * What an Ashen Forge keeps: the Ashes all its users share, and the one weapon it holds. Both stay
 * in the Forge when a screen closes and are saved with the block. Neither is offered to hoppers or
 * pipes: only a player at the Forge can put items in or take them out. Clients are told which
 * weapon the Forge holds, so they can show it above the block.
 */
public final class AshenForgeTile extends TileEntity {

    private static final String ASHES_KEY = "Ashes";
    private static final String SLOT_KEY = "Slot";
    private static final String WEAPON_KEY = "Weapon";
    /** Only in the data sent to clients: how many slots the storage has on the server. */
    private static final String SLOT_COUNT_KEY = "AshSlots";
    /** Saved data naming a slot at or above this is damaged; such an entry is not loaded. */
    private static final int MOST_SLOTS_EVER = 1024;

    /** One object for the whole life of this Forge, so every open screen works on the same Ashes. */
    private final Storage ashes = new Storage(Settings.general().getInt(SettingKey.FORGE_ASH_SLOTS));
    /** One object for the whole life of this Forge, so every open screen works on the same weapon. */
    private final WeaponSlot weapon = new WeaponSlot();

    /** The shared Ash storage. It refuses every item that is not an Ash. */
    public IItemHandlerModifiable ashes() {
        return ashes;
    }

    /** The slot for the one weapon the Forge holds. It takes one item and refuses Ashes. */
    public IItemHandlerModifiable weaponSlot() {
        return weapon;
    }

    /** The weapon the Forge holds now; empty when it holds none. */
    public ItemStack weapon() {
        return weapon.getStackInSlot(0);
    }

    /** Puts the skill on the held weapon, in place of any other. Does nothing without a weapon. */
    public void putSkillOnWeapon(String skillId) {
        if (!weapon().isEmpty()) {
            SkillTag.setSkillId(weapon(), skillId);
            weaponChanged();
        }
    }

    /**
     * Takes the skill off the held weapon.
     *
     * @return the id of the skill it carried, or null when it carried none
     */
    public String takeSkillOffWeapon() {
        String had = SkillTag.getSkillId(weapon());
        if (had != null) {
            SkillTag.clear(weapon());
            weaponChanged();
        }
        return had;
    }

    /** Saves the Forge and tells the clients that show the weapon. */
    private void weaponChanged() {
        markDirty();
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 2);
        }
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

    /** Drops every stored Ash and the held weapon at the block. Afterwards the Forge is empty. */
    void dropContents(World world, BlockPos at) {
        for (int slot = 0; slot < ashes.getSlots(); slot++) {
            ItemStack stack = ashes.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ashes.setStackInSlot(slot, ItemStack.EMPTY);
                InventoryHelper.spawnItemStack(world, at.getX(), at.getY(), at.getZ(), stack);
            }
        }
        ItemStack held = weapon();
        if (!held.isEmpty()) {
            weapon.setStackInSlot(0, ItemStack.EMPTY);
            InventoryHelper.spawnItemStack(world, at.getX(), at.getY(), at.getZ(), held);
        }
    }

    /** The contents stay when only the block's facing changes (a wrench, a command). */
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
        writeWeapon(compound);
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
        readWeapon(compound);
    }

    private void writeWeapon(NBTTagCompound compound) {
        ItemStack held = weapon();
        if (!held.isEmpty()) {
            compound.setTag(WEAPON_KEY, held.writeToNBT(new NBTTagCompound()));
        }
    }

    /** An absent entry means an empty slot. An Ash is never loaded as the weapon. */
    private void readWeapon(NBTTagCompound compound) {
        ItemStack held = compound.hasKey(WEAPON_KEY, Constants.NBT.TAG_COMPOUND)
                ? new ItemStack(compound.getCompoundTag(WEAPON_KEY))
                : ItemStack.EMPTY;
        weapon.load(Ash.isAsh(held) ? ItemStack.EMPTY : held);
    }

    private static boolean isUsable(int slot) {
        return slot >= 0 && slot < MOST_SLOTS_EVER;
    }

    /**
     * What a client gets: where the Forge is, how many storage slots it has, and the weapon it
     * holds. The Ashes come through the open screen.
     */
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.writeToNBT(new NBTTagCompound());
        tag.setInteger(SLOT_COUNT_KEY, ashes.getSlots());
        writeWeapon(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.readFromNBT(tag);
        int slots = tag.getInteger(SLOT_COUNT_KEY);
        if (isUsable(slots - 1) && slots != ashes.getSlots()) {
            ashes.setSize(slots);
        }
        readWeapon(tag);
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }

    /** The weapon is drawn above the block, outside the block's own space. */
    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos, pos.add(1, 2, 1));
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

    private final class WeaponSlot extends ItemStackHandler {

        WeaponSlot() {
            super(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !Ash.isAsh(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return Ash.isAsh(stack) ? stack : super.insertItem(slot, stack, simulate);
        }

        /** Sets the content from saved or received data, without reporting a change. */
        void load(ItemStack stack) {
            stacks.set(0, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            weaponChanged();
        }
    }
}
