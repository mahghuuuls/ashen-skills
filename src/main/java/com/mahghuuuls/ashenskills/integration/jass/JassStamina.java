package com.mahghuuuls.ashenskills.integration.jass;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.integration.PreparationRegen;
import com.mahghuuuls.ashenskills.integration.ResourcePool;
import com.mahghuuuls.jass.api.JassApi;
import com.mahghuuuls.jass.api.PreparationResult;
import com.mahghuuuls.jass.api.PreparedStaminaAction;
import com.mahghuuuls.jass.api.SettlementOutcome;
import com.mahghuuuls.jass.api.StaminaContribution;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;

/**
 * Stamina through JASS. A skill use is one JASS discrete action: JASS decides whether it may start,
 * applies its own efficiency, and lets the cost go into debt by its own rules. The cost is only
 * held when claimed and paid when taken, so a use that fails for another reason costs no stamina.
 * Everything here runs on the server thread, JASS's question about regeneration included.
 */
public final class JassStamina implements ResourcePool, PreparationRegen {

    private static final ResourceLocation SKILL_ACTION = new ResourceLocation(Tags.MOD_ID, "skill");
    private static final ResourceLocation REGEN_PROVIDER = new ResourceLocation(Tags.MOD_ID, "preparation_regeneration");

    /** Players in preparation, with the multiplier of their regeneration. */
    private final Map<UUID, Double> regenMultipliers = new HashMap<>();

    /** Set when JASS failed once; from then on stamina counts as not installed. */
    private boolean failed;

    /** Tells JASS about the regeneration rule. Must run while mods load, after JASS. */
    public JassStamina() {
        JassApi.registerContextualProvider(REGEN_PROVIDER, this::contribution);
    }

    @Override
    public boolean isInstalled() {
        return !failed;
    }

    @Override
    public Claim claim(EntityPlayer player, double amount) {
        if (failed) {
            return Claim.FREE;
        }
        final PreparedStaminaAction prepared;
        try {
            prepared = JassApi.getStaminaService().prepareDiscreteAction(
                    player, SKILL_ACTION, amount, false, player.getHeldItemMainhand(), EnumHand.MAIN_HAND);
            if (prepared.getResult() == PreparationResult.REFUSED) {
                return null;
            }
        } catch (RuntimeException | LinkageError problem) {
            giveUp(problem);
            return Claim.FREE;
        }
        return new Claim() {
            @Override
            public boolean take() {
                try {
                    return prepared.settle().getOutcome() != SettlementOutcome.INVALIDATED;
                } catch (RuntimeException | LinkageError problem) {
                    giveUp(problem);
                    return false;
                }
            }

            @Override
            public void release() {
                try {
                    prepared.release();
                } catch (RuntimeException | LinkageError problem) {
                    // JASS drops a claim that is still open at the end of the tick.
                    giveUp(problem);
                }
            }
        };
    }

    @Override
    public void begin(EntityPlayer player, double multiplier) {
        regenMultipliers.put(player.getUniqueID(), multiplier);
        askJassToReadAgain(player);
    }

    @Override
    public void end(EntityPlayer player) {
        if (regenMultipliers.remove(player.getUniqueID()) != null) {
            askJassToReadAgain(player);
        }
    }

    @Override
    public void forget(UUID playerId) {
        // JASS asks again by itself when the player comes back.
        regenMultipliers.remove(playerId);
    }

    /** JASS keeps the answer of {@link #contribution} until it is told that the answer changed. */
    private void askJassToReadAgain(EntityPlayer player) {
        try {
            JassApi.markModifierCacheDirty(player);
        } catch (RuntimeException | LinkageError problem) {
            giveUp(problem);
        }
    }

    private StaminaContribution contribution(EntityPlayer player) {
        Double multiplier = regenMultipliers.get(player.getUniqueID());
        if (multiplier == null) {
            return StaminaContribution.EMPTY;
        }
        return StaminaContribution.builder().regenerationReduction(PreparationRegen.reductionFor(multiplier)).build();
    }

    private void giveUp(Throwable problem) {
        if (failed) {
            return;
        }
        failed = true;
        LogManager.getLogger(Tags.MOD_NAME).error(
                "JASS failed; skills treat stamina as not installed until the game is restarted", problem);
    }
}
