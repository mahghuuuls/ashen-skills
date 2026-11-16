package com.mahghuuuls.ashenskills.skill.builtin;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillMotion;
import com.mahghuuuls.ashenskills.api.SkillNumber;

/**
 * A strike that carries the user forward and hits what stands in a band: from where the dash
 * began to the skill's reach ahead of the user, as wide as the skill's width, so what the user
 * ran through is struck too. A narrow band is a thrust; a wide one is a cut made on the run.
 */
public final class DashStrike implements SkillBehavior {

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
