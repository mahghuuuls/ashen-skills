package com.mahghuuuls.ashenskills.client.hud;

/**
 * Where a HUD part is drawn and how large, after the player's offset and scale. The part grows
 * and shrinks around its own middle, the offset then moves it, and a part that would leave the
 * screen stops at the screen edge. With no offset and scale 1 a part that is on the screen stays
 * exactly at its normal place.
 */
public final class HudPlacement {

    /** The left edge of the drawn part, in GUI pixels. */
    public final double x;
    /** The top edge of the drawn part, in GUI pixels. */
    public final double y;
    /** How many times its normal size the part is drawn. */
    public final double scale;
    /** The width of the drawn part, in GUI pixels. */
    public final double width;
    /** The height of the drawn part, in GUI pixels. */
    public final double height;

    private HudPlacement(double x, double y, double scale, double width, double height) {
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.width = width;
        this.height = height;
    }

    /**
     * @param normalX the left edge of the part at its normal place
     * @param normalY the top edge of the part at its normal place
     * @param width the width of the part at its normal size
     * @param height the height of the part at its normal size
     * @param offsetX GUI pixels to the right of the normal place
     * @param offsetY GUI pixels below the normal place
     */
    public static HudPlacement of(int screenWidth, int screenHeight, double normalX, double normalY,
            int width, int height, int offsetX, int offsetY, double scale) {
        double drawnWidth = width * scale;
        double drawnHeight = height * scale;
        double x = normalX + (width - drawnWidth) / 2 + offsetX;
        double y = normalY + (height - drawnHeight) / 2 + offsetY;
        return new HudPlacement(onScreen(x, drawnWidth, screenWidth), onScreen(y, drawnHeight, screenHeight), scale,
                drawnWidth, drawnHeight);
    }

    /**
     * This part where it does not cover the other part. Two parts can end up on each other, for
     * example when the screen edge stops both; this part then goes straight above the other
     * one, or below it when there is no room above.
     */
    public HudPlacement clearOf(HudPlacement other, int screenHeight) {
        boolean overlaps = x < other.x + other.width && other.x < x + width
                && y < other.y + other.height && other.y < y + height;
        if (!overlaps) {
            return this;
        }
        double above = other.y - height;
        double clear = above >= 0 ? above : onScreen(other.y + other.height, height, screenHeight);
        return new HudPlacement(x, clear, scale, width, height);
    }

    /** The place and the size as one text, for the diagnostic log. */
    public String describe() {
        return x + "," + y + " scale " + scale;
    }

    /** A part larger than the screen starts at the screen's left or top edge. */
    private static double onScreen(double start, double length, int screen) {
        return Math.max(0, Math.min(start, screen - length));
    }
}
