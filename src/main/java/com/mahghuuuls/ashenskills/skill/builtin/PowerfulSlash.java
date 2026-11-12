package com.mahghuuuls.ashenskills.skill.builtin;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillNumber;

/** A wide slash in front of the user. */
public final class PowerfulSlash implements SkillBehavior {

    @Override
    public void onHitWindow(SkillContext context, int windowIndex) {
        context.hit(HitShape.arc(
                context.skill().number(SkillNumber.REACH),
                context.skill().number(SkillNumber.ARC)));
    }
}
