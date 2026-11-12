package com.mahghuuuls.ashenskills.client.animation.clip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Three numbers (x, y, z) that change over time, given as keys in rising time order. */
public final class Track {

    /** The value at one moment, and how the motion arrives at it from the key before. */
    public static final class Key {

        final double seconds;
        final double x;
        final double y;
        final double z;
        final Easing easing;

        public Key(double seconds, double x, double y, double z, Easing easing) {
            this.seconds = seconds;
            this.x = x;
            this.y = y;
            this.z = z;
            this.easing = easing;
        }

        public double seconds() {
            return seconds;
        }
    }

    private final List<Key> keys;

    /** @param keys at least one key, in strictly rising time order */
    public Track(List<Key> keys) {
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("A track needs at least one key");
        }
        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i).seconds <= keys.get(i - 1).seconds) {
                throw new IllegalArgumentException("Track keys must be in rising time order");
            }
        }
        this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
    }

    public List<Key> keys() {
        return keys;
    }

    /**
     * The value at a time. Before the first key it is the first key's value; after the last key it
     * is the last key's value.
     *
     * @return x, y, z
     */
    public double[] at(double seconds) {
        Key first = keys.get(0);
        if (seconds <= first.seconds) {
            return new double[] {first.x, first.y, first.z};
        }
        for (int i = 1; i < keys.size(); i++) {
            Key to = keys.get(i);
            if (seconds < to.seconds) {
                Key from = keys.get(i - 1);
                double share = to.easing.apply((seconds - from.seconds) / (to.seconds - from.seconds));
                return new double[] {
                    from.x + (to.x - from.x) * share, from.y + (to.y - from.y) * share, from.z + (to.z - from.z) * share
                };
            }
        }
        Key last = keys.get(keys.size() - 1);
        return new double[] {last.x, last.y, last.z};
    }
}
