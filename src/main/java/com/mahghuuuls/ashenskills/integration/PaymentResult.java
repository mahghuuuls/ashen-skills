package com.mahghuuuls.ashenskills.integration;

/** How an attempt to pay a skill's stamina and mana ended. Only {@link #PAID} took anything. */
public enum PaymentResult {

    PAID,
    /** The skill costs stamina, no stamina mod is installed, and the server does not waive that. */
    STAMINA_MOD_MISSING,
    /** The skill costs mana, no mana mod is installed, and the server does not waive that. */
    MANA_MOD_MISSING,
    NOT_ENOUGH_STAMINA,
    NOT_ENOUGH_MANA
}
