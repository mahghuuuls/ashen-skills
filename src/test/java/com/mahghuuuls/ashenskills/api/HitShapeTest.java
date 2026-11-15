package com.mahghuuuls.ashenskills.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HitShapeTest {

    /** A box 0.6 wide, like a zombie, standing at the given point. The height plays no part. */
    private static boolean hits(HitShape shape, float yaw, double x, double unusedY, double z) {
        return shape.contains(0, 0, yaw, x - 0.3, z - 0.3, x + 0.3, z + 0.3);
    }

    /** A point as far as {@code distance} from the user, {@code degrees} to the side of +Z. */
    private static boolean hitsAt(HitShape shape, double distance, double degrees) {
        double angle = Math.toRadians(degrees);
        return hits(shape, 0, -distance * Math.sin(angle), 0, distance * Math.cos(angle));
    }

    private static final HitShape SLASH = HitShape.arc(3.5, 110);

    @Test
    void yawZeroFacesPlusZAndYawNinetyFacesMinusX() {
        assertTrue(hits(SLASH, 0, 0, 0, 2));
        assertFalse(hits(SLASH, 0, 0, 0, -2));
        assertTrue(hits(SLASH, 90, -2, 0, 0));
        assertFalse(hits(SLASH, 90, 2, 0, 0));
        assertTrue(hits(SLASH, 180, 0, 0, -2));
        assertTrue(hits(SLASH, -90, 2, 0, 0));
    }

    @Test
    void theArcEndsAtHalfItsAngleToEachSide() {
        assertTrue(hitsAt(SLASH, 2, 54));
        assertTrue(hitsAt(SLASH, 2, -54));
        assertFalse(hitsAt(SLASH, 2, 56));
        assertFalse(hitsAt(SLASH, 2, -56));
        assertFalse(hitsAt(SLASH, 2, 180));
    }

    @Test
    void theArcReachesAsFarAsTheNearestPointOfTheTarget() {
        assertTrue(hits(SLASH, 0, 0, 0, 3.7));
        assertFalse(hits(SLASH, 0, 0, 0, 3.9));
        // A wide box whose center is beyond reach but whose near face is within it.
        assertTrue(SLASH.contains(0, 0, 0, -1.0, 3.0, 1.0, 5.0));
    }

    @Test
    void aTargetStandingOnTheUserIsInTheArc() {
        assertTrue(hits(SLASH, 0, 0, 0, 0));
    }

    @Test
    void aLineCoversAStripInFrontOfTheUserOnly() {
        HitShape lunge = HitShape.line(4.0, 1.0);
        assertTrue(hits(lunge, 0, 0, 0, 3));
        assertTrue(hits(lunge, 0, 0.7, 0, 3));
        assertFalse(hits(lunge, 0, 0.9, 0, 3));
        assertTrue(hits(lunge, 0, 0, 0, 4.2));
        assertFalse(hits(lunge, 0, 0, 0, 4.4));
        assertFalse(hits(lunge, 0, 0, 0, -1));
        assertTrue(hits(lunge, 90, -3, 0, 0));
        assertFalse(hits(lunge, 90, 0, 0, 3));
    }

    @Test
    void aLineFromBehindAlsoCoversTheGroundBehindTheUserAndNothingMore() {
        HitShape thrust = HitShape.lineFromBehind(6.0, 4.0, 1.0);

        assertTrue(hits(thrust, 0, 0, 0, -5.8));
        assertTrue(hits(thrust, 0, 0, 0, -6.2));
        assertFalse(hits(thrust, 0, 0, 0, -6.4));
        assertTrue(hits(thrust, 0, 0, 0, 4.2));
        assertFalse(hits(thrust, 0, 0, 0, 4.4));
        assertFalse(hits(thrust, 0, 0.9, 0, -3));
        // What a search for targets must cover: the longer of the two ends.
        assertEquals(4.0, thrust.reach());
        assertEquals(6.0, thrust.farthest());
        assertEquals(4.0, HitShape.lineFromBehind(2.5, 4.0, 1.0).farthest());
        assertEquals(4.0, HitShape.line(4.0, 1.0).farthest());
    }

    @Test
    void aShapeNeedsPositiveNumbers() {
        assertThrows(IllegalArgumentException.class, () -> HitShape.arc(0, 90));
        assertThrows(IllegalArgumentException.class, () -> HitShape.line(3, 0));
        assertThrows(IllegalArgumentException.class, () -> HitShape.lineFromBehind(-1, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> HitShape.lineFromBehind(Double.NaN, 3, 1));
    }
}
