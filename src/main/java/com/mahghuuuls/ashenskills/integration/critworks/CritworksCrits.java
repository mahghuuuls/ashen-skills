package com.mahghuuuls.ashenskills.integration.critworks;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.integration.CritGateway;
import com.mahghuuuls.critworks.api.CritAttackKind;
import com.mahghuuuls.critworks.api.CritworksApi;
import com.mahghuuuls.critworks.api.IAttackIntegration;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import org.apache.logging.log4j.LogManager;

/**
 * Critical hits through Critworks. A strike is one Critworks attack: Critworks decides once, when
 * the strike begins, whether it is critical, by its own rules for the weapon and the player; until
 * the strike ends it multiplies the damage this player deals. This mod multiplies nothing itself,
 * so a critical counts once.
 */
public final class CritworksCrits implements CritGateway {

    /** A skill strike is a full blow, never a half-charged swing. */
    private static final float FULL_CHARGE = 1.0F;

    /** Set when Critworks failed once; from then on strikes never crit, and everything else goes on. */
    private boolean failed;

    /** Tells Critworks about this mod's attacks. Must run while mods load. */
    public CritworksCrits() {
        CritworksApi.registerAttackIntegration(new IAttackIntegration() {
            @Override
            public String getId() {
                return Tags.MOD_ID + ":skill_strike";
            }
        });
    }

    @Override
    public void beginStrike(EntityPlayer user, ItemStack weapon) {
        if (failed) {
            return;
        }
        try {
            CritworksApi.beginAttack(user, weapon, EnumHand.MAIN_HAND, CritAttackKind.MELEE, FULL_CHARGE);
        } catch (RuntimeException | LinkageError problem) {
            giveUp(problem);
        }
    }

    @Override
    public void endStrike(EntityPlayer user) {
        if (failed) {
            return;
        }
        try {
            CritworksApi.endAttack(user);
        } catch (RuntimeException | LinkageError problem) {
            giveUp(problem);
        }
    }

    private void giveUp(Throwable problem) {
        failed = true;
        LogManager.getLogger(Tags.MOD_NAME).error(
                "Critworks failed; skill strikes will not be critical until the game is restarted", problem);
    }
}
