package com.mahghuuuls.ashenskills.runtime;

/**
 * The wind-up of a stance: the time the stance must be held before an attack press executes the
 * skill. Counted in server ticks, like every other time of a skill use.
 */
final class WindUp {

    private WindUp() {
    }

    /** The ticks a wind-up of this many seconds lasts. A part of a tick counts as a whole one. */
    static int ticks(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds * SessionTimeline.TICKS_PER_SECOND - 1.0e-6));
    }

    /** Whether a stance held for this many ticks is past a wind-up of this many seconds. */
    static boolean isOver(int ticksInStance, double seconds) {
        return ticksInStance >= ticks(seconds);
    }
}
