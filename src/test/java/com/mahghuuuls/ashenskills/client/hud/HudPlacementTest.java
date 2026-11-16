package com.mahghuuuls.ashenskills.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudPlacementTest {

    // A 427x240 screen is the game's 854x480 window at GUI scale 2; the skill HUD frame is 24x24.
    private static final int SCREEN_WIDTH = 427;
    private static final int SCREEN_HEIGHT = 240;
    private static final int FRAME = 24;
    private static final int FRAME_X = 332;
    private static final int FRAME_Y = 216;

    private static HudPlacement frame(int offsetX, int offsetY, double scale) {
        return HudPlacement.of(SCREEN_WIDTH, SCREEN_HEIGHT, FRAME_X, FRAME_Y, FRAME, FRAME, offsetX, offsetY, scale);
    }

    private static void assertPlaced(double x, double y, double scale, HudPlacement place) {
        assertEquals(x, place.x, 1e-9, "x");
        assertEquals(y, place.y, 1e-9, "y");
        assertEquals(scale, place.scale, 1e-9, "scale");
    }

    @Test
    void withNoOffsetAndScaleOneAPartStaysAtItsNormalPlace() {
        assertPlaced(FRAME_X, FRAME_Y, 1, frame(0, 0, 1));
        // The same at every GUI scale of a 1920x1080 window: 1, 2, 3 and 4.
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            int width = 1920 / guiScale;
            int height = 1080 / guiScale;
            int x = width / 2 + 91 + 28;
            int y = height - FRAME;
            assertPlaced(x, y, 1, HudPlacement.of(width, height, x, y, FRAME, FRAME, 0, 0, 1));
        }
    }

    @Test
    void anOffsetMovesAPartRightAndDownOrLeftAndUp() {
        assertPlaced(FRAME_X + 40, FRAME_Y - 30, 1, frame(40, -30, 1));
        assertPlaced(FRAME_X - 100, FRAME_Y - 7, 1, frame(-100, -7, 1));
    }

    @Test
    void aPartGrowsAndShrinksAroundItsOwnMiddle() {
        // Twice the size, 40 to the left and 30 up: the middle moves by the offset alone.
        HudPlacement doubled = frame(-40, -30, 2);
        assertPlaced(FRAME_X - 40 - FRAME / 2.0, FRAME_Y - 30 - FRAME / 2.0, 2, doubled);
        assertEquals(FRAME_X - 40 + FRAME / 2.0, doubled.x + FRAME * doubled.scale / 2, 1e-9);

        HudPlacement halved = frame(-40, -30, 0.5);
        assertPlaced(FRAME_X - 40 + FRAME / 4.0, FRAME_Y - 30 + FRAME / 4.0, 0.5, halved);
    }

    @Test
    void aPartThatWouldLeaveTheScreenStopsAtEachEdge() {
        assertPlaced(SCREEN_WIDTH - FRAME, FRAME_Y, 1, frame(2000, 0, 1));
        assertPlaced(0, FRAME_Y, 1, frame(-2000, 0, 1));
        assertPlaced(FRAME_X, 0, 1, frame(0, -2000, 1));
        assertPlaced(FRAME_X, SCREEN_HEIGHT - FRAME, 1, frame(0, 2000, 1));
    }

    @Test
    void aLargerPartIsKeptOnTheScreenAsAWhole() {
        // At three times its size the frame at the bottom edge would hang out below; it is moved up.
        HudPlacement tripled = frame(0, 0, 3);
        assertPlaced(FRAME_X - FRAME, SCREEN_HEIGHT - 3 * FRAME, 3, tripled);
        // And at the right edge with an offset far to the right.
        assertPlaced(SCREEN_WIDTH - 3 * FRAME, SCREEN_HEIGHT - 3 * FRAME, 3, frame(2000, 2000, 3));
    }

    @Test
    void aLineThatDoesNotTouchTheFrameStaysWhereItIs() {
        // The refusal line above the hotbar and the frame beside the hotbar, both at their normal places.
        HudPlacement line = HudPlacement.of(SCREEN_WIDTH, SCREEN_HEIGHT, 170, 168, 87, 8, 0, 0, 1);
        assertPlaced(170, 168, 1, line.clearOf(frame(0, 0, 1), SCREEN_HEIGHT));
    }

    @Test
    void aLineStoppedOnTheFrameAtTheBottomEdgeGoesAboveTheFrame() {
        HudPlacement frame = frame(2000, 2000, 1);
        HudPlacement line = HudPlacement.of(SCREEN_WIDTH, SCREEN_HEIGHT, 170, 168, 87, 8, 2000, 2000, 1);
        assertPlaced(SCREEN_WIDTH - 87, SCREEN_HEIGHT - 8, 1, line);
        assertPlaced(SCREEN_WIDTH - 87, SCREEN_HEIGHT - FRAME - 8, 1, line.clearOf(frame, SCREEN_HEIGHT));
    }

    @Test
    void aLineStoppedOnTheFrameAtTheTopEdgeGoesBelowTheFrame() {
        HudPlacement frame = frame(-2000, -2000, 3);
        HudPlacement line = HudPlacement.of(SCREEN_WIDTH, SCREEN_HEIGHT, 170, 168, 87, 8, -2000, -2000, 3);
        assertPlaced(0, 0, 3, line);
        assertPlaced(0, 3 * FRAME, 3, line.clearOf(frame, SCREEN_HEIGHT));
    }

    @Test
    void aPartLargerThanTheScreenStartsAtTheLeftAndTopEdge() {
        assertPlaced(0, 0, 3, HudPlacement.of(50, 40, 10, 10, FRAME, FRAME, 5, 5, 3));
    }
}
