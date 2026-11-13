package com.mahghuuuls.ashenskills.client.hud;

/** What the skill HUD shows for the weapon in the main hand. */
public enum HudState {

    /** The item carries no skill: no HUD at all. */
    HIDDEN,
    /** The skill can be used now. */
    READY,
    /** The skill fits the weapon, and the cooldown runs. */
    ON_COOLDOWN,
    /** The skill is attached but cannot be used on this weapon, or no installed mod provides it. */
    UNUSABLE;

    private static final long MILLIS_PER_SECOND = 1000;

    /** Which frame of the frame sheet shows this state, counted from the left. */
    public int frame() {
        return this == READY ? 0 : this == ON_COOLDOWN ? 1 : 2;
    }

    /**
     * @param carriesSkill whether the main-hand item has a skill attached
     * @param skillUsable whether that skill exists and fits the weapon
     * @param cooldownRemainingMillis what the server last reported, counted down by this client
     */
    public static HudState of(boolean carriesSkill, boolean skillUsable, long cooldownRemainingMillis) {
        if (!carriesSkill) {
            return HIDDEN;
        }
        if (!skillUsable) {
            return UNUSABLE;
        }
        return cooldownRemainingMillis > 0 ? ON_COOLDOWN : READY;
    }

    /** The whole seconds to show for a running cooldown: 8 from the start, 1 during the last second. */
    public static long secondsShown(long cooldownRemainingMillis) {
        return (Math.max(0, cooldownRemainingMillis) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND;
    }

    /**
     * How many of an icon's pixel rows the cooldown still covers, counted from the top.
     *
     * @param iconRows the icon's height in pixels
     */
    public static int rowsCovered(long cooldownRemainingMillis, long cooldownLengthMillis, int iconRows) {
        if (cooldownRemainingMillis <= 0 || cooldownLengthMillis <= 0) {
            return 0;
        }
        long covered = (cooldownRemainingMillis * iconRows + cooldownLengthMillis - 1) / cooldownLengthMillis;
        return (int) Math.min(iconRows, covered);
    }
}
