package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.skill.Ash;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

/**
 * One player's open Ashen Forge. It joins three things: the Forge's weapon slot, the Forge's Ash
 * storage, and the player's inventory. The weapon and the Ashes belong to the Forge: every player
 * who has it open works on the same items, and closing this container moves nothing.
 */
public final class ForgeContainer extends Container {

    /** Slot numbers: the weapon slot, then the storage, then the player's 27 slots and hotbar. */
    public static final int WEAPON_SLOT = 0;
    public static final int FIRST_STORAGE_SLOT = 1;
    public static final int STORAGE_COLUMNS = 9;
    public static final int STORAGE_SLOTS_PER_PAGE = 54;

    private static final double REACH_SQUARED = 64.0;

    private final AshenForgeTile forge;
    private final int storageSlots;

    public ForgeContainer(InventoryPlayer playerInventory, AshenForgeTile forge) {
        this.forge = forge;
        IItemHandlerModifiable ashes = forge.ashes();
        this.storageSlots = ashes.getSlots();
        addSlotToContainer(new SlotItemHandler(forge.weaponSlot(), 0, 8, 18) {
            /** Asked directly: the general way empties and refills the slot to find out, which the Forge would report as two changes. */
            @Override
            public boolean isItemValid(ItemStack stack) {
                return !Ash.isAsh(stack);
            }

            /** Answered directly for the same reason: the slot holds one item. */
            @Override
            public int getItemStackLimit(ItemStack stack) {
                return 1;
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
        return forge.weapon();
    }

    /** The skills this Forge offers for the weapon it holds now. Empty without a weapon. */
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
     * Attaches the skill to the weapon the Forge holds, in place of any other, when an Ash of it is
     * stored at this moment. No Ash is used up. A sound at the Forge tells nearby players that it
     * worked. Server side.
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
            forge.putSkillOnWeapon(skillId);
            detectAndSendChanges();
            player.world.playSound(null, forge.getPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS,
                    1.0F, 0.9F + player.world.rand.nextFloat() * 0.1F);
        }
        Diagnostics.log("FORGE", "player={} action=attach skill={} weapon={} result={}",
                player.getName(), skillId, weapon.isEmpty() ? "none" : weapon.getItem().getRegistryName(),
                refusal == null ? "ATTACHED" : refusal);
        return refusal;
    }

    /** Takes the skill off the weapon the Forge holds. Always allowed. Server side. */
    public void detach(EntityPlayer player) {
        ItemStack weapon = weapon();
        String had = forge.takeSkillOffWeapon();
        if (had != null) {
            detectAndSendChanges();
        }
        Diagnostics.log("FORGE", "player={} action=detach weapon={} had={}",
                player.getName(), weapon.isEmpty() ? "none" : weapon.getItem().getRegistryName(), had);
    }

    /** False as soon as the player walked away or the Forge is gone; the game then closes this container. */
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player.getDistanceSqToCenter(forge.getPos()) <= REACH_SQUARED
                && !forge.isInvalid()
                && player.world.getTileEntity(forge.getPos()) == forge;
    }

    /** Shift-click: out of the Forge into the inventory; an Ash into the storage; anything else into the weapon slot. */
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
            moved = moveOneToWeaponSlot(inSlot);
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

    private boolean moveOneToWeaponSlot(ItemStack from) {
        if (!weapon().isEmpty()) {
            return false;
        }
        forge.weaponSlot().setStackInSlot(0, from.splitStack(1));
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
