package com.mahghuuuls.ashenskills.client.animation.clip;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * One animation of one skill phase for one view: for some bones, a rotation and a position that
 * change over time. A bone the clip does not name is left as the game poses it.
 */
public final class Clip {

    private final double lengthSeconds;
    private final Map<Bone, Track> rotations;
    private final Map<Bone, Track> positions;

    /**
     * @param rotations degrees around the x, y and z axis of the game's player model, per bone
     * @param positions offset from the bone's normal place, in model units (1/16 block), per bone
     */
    public Clip(double lengthSeconds, Map<Bone, Track> rotations, Map<Bone, Track> positions) {
        if (lengthSeconds <= 0) {
            throw new IllegalArgumentException("A clip needs a positive length");
        }
        this.lengthSeconds = lengthSeconds;
        this.rotations = Collections.unmodifiableMap(new EnumMap<>(rotations));
        this.positions = Collections.unmodifiableMap(new EnumMap<>(positions));
    }

    public double lengthSeconds() {
        return lengthSeconds;
    }

    Map<Bone, Track> rotations() {
        return rotations;
    }

    Map<Bone, Track> positions() {
        return positions;
    }
}
