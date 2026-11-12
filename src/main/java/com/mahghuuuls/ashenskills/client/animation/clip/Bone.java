package com.mahghuuuls.ashenskills.client.animation.clip;

/** The parts of a player a clip can move. */
public enum Bone {

    HEAD("head"),
    BODY("body"),
    RIGHT_ARM("right_arm"),
    LEFT_ARM("left_arm"),
    RIGHT_LEG("right_leg"),
    LEFT_LEG("left_leg"),
    /**
     * The held weapon. Its rotation turns the weapon inside the hand, around the grip, on the axes
     * of the arm that holds it. Without it the weapon sits as the game holds it, at a right angle
     * to the arm.
     */
    ITEM("item"),
    /**
     * The user's own camera, first-person view only. Its rotation is pitch (x), yaw (y) and roll
     * (z) added to the view at full camera effect intensity.
     */
    CAMERA("camera");

    private final String fileName;

    Bone(String fileName) {
        this.fileName = fileName;
    }

    /** The name used in clip files. */
    public String fileName() {
        return fileName;
    }

    /** @return the bone with that clip-file name, or null */
    public static Bone byFileName(String name) {
        for (Bone bone : values()) {
            if (bone.fileName.equals(name)) {
                return bone;
            }
        }
        return null;
    }
}
