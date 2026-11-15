package com.mahghuuuls.ashenskills.api;

/**
 * What charging a skill is worth. A charge that reaches the minimum time may execute; from there
 * the damage bonus grows in a straight line until the full time and stays at its maximum after.
 * This is the only place that knows the shape; the server uses it for the damage and a client for
 * its charge bar.
 */
public final class ChargeCurve {

    private final double minSeconds;
    private final double fullSeconds;
    private final double maxBonus;

    /**
     * @param minSeconds the shortest charge that executes
     * @param fullSeconds the charge that gives the whole bonus; a value below the minimum counts as
     *     the minimum
     * @param maxBonus the extra share of damage at full charge: 0.8 for 80 percent more
     */
    public ChargeCurve(double minSeconds, double fullSeconds, double maxBonus) {
        this.minSeconds = Math.max(0.0, minSeconds);
        this.fullSeconds = Math.max(this.minSeconds, fullSeconds);
        this.maxBonus = Math.max(0.0, maxBonus);
    }

    /** The curve of a charged skill, from its values. */
    public static ChargeCurve of(SkillDefinition skill) {
        return new ChargeCurve(
                skill.number(SkillNumber.MIN_CHARGE),
                skill.number(SkillNumber.FULL_CHARGE),
                skill.number(SkillNumber.MAX_CHARGE_BONUS));
    }

    /** Whether a charge of this length executes when the key is released. */
    public boolean reachesMinimum(double chargedSeconds) {
        return chargedSeconds >= minSeconds;
    }

    /** Whether a charge of this length gives the whole bonus. */
    public boolean isFull(double chargedSeconds) {
        return chargedSeconds >= fullSeconds;
    }

    /**
     * Whether the charge becomes full in this tick of charging. Asked once for every tick, counted
     * from 1, it is true for exactly one of them: the first tick at which the charge is full, which
     * is the first tick of all for a skill whose full charge is no time at all.
     *
     * @param tick how many ticks the charge has lasted, 1 for its first tick
     */
    public boolean becomesFullAtTick(int tick, int ticksPerSecond) {
        return isFull(tick / (double) ticksPerSecond)
                && (tick <= 1 || !isFull((tick - 1) / (double) ticksPerSecond));
    }

    /**
     * The extra share of damage for a charge of this length: 0 up to the minimum, the maximum from
     * the full time on, a straight line between.
     */
    public double bonus(double chargedSeconds) {
        if (chargedSeconds < minSeconds) {
            return 0.0;
        }
        // Asked first, so a charge that is full has the whole bonus even when full and minimum are one moment.
        if (isFull(chargedSeconds)) {
            return maxBonus;
        }
        return maxBonus * (chargedSeconds - minSeconds) / (fullSeconds - minSeconds);
    }

    /** How much of the way to a full charge a charge of this length is, from 0 to 1: what a bar shows. */
    public double progress(double chargedSeconds) {
        if (fullSeconds <= 0.0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, chargedSeconds / fullSeconds));
    }

    /** Where the minimum lies on the way to a full charge, from 0 to 1: the place of a bar's first marker. */
    public double minimumProgress() {
        return progress(minSeconds);
    }
}
