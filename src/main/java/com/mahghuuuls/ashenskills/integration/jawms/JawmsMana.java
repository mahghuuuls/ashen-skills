package com.mahghuuuls.ashenskills.integration.jawms;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.integration.ResourcePool;
import com.mahghuuuls.jawms.api.ManaApi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

/**
 * Mana through JAWMS. JAWMS counts whole mana points and has no way to hold an amount: it removes
 * exactly the amount or nothing. So a claim is always given, and taking it is the only moment that
 * can fail.
 */
public final class JawmsMana implements ResourcePool {

    private static final ResourceLocation SKILL_CAUSE = new ResourceLocation(Tags.MOD_ID, "skill");

    /** Set when JAWMS failed once; from then on mana counts as not installed. */
    private boolean failed;

    @Override
    public boolean isInstalled() {
        return !failed;
    }

    @Override
    public Claim claim(final EntityPlayer player, double amount) {
        if (failed) {
            return Claim.FREE;
        }
        // The caller already made the cost a whole number; JAWMS takes nothing else.
        final int points = (int) Math.round(amount);
        return new Claim() {
            @Override
            public boolean take() {
                try {
                    return ManaApi.getManaService().consumeMana(player, points, SKILL_CAUSE).isSuccessful();
                } catch (RuntimeException | LinkageError problem) {
                    giveUp(problem);
                    return false;
                }
            }

            @Override
            public void release() {
                // Nothing was held.
            }
        };
    }

    private void giveUp(Throwable problem) {
        if (failed) {
            return;
        }
        failed = true;
        LogManager.getLogger(Tags.MOD_NAME).error(
                "JAWMS failed; skills treat mana as not installed until the game is restarted", problem);
    }
}
