package com.mahghuuuls.ashenskills.client.animation.clip;

import java.util.Map;

/** Reads a clip at a moment. Knows nothing about the game, so it can be tested alone. */
public final class ClipSampler {

    private static final double MILLIS_PER_TICK = 50.0;

    private ClipSampler() {
    }

    /**
     * @param seconds time since the clip began; before 0 gives the first pose, after the clip's
     *     length the last pose
     */
    public static Pose pose(Clip clip, double seconds) {
        double time = Math.max(0, Math.min(clip.lengthSeconds(), seconds));
        Pose pose = new Pose();
        for (Map.Entry<Bone, Track> rotation : clip.rotations().entrySet()) {
            pose.setRotation(rotation.getKey(), rotation.getValue().at(time));
        }
        for (Map.Entry<Bone, Track> position : clip.positions().entrySet()) {
            pose.setPosition(position.getKey(), position.getValue().at(time));
        }
        return pose;
    }

    /**
     * The time in a clip that belongs to a moment of a phase. A clip covers its whole phase: when
     * a pack makes the phase longer or shorter than the clip, the clip is stretched to fit, so the
     * motion always starts with the phase and ends with it.
     *
     * @param secondsIntoPhase time since the phase began
     * @param phaseSeconds length of the phase as the server runs it
     */
    public static double clipTime(Clip clip, double secondsIntoPhase, double phaseSeconds) {
        if (phaseSeconds <= 0) {
            return clip.lengthSeconds();
        }
        return clip.lengthSeconds() * Math.max(0, Math.min(1, secondsIntoPhase / phaseSeconds));
    }

    /**
     * How far into a phase a player is now, from what the server last reported.
     *
     * @param elapsedTicksAtReport ticks of the phase that had passed when the server reported it
     * @param millisSinceReport real time since that report arrived
     */
    public static double secondsIntoPhase(int elapsedTicksAtReport, long millisSinceReport) {
        return (elapsedTicksAtReport * MILLIS_PER_TICK + Math.max(0, millisSinceReport)) / 1000.0;
    }
}
