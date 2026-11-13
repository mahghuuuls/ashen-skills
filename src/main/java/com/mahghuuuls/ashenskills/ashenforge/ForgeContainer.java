package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.SkillTag;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * One player's open Ashen Forge. It joins three things: the weapon input, which belongs to this
 * container alone and so to this player alone; the Forge's shared Ash storage; and the player's
 * inventory. The weapon is never part of the Forge: whenever this container closes, for any
 * reason, the weapon goes back to the player.
 */
public final class ForgeContainer extends Container {

    /** Slot numbers: the weapon input, then the storage, then the player's 27 slots and hotbar. */
    public static final int WEAPON_SLOT = 0;
    public static final int FIRST_STORAGE_SLOT = 1;
    public static final int STORAGE_COLUMNS = 9;
    public static final int STORAGE_SLOTS_PER_PAGE = 54;

    private static final double REACH_SQUARED = 64.0;

    private final AshenForgeTile forge;
    private final int storageSlots;
    private final ItemStackHandler weaponInput = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !Ash.isAsh(stack);
        }
    };
    public ForgeContainer(InventoryPlayer playerInventory, AshenForgeTile forge) {
        this.forge = forge;
        IItemHandlerModifiable ashes = forge.ashes();
        this.storageSlots = ashes.getSlots();
        addSlotToContainer(new SlotItemHandler(weaponInput, 0, 8, 18) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !Ash.isAsh(stack);
            }
        });
        for (int slot = 0; slot < storageSlots; slot++) {
            int onPage = slot % STORAGE_SLOTS_PER_PAGE;
            addSlotToContainer(new SlotItemHandler(
                    ashes, slot, 8 + (onPage % STORAGE_COLUMNS) * 18, 18 + (onPage / STORAGE_COLUMNS) * 18) {
                /** Also for a stack that grew or shrank in place, which the storage itself does not notice. */
                @Override
                public void onSlotChanged() {
                    super.onSlotChanged();
                    ForgeContainer.this.forge.markDirty();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(playerInventory, column, 8 + column * 18, 198));
        }
    }

    public int storageSlots() {
        return storageSlots;
    }

    public ItemStack weapon() {
        return weaponInput.getStackInSlot(0);
    }

    /** The skills this Forge offers for the weapon in the input now. Empty without a weapon. */
    public List<String> offeredSkills() {
        final ItemStack weapon = weapon();
        if (weapon.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> stored = new ArrayList<>();
        for (int slot = 0; slot < storageSlots; slot++) {
            ItemStack stack = getSlot(FIRST_STORAGE_SLOT + slot).getStack();
            if (!stack.isEmpty()) {
                stored.add(Ash.skillOf(stack));
            }
        }
        return ForgeRules.offered(stored, id -> fits(weapon, id));
    }

    /**
     * Attaches the skill to the weapon in the input, in place of any other, when an Ash of it is
     * stored at this moment. No Ash is used up. Server side.
     *
     * @return null when the skill was attached, otherwise why not
     */
    public ForgeRules.Refusal attach(EntityPlayer player, String skillId) {
        ItemStack weapon = weapon();
        SkillDefinition skill = exactSkill(skillId);
        ForgeRules.Refusal refusal = ForgeRules.refusalToAttach(
                !weapon.isEmpty(), skill != null, skill != null && WeaponTypes.isCompatible(weapon, skill),
                forge.holdsAshOf(skillId));
        if (refusal == null) {
            SkillTag.setSkillId(weapon, skillId);
            detectAndSendChanges();
        }
        Diagnostics.log("FORGE", "player={} action=attach skill={} weapon={} result={}",
                player.getName(), skillId, weapon.isEmpty() ? "none" : weapon.getItem().getRegistryName(),
                refusal == null ? "ATTACHED" : refusal);
        return refusal;
    }

    /** Takes the skill off the weapon in the input. Always allowed. Server side. */
    public void detach(EntityPlayer player) {
        ItemStack weapon = weapon();
        String had = SkillTag.getSkillId(weapon);
        SkillTag.clear(weapon);
        detectAndSendChanges();
        Diagnostics.log("FORGE", "player={} action=detach weapon={} had={}",
                player.getName(), weapon.isEmpty() ? "none" : weapon.getItem().getRegistryName(), had);
    }

    /**
     * Gives the weapon in the input back to the player: into the inventory when it has an empty
     * slot, otherwise onto the ground at the player. Afterwards the input is empty, so calling this
     * again gives nothing a second time.
     *
     * @param inventoryStillCounts false when the player is dead and the inventory was already dealt
     *     with; the weapon then goes to the ground
     */
    public void returnWeapon(EntityPlayer player, boolean inventoryStillCounts) {
        ItemStack weapon = weapon();
        if (weapon.isEmpty()) {
            return;
        }
        Object item = weapon.getItem().getRegistryName();
        weaponInput.setStackInSlot(0, ItemStack.EMPTY);
        // In Creative the game would "add" an item to a full inventory by deleting it, so look first.
        boolean intoInventory = inventoryStillCounts && player.inventory.getFirstEmptyStack() >= 0
                && player.inventory.addItemStackToInventory(weapon);
        if (!intoInventory) {
            player.dropItem(weapon, false);
        }
        Diagnostics.log("FORGE", "player={} action=returnWeapon item={} to={}",
                player.getName(), item, intoInventory ? "inventory" : "ground");
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            returnWeapon(player, player.isEntityAlive());
        }
    }

    /** False as soon as the player walked away or the Forge is gone; the game then closes this container. */
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSqToCenter(forge.getPos()) <= REACH_SQUARED
                && !forge.isInvalid()
                && player.world.getTileEntity(forge.getPos()) == forge;
    }

    /** Shift-click: out of the Forge into the inventory; an Ash into the storage; anything else into the input. */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getStack();
        ItemStack before = inSlot.copy();
        int firstPlayerSlot = FIRST_STORAGE_SLOT + storageSlots;
        boolean moved;
        if (index < firstPlayerSlot) {
            moved = mergeItemStack(inSlot, firstPlayerSlot, inventorySlots.size(), true);
        } else if (Ash.isAsh(inSlot)) {
            moved = mergeItemStack(inSlot, FIRST_STORAGE_SLOT, firstPlayerSlot, false);
        } else {
            moved = moveOneToInput(inSlot);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (inSlot.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        slot.onTake(player, inSlot);
        return before;
    }

    private boolean moveOneToInput(ItemStack from) {
        if (!weapon().isEmpty()) {
            return false;
        }
        weaponInput.setStackInSlot(0, from.splitStack(1));
        return true;
    }

    private static boolean fits(ItemStack weapon, String skillId) {
        SkillDefinition skill = exactSkill(skillId);
        return skill != null && WeaponTypes.isCompatible(weapon, skill);
    }

    /** The skill with exactly this id text; an id in another spelling names no skill. */
    private static SkillDefinition exactSkill(String skillId) {
        if (skillId == null) {
            return null;
        }
        ResourceLocation id = new ResourceLocation(skillId);
        return id.toString().equals(skillId) ? SkillRegistry.get(id) : null;
    }
}
