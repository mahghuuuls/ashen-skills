package com.mahghuuuls.ashenskills.api;

/**
 * How a skill moves its user during the execution: straight ahead, in the direction the user faced
 * when the skill executed, a distance over a time. A skill only states this; the mod moves the
 * player, with the game's normal collision, so the motion stops at solid blocks and the player's
 * own movement keys do not steer it. The game cannot move a player faster than about 3.9 blocks in
 * a tick: a motion that asks for more takes the longer time that speed needs. A motion still
 * running when the execution ends is cut short there.
 */
public final class SkillMotion {

    private final double distance;
    private final double seconds;

    /**
     * @param distance how far the user travels on open ground, in blocks
     * @param seconds how long the travel takes, from the start of the execution
     */
    public SkillMotion(double distance, double seconds) {
        if (!(distance >= 0) || !(seconds >= 0) || Double.isInfinite(distance) || Double.isInfinite(seconds)) {
            throw new IllegalArgumentException("A motion needs a distance and a time that are numbers of 0 or more");
        }
        this.distance = distance;
        this.seconds = seconds;
    }

    public double distance() {
        return distance;
    }

    public double seconds() {
        return seconds;
    }
}
