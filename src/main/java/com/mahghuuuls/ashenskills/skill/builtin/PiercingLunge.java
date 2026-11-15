package com.mahghuuuls.ashenskills.skill.builtin;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillMotion;
import com.mahghuuuls.ashenskills.api.SkillNumber;

/**
 * A thrust that carries the user forward and strikes what stands in a narrow line: from where the
 * thrust began to the skill's reach ahead of the user, so what the user ran through is struck too.
 */
public final class PiercingLunge implements SkillBehavior {

    @Override
    public SkillMotion motionAtExecution(SkillContext context) {
        return new SkillMotion(
                context.skill().number(SkillNumber.TRAVEL_DISTANCE),
                context.skill().number(SkillNumber.TRAVEL_TIME));
    }

    @Override
    public void onHitWindow(SkillContext context, int windowIndex) {
        context.hit(HitShape.lineFromBehind(
                context.travelled(),
                context.skill().number(SkillNumber.REACH),
                context.skill().number(SkillNumber.WIDTH)));
    }
}
