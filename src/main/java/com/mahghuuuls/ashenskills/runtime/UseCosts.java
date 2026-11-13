package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.attribute.AttributeFormulas;
import com.mahghuuuls.ashenskills.integration.PaymentResult;

/** What one use of a skill costs a player with given attribute points, and how a failed payment reads. */
final class UseCosts {

    private UseCosts() {
    }

    /**
     * @param cooldownSeconds the skill's cooldown
     * @param scalePoints the points of Haste that halve a cooldown
     * @param hastePoints the player's Haste
     */
    static long cooldownMillis(double cooldownSeconds, double scalePoints, double hastePoints) {
        return Math.round(cooldownSeconds * 1000 * AttributeFormulas.remainingShare(scalePoints, hastePoints));
    }

    /**
     * @param cost the skill's stamina or mana cost
     * @param scalePoints the points of Efficiency that halve a cost
     * @param efficiencyPoints the player's Efficiency for that resource
     */
    static double resourceCost(double cost, double scalePoints, double efficiencyPoints) {
        return cost * AttributeFormulas.remainingShare(scalePoints, efficiencyPoints);
    }

    /** Why the skill does not start after this payment result; null when it was paid. */
    static RefusalReason refusalFor(PaymentResult result) {
        switch (result) {
            case PAID:
                return null;
            case STAMINA_MOD_MISSING:
                return RefusalReason.NO_STAMINA_SYSTEM;
            case MANA_MOD_MISSING:
                return RefusalReason.NO_MANA_SYSTEM;
            case NOT_ENOUGH_STAMINA:
                return RefusalReason.NOT_ENOUGH_STAMINA;
            case NOT_ENOUGH_MANA:
                return RefusalReason.NOT_ENOUGH_MANA;
            default:
                throw new IllegalArgumentException("Unknown payment result " + result);
        }
    }
}
