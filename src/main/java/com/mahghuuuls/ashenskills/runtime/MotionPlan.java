package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillMotion;

/**
 * A skill's motion as steps of server ticks: for a number of ticks the user is pushed the same
 * distance each tick, and in the tick after the last push the user is stopped, so the travel ends
 * where the skill says and does not slide on. Knows nothing about the game, so it can be tested
 * alone.
 */
final class MotionPlan {

    /** What to do with the user in one tick of the execution. */
    enum Step {
        PUSH, STOP, NOTHING
    }

    /**
     * The fastest push, in blocks per tick: the game's speed message to a client cannot carry
     * more, so a faster push would move the player less than the skill says.
     */
    static final double FASTEST_BLOCKS_PER_TICK = 3.9;

    /**
     * The server trusts where it sees the player when more than this many ticks have run since the
     * stop tick: the player's own
     * client moves the player, and its last positions of the travel arrive a little later.
     */
    static final int SETTLE_TICKS = 5;

    private final int pushTicks;
    private final double distance;
    private final double blocksPerTick;
    private final double directionX;
    private final double directionZ;

    /**
     * @param yawDegrees the direction fixed when the skill executed, as the game measures a
     *     player's facing: 0 looks toward positive z, 90 toward negative x
     */
    MotionPlan(SkillMotion motion, float yawDegrees) {
        // A motion with no time still takes one tick: a player is never placed, only pushed.
        // A motion too fast for the game takes the ticks the fastest push needs.
        long asked = Math.max(1, Math.round(motion.seconds() * SessionTimeline.TICKS_PER_SECOND));
        long needed = (long) Math.ceil(motion.distance() / FASTEST_BLOCKS_PER_TICK);
        this.pushTicks = (int) Math.min(Integer.MAX_VALUE, Math.max(asked, needed));
        this.distance = motion.distance();
        this.blocksPerTick = Math.min(FASTEST_BLOCKS_PER_TICK, motion.distance() / pushTicks);
        double yaw = Math.toRadians(yawDegrees);
        this.directionX = -Math.sin(yaw);
        this.directionZ = Math.cos(yaw);
    }

    /** @param executionTicksElapsed ticks of the execution that have run before this one */
    Step stepAt(int executionTicksElapsed) {
        if (executionTicksElapsed < pushTicks) {
            return Step.PUSH;
        }
        return executionTicksElapsed == pushTicks ? Step.STOP : Step.NOTHING;
    }

    int pushTicks() {
        return pushTicks;
    }

    /** Whether this plan carries the user anywhere at all. */
    boolean moves() {
        return distance > 0;
    }

    /**
     * Whether the travel is over long enough that the place the server sees the player in is the
     * place the travel left the player in.
     *
     * @param executionTicksElapsed ticks of the execution that have run before this one
     */
    boolean settledAt(int executionTicksElapsed) {
        return executionTicksElapsed - SETTLE_TICKS > pushTicks;
    }

    /** The speed along x during a push, in blocks per tick. */
    double pushX() {
        return directionX * blocksPerTick;
    }

    /** The speed along z during a push, in blocks per tick. */
    double pushZ() {
        return directionZ * blocksPerTick;
    }

    /**
     * How far a point lies from a start along this plan's direction: the travel so far. Never
     * below 0 and never above the plan's distance, whatever else moved the player meanwhile.
     */
    double travelled(double startX, double startZ, double x, double z) {
        double along = (x - startX) * directionX + (z - startZ) * directionZ;
        return Math.max(0.0, Math.min(distance, along));
    }
}
