package com.mahghuuuls.ashenskills.client.animation.clip;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Where a clip puts each of its bones at one moment. */
public final class Pose {

    private final Map<Bone, double[]> rotations = new EnumMap<>(Bone.class);
    private final Map<Bone, double[]> positions = new EnumMap<>(Bone.class);

    Pose() {
    }

    void setRotation(Bone bone, double[] degrees) {
        rotations.put(bone, degrees);
    }

    void setPosition(Bone bone, double[] offset) {
        positions.put(bone, offset);
    }

    /** @return degrees around x, y, z, or null when the clip leaves this bone's rotation alone */
    public double[] rotationDegrees(Bone bone) {
        return rotations.get(bone);
    }

    /** @return radians around x, y, z, or null when the clip leaves this bone's rotation alone */
    public float[] rotationRadians(Bone bone) {
        double[] degrees = rotations.get(bone);
        if (degrees == null) {
            return null;
        }
        return new float[] {
            (float) Math.toRadians(degrees[0]), (float) Math.toRadians(degrees[1]), (float) Math.toRadians(degrees[2])
        };
    }

    /** @return offset in model units along x, y, z, or null when the clip leaves this bone in place */
    public double[] position(Bone bone) {
        return positions.get(bone);
    }

    /**
     * The same pose for a left-handed player: what the right arm and leg did the left ones do, and
     * every turn to one side becomes a turn to the other. Clips are written for a right-handed player.
     */
    public Pose mirrored() {
        Pose mirror = new Pose();
        for (Map.Entry<Bone, double[]> rotation : rotations.entrySet()) {
            double[] degrees = rotation.getValue();
            mirror.rotations.put(otherSide(rotation.getKey()), new double[] {degrees[0], -degrees[1], -degrees[2]});
        }
        for (Map.Entry<Bone, double[]> position : positions.entrySet()) {
            double[] offset = position.getValue();
            mirror.positions.put(otherSide(position.getKey()), new double[] {-offset[0], offset[1], offset[2]});
        }
        return mirror;
    }

    private static Bone otherSide(Bone bone) {
        switch (bone) {
            case RIGHT_ARM:
                return Bone.LEFT_ARM;
            case LEFT_ARM:
                return Bone.RIGHT_ARM;
            case RIGHT_LEG:
                return Bone.LEFT_LEG;
            case LEFT_LEG:
                return Bone.RIGHT_LEG;
            default:
                return bone;
        }
    }

    /** All values on one line, bones in a fixed order, two decimals: for the log and for comparing. */
    public String describe() {
        StringBuilder text = new StringBuilder();
        for (Bone bone : Bone.values()) {
            append(text, bone, "rot", rotations.get(bone));
            append(text, bone, "pos", positions.get(bone));
        }
        return text.length() == 0 ? "empty" : text.toString().trim();
    }

    private static void append(StringBuilder text, Bone bone, String kind, double[] values) {
        if (values != null) {
            text.append(String.format(Locale.ROOT, " %s.%s=[%.2f,%.2f,%.2f]",
                    bone.fileName(), kind, values[0], values[1], values[2]));
        }
    }
}
