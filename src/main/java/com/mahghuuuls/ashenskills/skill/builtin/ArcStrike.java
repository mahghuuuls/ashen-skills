package com.mahghuuuls.ashenskills.skill.builtin;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillNumber;

/**
 * One strike in an arc in front of the user, as far and as wide as the skill's reach and arc say.
 * What makes two such skills differ (how they start, their numbers, their animations) is not here.
 */
public final class ArcStrike implements SkillBehavior {

    @Override
    public void onHitWindow(SkillContext context, int windowIndex) {
        context.hit(HitShape.arc(
                context.skill().number(SkillNumber.REACH),
                context.skill().number(SkillNumber.ARC)));
    }
}
