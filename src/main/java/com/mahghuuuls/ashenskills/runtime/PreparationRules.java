package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;

/**
 * What ends a preparation (a held stance, a charge) without the skill executing. A preparation
 * that ends this way costs nothing and starts no cooldown. Releasing the key is not here: what a
 * release means depends on the kind of skill, and the runtime decides that.
 */
final class PreparationRules {

    private PreparationRules() {
    }

    /**
     * The check made on every tick of a preparation, in a fixed order.
     *
     * @param weaponSwitched whether the main-hand item is no longer the weapon the skill started on
     * @param stateAllowed whether the player's state (ground, riding, ...) still allows the skill
     * @param usingItem whether the player raises a shield or uses an item; that action goes on
     * @return why the preparation ends now, or null when it continues
     */
    static String endCause(boolean weaponSwitched, boolean stateAllowed, boolean usingItem) {
        if (weaponSwitched) {
            return "weaponSwitched";
        }
        if (!stateAllowed) {
            return "stateNotAllowed";
        }
        return usingItem ? "itemUse" : null;
    }

    /** Whether damage, or knockback, ends the preparation of this skill. */
    static boolean endedByHurt(SkillDefinition skill, boolean knockback) {
        return skill.flag(knockback
                ? SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK
                : SkillFlag.PREPARATION_CANCELED_BY_DAMAGE);
    }
}
