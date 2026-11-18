package com.mahghuuuls.ashenskills.runtime;

import java.util.HashSet;
import java.util.Set;

/**
 * What one hit window may still damage. A window damages at most the configured number of targets
 * and each target once, however many times the skill strikes during that window.
 */
final class WindowBudget {

    private final int cap;
    private final Set<Integer> struck = new HashSet<>();

    /** @param cap the most targets the window may damage */
    WindowBudget(int cap) {
        this.cap = Math.max(0, cap);
    }

    /** How many more targets the window may damage. */
    int left() {
        return cap - struck.size();
    }

    /** The ids of the entities this window has damaged. The strike adds to it. */
    Set<Integer> struck() {
        return struck;
    }
}
