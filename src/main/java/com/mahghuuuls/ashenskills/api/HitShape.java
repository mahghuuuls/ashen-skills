package com.mahghuuuls.ashenskills.api;

/**
 * The area a strike covers on the ground plane, measured from the user along the direction the user
 * faced when the skill executed. A target is inside when its bounding box reaches into the area.
 * How far up and down a strike reaches, and whether a target is then hit (walls, caps, PvP rules),
 * is decided by the mod, not by the shape.
 */
public final class HitShape {

    private enum Kind { ARC, LINE }

    private final Kind kind;
    private final double reach;
    private final double size;

    private HitShape(Kind kind, double reach, double size) {
        if (reach <= 0 || size <= 0) {
            throw new IllegalArgumentException("A hit shape needs a positive reach and size");
        }
        this.kind = kind;
        this.reach = reach;
        this.size = size;
    }

    /**
     * A slice of a circle in front of the user.
     *
     * @param reach radius in blocks
     * @param arcDegrees full opening angle, centered on the facing direction, up to 360
     */
    public static HitShape arc(double reach, double arcDegrees) {
        return new HitShape(Kind.ARC, reach, Math.min(arcDegrees, 360.0));
    }

    /**
     * A straight strip in front of the user.
     *
     * @param reach length in blocks
     * @param width full width in blocks
     */
    public static HitShape line(double reach, double width) {
        return new HitShape(Kind.LINE, reach, width);
    }

    /** The farthest distance from the user at which anything can be inside the shape. */
    public double reach() {
        return reach;
    }

    /**
     * Whether a box reaches into the shape.
     *
     * @param originX user's position
     * @param originZ user's position
     * @param yawDegrees the facing direction as the game measures it: 0 looks toward +Z, 90 toward -X
     */
    public boolean contains(
            double originX, double originZ, float yawDegrees, double minX, double minZ, double maxX, double maxZ) {
        double yaw = Math.toRadians(yawDegrees);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double centerX = (minX + maxX) / 2 - originX;
        double centerZ = (minZ + maxZ) / 2 - originZ;
        return kind == Kind.ARC
                ? inArc(forwardX, forwardZ, centerX, centerZ, originX, originZ, minX, minZ, maxX, maxZ)
                : inLine(forwardX, forwardZ, centerX, centerZ, Math.max(maxX - minX, maxZ - minZ) / 2);
    }

    /** Close enough (nearest point of the box) and in the right direction (center of the box). */
    private boolean inArc(
            double forwardX, double forwardZ, double centerX, double centerZ,
            double originX, double originZ, double minX, double minZ, double maxX, double maxZ) {
        double nearestX = clamp(originX, minX, maxX) - originX;
        double nearestZ = clamp(originZ, minZ, maxZ) - originZ;
        if (nearestX * nearestX + nearestZ * nearestZ > reach * reach) {
            return false;
        }
        double centerDistance = Math.sqrt(centerX * centerX + centerZ * centerZ);
        if (centerDistance < 1.0e-6) {
            return true;
        }
        double cosine = (centerX * forwardX + centerZ * forwardZ) / centerDistance;
        return cosine >= Math.cos(Math.toRadians(size / 2)) - 1.0e-9;
    }

    /** The box, taken as a circle of its half width, overlaps the strip. */
    private boolean inLine(double forwardX, double forwardZ, double centerX, double centerZ, double halfBox) {
        double along = centerX * forwardX + centerZ * forwardZ;
        double across = Math.abs(centerX * forwardZ - centerZ * forwardX);
        return along + halfBox >= 0 && along - halfBox <= reach && across <= size / 2 + halfBox;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
