package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class PreparationRulesTest {

    private static SkillDefinition stance(boolean endedByDamage, boolean endedByKnockback) {
        return SkillDefinition.builder(new ResourceLocation("test", "stance"), ActivationKind.STANCE)
                .flag(SkillFlag.PREPARATION_CANCELED_BY_DAMAGE, endedByDamage)
                .flag(SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK, endedByKnockback)
                .build();
    }

    @Test
    void aPreparationContinuesWhileNothingForbidsIt() {
        assertNull(PreparationRules.endCause(false, true, false));
    }

    @Test
    void aSwitchedWeaponAForbiddenStateOrAnItemUseEndsThePreparation() {
        assertEquals("weaponSwitched", PreparationRules.endCause(true, true, false));
        assertEquals("stateNotAllowed", PreparationRules.endCause(false, false, false));
        assertEquals("itemUse", PreparationRules.endCause(false, true, true));
    }

    @Test
    void theFirstCauseInTheFixedOrderIsTheAnswer() {
        assertEquals("weaponSwitched", PreparationRules.endCause(true, false, true));
        assertEquals("stateNotAllowed", PreparationRules.endCause(false, false, true));
    }

    @Test
    void damageAndKnockbackEachFollowTheirOwnRuleOfTheSkill() {
        assertTrue(PreparationRules.endedByHurt(stance(true, false), false));
        assertFalse(PreparationRules.endedByHurt(stance(true, false), true));
        assertTrue(PreparationRules.endedByHurt(stance(false, true), true));
        assertFalse(PreparationRules.endedByHurt(stance(false, true), false));
    }

    @Test
    void theShippedUnsheatheStanceEndsOnDamageAndOnKnockback() {
        SkillDefinition unsheathe = BuiltInSkills.definitionOf("unsheathe", DefaultsData.loadBundled());

        assertTrue(PreparationRules.endedByHurt(unsheathe, false));
        assertTrue(PreparationRules.endedByHurt(unsheathe, true));
    }
}
