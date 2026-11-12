package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.weapon.AttachedSkill;

/** The checks a skill must pass to start, in their fixed order. The first failure is the answer. */
final class ActivationRules {

    private ActivationRules() {
    }

    /**
     * @param skillStatus what the main-hand item carries
     * @param inSkillPhase whether the player is already using a skill
     * @param cooldownRemainingMillis time left on the exclusive cooldown, 0 when none runs
     * @param stateAllowed whether the player's state (ground, riding, ...) allows this skill
     * @return why the skill may not start, or null when it may
     */
    static RefusalReason firstRefusal(
            AttachedSkill.Status skillStatus, boolean inSkillPhase, long cooldownRemainingMillis, boolean stateAllowed) {
        switch (skillStatus) {
            case NONE:
                return RefusalReason.NO_SKILL;
            case UNAVAILABLE:
                return RefusalReason.SKILL_UNAVAILABLE;
            case INCOMPATIBLE:
                return RefusalReason.INCOMPATIBLE_WEAPON;
            default:
                break;
        }
        if (inSkillPhase) {
            return RefusalReason.BUSY;
        }
        if (cooldownRemainingMillis > 0) {
            return RefusalReason.ON_COOLDOWN;
        }
        return stateAllowed ? null : RefusalReason.NOT_ALLOWED_NOW;
    }
}
