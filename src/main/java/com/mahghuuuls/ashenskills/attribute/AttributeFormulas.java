package com.mahghuuuls.ashenskills.attribute;

/** What the points of Haste and of the two Efficiencies do to a length or a cost. */
public final class AttributeFormulas {

    private AttributeFormulas() {
    }

    /**
     * The share of a cooldown or cost that remains. With as many points as the scale, half
     * remains; each further point removes less than the one before, so the share never reaches 0.
     * Points below 0 count as 0: these attributes can only shorten and cheapen.
     *
     * @param scalePoints the points that halve the value; above 0
     * @param points the player's points of the attribute
     * @return a factor above 0 and at most 1
     */
    public static double remainingShare(double scalePoints, double points) {
        return scalePoints / (scalePoints + Math.max(0.0, points));
    }
}
