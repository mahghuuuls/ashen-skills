package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class ActionLockTest {

    private static SkillDefinition skill(boolean blockExec, boolean useExec, boolean blockRec, boolean useRec) {
        return SkillDefinition.builder(new ResourceLocation("test", "slash"), ActivationKind.INSTANT)
                .flag(SkillFlag.BLOCKING_DURING_EXECUTION, blockExec)
                .flag(SkillFlag.ITEM_USE_DURING_EXECUTION, useExec)
                .flag(SkillFlag.BLOCKING_DURING_RECOVERY, blockRec)
                .flag(SkillFlag.ITEM_USE_DURING_RECOVERY, useRec)
                .build();
    }

    @Test
    void noSkillUseBlocksNothing() {
        assertFalse(ActionLock.blocksAttack(null));
    }

    @Test
    void eachPhaseAndEachKindOfUseFollowsItsOwnRule() {
        SkillDefinition onlyShieldInExecution = skill(true, false, false, false);
        assertTrue(ActionLock.allowsUse(onlyShieldInExecution, SkillPhase.EXECUTION, true));
        assertFalse(ActionLock.allowsUse(onlyShieldInExecution, SkillPhase.EXECUTION, false));
        assertFalse(ActionLock.allowsUse(onlyShieldInExecution, SkillPhase.RECOVERY, true));
        assertFalse(ActionLock.allowsUse(onlyShieldInExecution, SkillPhase.INTERRUPTED_RECOVERY, true));

        SkillDefinition onlyItemsInExecution = skill(false, true, false, false);
        assertTrue(ActionLock.allowsUse(onlyItemsInExecution, SkillPhase.EXECUTION, false));
        assertFalse(ActionLock.allowsUse(onlyItemsInExecution, SkillPhase.EXECUTION, true));
        assertFalse(ActionLock.allowsUse(onlyItemsInExecution, SkillPhase.RECOVERY, false));

        SkillDefinition onlyShieldInRecovery = skill(false, false, true, false);
        assertTrue(ActionLock.allowsUse(onlyShieldInRecovery, SkillPhase.RECOVERY, true));
        assertTrue(ActionLock.allowsUse(onlyShieldInRecovery, SkillPhase.INTERRUPTED_RECOVERY, true));
        assertFalse(ActionLock.allowsUse(onlyShieldInRecovery, SkillPhase.RECOVERY, false));
        assertFalse(ActionLock.allowsUse(onlyShieldInRecovery, SkillPhase.EXECUTION, true));

        SkillDefinition onlyItemsInRecovery = skill(false, false, false, true);
        assertTrue(ActionLock.allowsUse(onlyItemsInRecovery, SkillPhase.RECOVERY, false));
        assertTrue(ActionLock.allowsUse(onlyItemsInRecovery, SkillPhase.INTERRUPTED_RECOVERY, false));
        assertFalse(ActionLock.allowsUse(onlyItemsInRecovery, SkillPhase.RECOVERY, true));
        assertFalse(ActionLock.allowsUse(onlyItemsInRecovery, SkillPhase.EXECUTION, false));
    }

    private static SkillDefinition skillWith(boolean jumpDuringExecution, boolean sneakingDuringSkill) {
        return SkillDefinition.builder(new ResourceLocation("test", "slash"), ActivationKind.INSTANT)
                .flag(SkillFlag.JUMP_DURING_EXECUTION, jumpDuringExecution)
                .flag(SkillFlag.SNEAKING_DURING_SKILL, sneakingDuringSkill)
                .build();
    }

    @Test
    void theJumpLockHoldsOnlyDuringExecutionAndOnlyWhenTheSkillForbidsJumping() {
        SkillDefinition noJump = skillWith(false, true);
        assertTrue(ActionLock.locksJump(noJump, SkillPhase.EXECUTION));
        assertFalse(ActionLock.locksJump(noJump, SkillPhase.RECOVERY));
        assertFalse(ActionLock.locksJump(noJump, SkillPhase.INTERRUPTED_RECOVERY));

        assertFalse(ActionLock.locksJump(skillWith(true, true), SkillPhase.EXECUTION));
        assertFalse(ActionLock.blocksJump(null));
    }

    @Test
    void theSneakLockFollowsTheSkillAndNotTheJumpRule() {
        assertTrue(ActionLock.locksSneak(skillWith(true, false)));
        assertFalse(ActionLock.locksSneak(skillWith(false, true)));
        assertFalse(ActionLock.blocksSneak(null));
    }
}
