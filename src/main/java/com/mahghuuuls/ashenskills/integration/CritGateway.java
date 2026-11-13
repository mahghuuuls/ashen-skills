package com.mahghuuuls.ashenskills.integration;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * The mod's view of a critical-hit system. One strike (all targets of one hit window) is wrapped
 * in {@link #beginStrike} and {@link #endStrike}, so the strike gets one critical roll shared by
 * all its targets.
 */
public interface CritGateway {

    /** No critical-hit system is installed: strikes never crit. */
    CritGateway NONE = new CritGateway() {
        @Override
        public void beginStrike(EntityPlayer user, ItemStack weapon) {
        }

        @Override
        public void endStrike(EntityPlayer user) {
        }
    };

    void beginStrike(EntityPlayer user, ItemStack weapon);

    /** Always called after {@link #beginStrike}, also when the strike failed part way. */
    void endStrike(EntityPlayer user);

    /**
     * Runs one strike between its begin and its end. The end is told even when the strike throws,
     * so a failed strike cannot leave the next attack of that player inside this one.
     *
     * @param strike damages the targets of one hit window
     */
    static void around(CritGateway gateway, EntityPlayer user, ItemStack weapon, Runnable strike) {
        gateway.beginStrike(user, weapon);
        try {
            strike.run();
        } finally {
            gateway.endStrike(user);
        }
    }
}
