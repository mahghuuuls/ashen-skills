package com.mahghuuuls.ashenskills.api;

/**
 * What a skill does, as code. The mod calls these methods on the server at the right moments; the
 * skill acts only through the {@link SkillContext} it is given. Costs, cooldown, phase timing,
 * interruption and cleanup are handled by the mod and are the same for every skill.
 */
public interface SkillBehavior {

    /**
     * One of the skill's hit windows has been reached. Usually calls {@link SkillContext#hit}.
     *
     * @param windowIndex 0 for the first window of this skill use
     */
    void onHitWindow(SkillContext context, int windowIndex);
}
