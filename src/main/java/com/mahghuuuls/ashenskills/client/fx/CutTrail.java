package com.mahghuuuls.ashenskills.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;

/**
 * The trail a cut leaves in the air: a crescent around the user that the blade reveals as it
 * passes and that fades behind it. A trail is described by a small file of the skill, so a pack
 * or an addon shapes it without code. Angles are in degrees around the user, 0 straight ahead,
 * positive to the user's right; lengths are in blocks; times in seconds.
 */
public final class CutTrail {

    /** The part of the sweep time over which a place lights up once the blade has reached it. */
    private static final double RISE_PART_OF_SWEEP = 0.15;

    private final double startAngle;
    private final double endAngle;
    private final double innerRadius;
    private final double outerRadius;
    private final double height;
    private final double tilt;
    private final double sweepSeconds;
    private final double fadeSeconds;
    private final float red;
    private final float green;
    private final float blue;
    private final int sparks;

    private CutTrail(double startAngle, double endAngle, double innerRadius, double outerRadius, double height,
            double tilt, double sweepSeconds, double fadeSeconds, float red, float green, float blue, int sparks) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.height = height;
        this.tilt = tilt;
        this.sweepSeconds = sweepSeconds;
        this.fadeSeconds = fadeSeconds;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.sparks = sparks;
    }

    /**
     * Reads a trail file.
     *
     * @throws IllegalArgumentException when a value is missing, is not a number, or cannot make a trail
     */
    public static CutTrail parse(Reader file) {
        JsonObject json;
        try {
            json = new JsonParser().parse(file).getAsJsonObject();
        } catch (RuntimeException notJson) {
            throw new IllegalArgumentException("not a JSON object: " + notJson.getMessage());
        }
        double inner = number(json, "innerRadius");
        double outer = number(json, "outerRadius");
        double sweep = number(json, "sweep");
        double fade = number(json, "fade");
        require(inner >= 0 && outer > inner, "outerRadius must be above innerRadius, and innerRadius not below 0");
        require(outer <= 16, "outerRadius must not be above 16");
        require(sweep > 0 && sweep <= 5 && fade > 0 && fade <= 5, "sweep and fade must be above 0 and not above 5");
        double start = number(json, "startAngle");
        double end = number(json, "endAngle");
        require(start != end && Math.abs(end - start) <= 360, "startAngle and endAngle must differ, by 360 at most");
        JsonElement color = json.get("color");
        require(color != null && color.isJsonArray() && color.getAsJsonArray().size() == 3, "color must be three numbers");
        JsonArray rgb = color.getAsJsonArray();
        double sparkCount = json.has("sparks") ? number(json, "sparks") : 0;
        require(sparkCount >= 0 && sparkCount <= 1000 && sparkCount == Math.rint(sparkCount),
                "sparks must be a whole number from 0 to 1000");
        int sparks = (int) sparkCount;
        return new CutTrail(start, end, inner, outer, number(json, "height"),
                json.has("tilt") ? number(json, "tilt") : 0, sweep, fade,
                channel(rgb.get(0)), channel(rgb.get(1)), channel(rgb.get(2)), sparks);
    }

    private static double number(JsonObject json, String name) {
        JsonElement value = json.get(name);
        require(value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(),
                "'" + name + "' must be a number");
        double number = value.getAsDouble();
        require(!Double.isNaN(number) && !Double.isInfinite(number), "'" + name + "' must be a finite number");
        return number;
    }

    private static float channel(JsonElement value) {
        require(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(), "color must be three numbers");
        double channel = value.getAsDouble();
        require(channel >= 0 && channel <= 255, "a color number must be 0 to 255");
        return (float) (channel / 255.0);
    }

    private static void require(boolean holds, String problem) {
        if (!holds) {
            throw new IllegalArgumentException(problem);
        }
    }

    /** Whether nothing of the trail is left to see at this age. */
    public boolean isOver(double ageSeconds) {
        return ageSeconds >= sweepSeconds + fadeSeconds;
    }

    /**
     * How strong the trail is at one place along the cut.
     *
     * @param along where along the cut, 0 at its start and 1 at its end
     * @return 0 where the blade has not been yet or the trail has faded, up to 1 just behind the blade
     */
    public double strengthAt(double along, double ageSeconds) {
        double sincePassed = ageSeconds - along * sweepSeconds;
        if (sincePassed <= 0) {
            return 0;
        }
        // A place lights up quickly, not at once, so the front of the trail has no hard edge.
        double lit = Math.min(1, sincePassed / (RISE_PART_OF_SWEEP * sweepSeconds));
        return lit * Math.max(0, 1 - sincePassed / fadeSeconds);
    }

    /**
     * A point of the trail, as an offset from the user's feet.
     *
     * @param along where along the cut, 0 at its start and 1 at its end
     * @param across 0 at the inner edge of the crescent and 1 at its outer edge
     * @param yawDegrees the game's yaw of the direction the user faced at the cut
     * @return x, y and z in blocks
     */
    public double[] pointAt(double along, double across, double yawDegrees) {
        double angle = Math.toRadians(startAngle + (endAngle - startAngle) * along);
        double radius = innerRadius + (outerRadius - innerRadius) * across;
        // In the user's own frame: ahead, to the right, up. The tilt rolls the flat crescent around the line ahead.
        double ahead = Math.cos(angle) * radius;
        double flatRight = Math.sin(angle) * radius;
        double roll = Math.toRadians(tilt);
        double right = flatRight * Math.cos(roll);
        double up = height + flatRight * Math.sin(roll);
        // The game's yaw 0 looks along +z, and the user's right is then -x.
        double yaw = Math.toRadians(yawDegrees);
        double x = -Math.sin(yaw) * ahead - Math.cos(yaw) * right;
        double z = Math.cos(yaw) * ahead - Math.sin(yaw) * right;
        return new double[] {x, up, z};
    }

    public float red() {
        return red;
    }

    public float green() {
        return green;
    }

    public float blue() {
        return blue;
    }

    /** How many sparks fly off along the cut when it starts. */
    public int sparks() {
        return sparks;
    }
}
