package com.mahghuuuls.ashenskills.api;

/**
 * What a skill does, as code. The mod calls these methods on the server at the right moments; the
 * skill acts only through the {@link SkillContext} it is given. Costs, cooldown, phase timing,
 * interruption and cleanup are handled by the mod and are the same for every skill.
 */
public interface SkillBehavior {

    /**
     * Asked once, at the moment the skill executes: how the skill moves its user during the
     * execution. The answer holds for this whole use. The mod does the moving, and it stops the
     * motion when the execution ends early. Read values from the context here; do not strike.
     *
     * <p>A use that moves its user is not interrupted for where the motion takes the user (into
     * the air past a ledge, into water) until the user is back in a state the skill allows.
     *
     * @return the motion, or null for a skill that does not move its user
     */
    default SkillMotion motionAtExecution(SkillContext context) {
        return null;
    }

    /**
     * One of the skill's hit windows has been reached. Usually calls {@link SkillContext#hit}.
     *
     * @param windowIndex 0 for the first window of this skill use
     */
    void onHitWindow(SkillContext context, int windowIndex);
}
