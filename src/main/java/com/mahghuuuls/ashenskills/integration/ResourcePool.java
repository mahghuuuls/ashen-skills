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
        public Claim claim(EntityPlayer player, double amount) {
            return null;
        }
    };

    /** False also after the owning mod failed; from then on its cost is treated as that of an absent mod. */
    boolean isInstalled();

    /**
     * Asks the owning mod whether the player may spend this much now, by that mod's own rules (it
     * may allow going into debt, or make the use free). Nothing is taken yet.
     *
     * @return the claim to take or to release, or null when the owning mod refuses
     */
    Claim claim(EntityPlayer player, double amount);

    /** An accepted request to spend. Exactly one of the two methods is called, once. */
    interface Claim {

        /** A claim that costs nothing. */
        Claim FREE = new Claim() {
            @Override
            public boolean take() {
                return true;
            }

            @Override
            public void release() {
            }
        };

        /**
         * Takes what was claimed.
         *
         * @return false when the owning mod took nothing after all
         */
        boolean take();

        /** Gives the claim up. The resource is as if it was never asked for. */
        void release();
    }
}
