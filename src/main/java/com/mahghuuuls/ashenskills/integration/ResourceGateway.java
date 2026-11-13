package com.mahghuuuls.ashenskills.integration;

import java.util.function.BooleanSupplier;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Pays the stamina and mana of a skill use. Callers give the costs and get one answer; which mods
 * hold the resources, what happens when one is not installed, and that both costs are taken or
 * neither, is decided here and nowhere else.
 */
public final class ResourceGateway {

    private final ResourcePool stamina;
    private final ResourcePool mana;
    private final BooleanSupplier waiveAbsentCosts;

    /** @param waiveAbsentCosts whether a cost is free when no mod provides its resource */
    public ResourceGateway(ResourcePool stamina, ResourcePool mana, BooleanSupplier waiveAbsentCosts) {
        this.stamina = stamina;
        this.mana = mana;
        this.waiveAbsentCosts = waiveAbsentCosts;
    }

    /** Works out what a use with these costs would take. Takes nothing. */
    public PaymentPlan quote(double staminaCost, double manaCost) {
        boolean waive = waiveAbsentCosts.getAsBoolean();
        PaymentResult blockedBy = null;
        if (!waive && staminaCost > 0 && !stamina.isInstalled()) {
            blockedBy = PaymentResult.STAMINA_MOD_MISSING;
        } else if (!waive && manaCost > 0 && !mana.isInstalled()) {
            blockedBy = PaymentResult.MANA_MOD_MISSING;
        }
        return new PaymentPlan(
                stamina.isInstalled() ? Math.max(0.0, staminaCost) : 0.0,
                mana.isInstalled() ? Math.max(0.0, manaCost) : 0.0,
                blockedBy);
    }

    /** Takes everything the plan names, or nothing. */
    public PaymentResult pay(EntityPlayer player, PaymentPlan plan) {
        if (plan.blockedBy() != null) {
            return plan.blockedBy();
        }
        if (plan.stamina() > 0 && !stamina.allowsSpending(player, plan.stamina())) {
            return PaymentResult.NOT_ENOUGH_STAMINA;
        }
        if (plan.mana() > 0 && !mana.allowsSpending(player, plan.mana())) {
            return PaymentResult.NOT_ENOUGH_MANA;
        }
        if (plan.stamina() > 0) {
            stamina.spend(player, plan.stamina());
        }
        if (plan.mana() > 0) {
            mana.spend(player, plan.mana());
        }
        return PaymentResult.PAID;
    }
}
