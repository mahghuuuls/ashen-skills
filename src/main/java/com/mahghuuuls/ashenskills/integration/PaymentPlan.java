package com.mahghuuuls.ashenskills.integration;

/**
 * What one skill use will take from the player's stamina and mana, as {@link ResourceGateway}
 * worked it out. A cost of a resource whose mod is absent is already 0 here when the server
 * waives it; when the server does not, the plan cannot be paid and says which mod is missing.
 */
public final class PaymentPlan {

    private final double stamina;
    private final double mana;
    private final PaymentResult blockedBy;

    PaymentPlan(double stamina, double mana, PaymentResult blockedBy) {
        this.stamina = stamina;
        this.mana = mana;
        this.blockedBy = blockedBy;
    }

    public double stamina() {
        return stamina;
    }

    public double mana() {
        return mana;
    }

    /** The missing mod that makes this plan impossible to pay, or null when none does. */
    public PaymentResult blockedBy() {
        return blockedBy;
    }
}
