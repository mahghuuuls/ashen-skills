package com.mahghuuuls.ashenskills.runtime;

/** Why a skill did not start. */
public enum RefusalReason {

    /** The main-hand item carries no skill. Not an error; nothing is shown. */
    NO_SKILL(false),
    /** No installed mod provides the attached skill. */
    SKILL_UNAVAILABLE(true),
    /** The attached skill does not fit this weapon's types. */
    INCOMPATIBLE_WEAPON(true),
    /** The player is already using a skill. Nothing is shown. */
    BUSY(false),
    /** The exclusive cooldown runs. */
    ON_COOLDOWN(true),
    /** The player's state (riding, in the air, ...) does not allow the skill. */
    NOT_ALLOWED_NOW(true),
    /** The skill costs stamina and no stamina mod is installed; the server does not waive that. */
    NO_STAMINA_SYSTEM(true),
    /** The skill costs mana and no mana mod is installed; the server does not waive that. */
    NO_MANA_SYSTEM(true),
    /** The stamina mod does not let the player pay the cost now. */
    NOT_ENOUGH_STAMINA(true),
    /** The mana mod does not let the player pay the cost now. */
    NOT_ENOUGH_MANA(true);

    private final boolean shownToPlayer;

    RefusalReason(boolean shownToPlayer) {
        this.shownToPlayer = shownToPlayer;
    }

    public boolean isShownToPlayer() {
        return shownToPlayer;
    }

    /** Language key of the short text the player sees. */
    public String messageKey() {
        return "ashenskills.refusal." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
