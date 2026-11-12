package com.mahghuuuls.ashenskills.combat.math;

/** How far up and down a strike reaches, measured from the user's feet. */
public final class VerticalReach {

    private VerticalReach() {
    }

    /**
     * @param feetY height of the user's feet
     * @param below how far below the feet a target's box may end and still be reached
     * @param above how far above the feet a target's box may begin and still be reached
     * @param minY bottom of the target's box
     * @param maxY top of the target's box
     */
    public static boolean overlaps(double feetY, double below, double above, double minY, double maxY) {
        return maxY >= feetY - below && minY <= feetY + above;
    }
}
