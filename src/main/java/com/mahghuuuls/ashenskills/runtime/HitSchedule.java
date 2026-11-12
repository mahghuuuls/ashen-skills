package com.mahghuuuls.ashenskills.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * When the hit windows of one skill use come due, counted in execution ticks. Each window comes
 * due once, in order; windows beyond the cap never do.
 */
final class HitSchedule {

    private final List<Integer> windowTicks;
    private int nextWindow;

    /**
     * @param windowSeconds time of each window from the start of execution
     * @param cap the most windows one skill use may have
     */
    HitSchedule(List<Double> windowSeconds, int cap) {
        List<Integer> ticks = new ArrayList<>();
        for (double seconds : windowSeconds) {
            ticks.add(SessionTimeline.toTicks(seconds));
        }
        Collections.sort(ticks);
        this.windowTicks = ticks.size() > cap ? ticks.subList(0, Math.max(0, cap)) : ticks;
    }

    /**
     * @param elapsedExecutionTicks how many execution ticks have passed
     * @return the index of the next window that is due now, or -1 when none is; call again until -1
     */
    int nextDue(int elapsedExecutionTicks) {
        if (nextWindow < windowTicks.size() && windowTicks.get(nextWindow) <= elapsedExecutionTicks) {
            return nextWindow++;
        }
        return -1;
    }
}
