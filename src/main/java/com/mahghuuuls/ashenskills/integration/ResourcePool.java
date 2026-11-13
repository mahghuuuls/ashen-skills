package com.mahghuuuls.ashenskills.integration;

import net.minecraft.entity.player.EntityPlayer;

/** One resource a skill can cost (stamina, mana), as the mod that owns that resource keeps it. */
public interface ResourcePool {

    /** No mod provides this resource. It can pay nothing; {@link ResourceGateway} decides what that means. */
    ResourcePool ABSENT = new ResourcePool() {
        @Override
        public boolean isInstalled() {
            return false;
        }

        @Override
        public boolean allowsSpending(EntityPlayer player, double amount) {
            return false;
        }

        @Override
        public void spend(EntityPlayer player, double amount) {
        }
    };

    boolean isInstalled();

    /**
     * Whether the owning mod lets the player spend this much now, by that mod's own rules (it may
     * allow going into debt).
     */
    boolean allowsSpending(EntityPlayer player, double amount);

    /** Takes the amount. Only called after {@link #allowsSpending} said yes for the same amount. */
    void spend(EntityPlayer player, double amount);
}
