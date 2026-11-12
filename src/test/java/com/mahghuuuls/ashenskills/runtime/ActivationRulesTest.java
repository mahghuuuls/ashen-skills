package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.weapon.AttachedSkill.Status;
import org.junit.jupiter.api.Test;

class ActivationRulesTest {

    @Test
    void aUsableSkillWithNothingInTheWayMayStart() {
        assertNull(ActivationRules.firstRefusal(Status.COMPATIBLE, false, 0, true));
    }

    @Test
    void eachObstacleAloneGivesItsOwnReason() {
        assertEquals(RefusalReason.NO_SKILL, ActivationRules.firstRefusal(Status.NONE, false, 0, true));
        assertEquals(RefusalReason.SKILL_UNAVAILABLE, ActivationRules.firstRefusal(Status.UNAVAILABLE, false, 0, true));
        assertEquals(RefusalReason.INCOMPATIBLE_WEAPON, ActivationRules.firstRefusal(Status.INCOMPATIBLE, false, 0, true));
        assertEquals(RefusalReason.BUSY, ActivationRules.firstRefusal(Status.COMPATIBLE, true, 0, true));
        assertEquals(RefusalReason.ON_COOLDOWN, ActivationRules.firstRefusal(Status.COMPATIBLE, false, 1, true));
        assertEquals(RefusalReason.NOT_ALLOWED_NOW, ActivationRules.firstRefusal(Status.COMPATIBLE, false, 0, false));
    }

    @Test
    void withSeveralObstaclesTheEarlierCheckDecides() {
        assertEquals(RefusalReason.NO_SKILL, ActivationRules.firstRefusal(Status.NONE, true, 5000, false));
        assertEquals(RefusalReason.SKILL_UNAVAILABLE, ActivationRules.firstRefusal(Status.UNAVAILABLE, true, 5000, false));
        assertEquals(RefusalReason.INCOMPATIBLE_WEAPON, ActivationRules.firstRefusal(Status.INCOMPATIBLE, true, 5000, false));
        assertEquals(RefusalReason.BUSY, ActivationRules.firstRefusal(Status.COMPATIBLE, true, 5000, false));
        assertEquals(RefusalReason.ON_COOLDOWN, ActivationRules.firstRefusal(Status.COMPATIBLE, false, 5000, false));
    }

    @Test
    void onlyReasonsThePlayerCanActOnAreShown() {
        assertFalse(RefusalReason.NO_SKILL.isShownToPlayer());
        assertFalse(RefusalReason.BUSY.isShownToPlayer());
        assertTrue(RefusalReason.ON_COOLDOWN.isShownToPlayer());
        assertTrue(RefusalReason.INCOMPATIBLE_WEAPON.isShownToPlayer());
        assertTrue(RefusalReason.SKILL_UNAVAILABLE.isShownToPlayer());
        assertTrue(RefusalReason.NOT_ALLOWED_NOW.isShownToPlayer());
        assertEquals("ashenskills.refusal.on_cooldown", RefusalReason.ON_COOLDOWN.messageKey());
    }
}
