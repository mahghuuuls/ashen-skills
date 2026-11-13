package com.mahghuuuls.ashenskills.integration;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Slows how fast a player's stamina comes back while that player prepares a skill (holds a
 * stance, charges a blow). Without a stamina mod nothing happens.
 */
public interface PreparationRegen {

    PreparationRegen NONE = new PreparationRegen() {
        @Override
        public void begin(EntityPlayer player, double multiplier) {
        }

        @Override
        public void end(EntityPlayer player) {
        }

        @Override
        public void forget(UUID playerId) {
        }
    };

    /**
     * From now until {@link #end}, the player's stamina regeneration is multiplied by this.
     *
     * @param multiplier 0 stops regeneration, 1 changes nothing; values outside 0 to 1 count as the
     *     nearer of the two
     */
    void begin(EntityPlayer player, double multiplier);

    /** Regeneration is the stamina mod's own again. Called on every way out of preparation. */
    void end(EntityPlayer player);

    /** As {@link #end}, for a player who left the game during preparation. */
    void forget(UUID playerId);

    /** The share of regeneration that a multiplier takes away: 1 for a multiplier of 0, 0 for 1. */
    static double reductionFor(double multiplier) {
        return 1.0 - Math.max(0.0, Math.min(1.0, multiplier));
    }
}
