package com.mahghuuuls.ashenskills.client.fx;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps the impact particles of one use of a skill under the player's limit, however many targets
 * that use hits. Each player's current use has its own count; a new use starts from zero.
 */
final class ParticleBudget {

    /** Per user: the use being counted and how many particles it has spawned. */
    private final Map<Integer, int[]> spent = new HashMap<>();

    /**
     * @param userEntityId whose skill use
     * @param useId tells that user's uses apart
     * @param wanted how many particles the moment asks for
     * @param limit the most one use may spawn on this client
     * @return how many of the wanted particles may be spawned now
     */
    int take(int userEntityId, int useId, int wanted, int limit) {
        int[] use = spent.get(userEntityId);
        if (use == null || use[0] != useId) {
            use = new int[] {useId, 0};
            spent.put(userEntityId, use);
        }
        int granted = Math.max(0, Math.min(wanted, limit - use[1]));
        use[1] += granted;
        return granted;
    }

    void clear() {
        spent.clear();
    }
}
