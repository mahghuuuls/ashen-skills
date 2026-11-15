package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillMotion;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.runtime.MotionPlan.Step;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import com.mahghuuuls.ashenskills.skill.builtin.PiercingLunge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class MotionPlanTest {

    private static final double EXACT = 1.0e-9;

    /** The shipped Piercing Lunge: 4 blocks in 0.3 seconds. */
    private static MotionPlan lunge(float yaw) {
        return new MotionPlan(new SkillMotion(4.0, 0.3), yaw);
    }

    @Test
    void theTravelIsSpreadEvenlyOverItsTicksAndAddsUpToTheDistance() {
        MotionPlan plan = lunge(0);

        assertEquals(6, plan.pushTicks());
        assertEquals(4.0 / 6, plan.pushZ(), EXACT);
        assertEquals(0.0, plan.pushX(), EXACT);
        assertEquals(4.0, plan.pushTicks() * Math.hypot(plan.pushX(), plan.pushZ()), EXACT);
    }

    @Test
    void thePlayerIsPushedForEveryTravelTickThenStoppedOnceThenLeftAlone() {
        MotionPlan plan = lunge(0);

        for (int tick = 0; tick < 6; tick++) {
            assertEquals(Step.PUSH, plan.stepAt(tick), "tick " + tick);
        }
        assertEquals(Step.STOP, plan.stepAt(6));
        assertEquals(Step.NOTHING, plan.stepAt(7));
        assertEquals(Step.NOTHING, plan.stepAt(40));
    }

    @Test
    void theDirectionFollowsTheGamesFacing() {
        // Facing 0 looks toward positive z; 90 toward negative x; 180 toward negative z; -90 toward positive x.
        assertEquals(-4.0 / 6, lunge(90).pushX(), EXACT);
        assertEquals(0.0, lunge(90).pushZ(), EXACT);
        assertEquals(-4.0 / 6, lunge(180).pushZ(), EXACT);
        assertEquals(4.0 / 6, lunge(-90).pushX(), EXACT);
        assertEquals(4.0 / 6, Math.hypot(lunge(37).pushX(), lunge(37).pushZ()), EXACT);
    }

    @Test
    void theTravelSoFarIsMeasuredAlongTheDirectionOnly() {
        MotionPlan plan = lunge(0);

        assertEquals(3.2, plan.travelled(10, 20, 10, 23.2), EXACT);
        // A step sideways is not travel, and neither is a step back.
        assertEquals(0.0, plan.travelled(10, 20, 15, 20), EXACT);
        assertEquals(0.0, plan.travelled(10, 20, 10, 19), EXACT);
        // Whatever else moved the player far away, the travel is never more than the plan's distance.
        assertEquals(4.0, plan.travelled(10, 20, 10, 5000), EXACT);
        assertEquals(2.0, lunge(90).travelled(10, 20, 8, 20), EXACT);
    }

    @Test
    void aMotionWithNoTimeStillTakesOneTick() {
        MotionPlan plan = new MotionPlan(new SkillMotion(2.0, 0.0), 0);

        assertEquals(1, plan.pushTicks());
        assertEquals(2.0, plan.pushZ(), EXACT);
        assertEquals(Step.PUSH, plan.stepAt(0));
        assertEquals(Step.STOP, plan.stepAt(1));
    }

    @Test
    void aMotionCannotGoBackwardsOrTakeLessThanNoTime() {
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(-1, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(4, -0.1));
    }

    @Test
    void aMotionNeedsRealNumbers() {
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(Double.NaN, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(4, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(Double.POSITIVE_INFINITY, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new SkillMotion(4, Double.POSITIVE_INFINITY));
    }

    @Test
    void aMotionFasterThanTheGameCanMoveAPlayerTakesTheTimeTheFastestPushNeeds() {
        // 40 blocks in 0.1 seconds would be 20 blocks per tick.
        MotionPlan plan = new MotionPlan(new SkillMotion(40.0, 0.1), 0);

        assertEquals(11, plan.pushTicks());
        assertTrue(plan.pushZ() <= MotionPlan.FASTEST_BLOCKS_PER_TICK);
        assertEquals(40.0, plan.pushTicks() * plan.pushZ(), EXACT);
        // At the limit exactly, nothing changes.
        assertEquals(2, new MotionPlan(new SkillMotion(7.8, 0.1), 0).pushTicks());
        // A distance beyond every sense still gives a plan, and a push the game can carry.
        MotionPlan endless = new MotionPlan(new SkillMotion(1.0e300, 0.1), 0);
        assertEquals(Integer.MAX_VALUE, endless.pushTicks());
        assertEquals(MotionPlan.FASTEST_BLOCKS_PER_TICK, endless.pushZ(), EXACT);
    }

    @Test
    void theTravelIsSettledSomeTicksAfterTheStopAndNotBefore() {
        MotionPlan plan = lunge(0);

        // Six pushes (ticks 0 to 5), the stop at tick 6, then the settle time.
        assertFalse(plan.settledAt(6));
        assertFalse(plan.settledAt(6 + MotionPlan.SETTLE_TICKS - 1));
        assertFalse(plan.settledAt(6 + MotionPlan.SETTLE_TICKS));
        assertTrue(plan.settledAt(7 + MotionPlan.SETTLE_TICKS));
    }

    @Test
    void onlyAMotionWithADistanceCarriesTheUserSomewhere() {
        assertTrue(lunge(0).moves());
        assertFalse(new MotionPlan(new SkillMotion(0.0, 0.3), 0).moves());
    }

    @Test
    void theShippedPiercingLungeStopsInsideItsExecution() {
        SkillDefinition lunge = BuiltInSkills.definitionOf("piercing_lunge", DefaultsData.loadBundled());
        MotionPlan plan = new MotionPlan(
                new SkillMotion(lunge.number(SkillNumber.TRAVEL_DISTANCE), lunge.number(SkillNumber.TRAVEL_TIME)), 0);
        int executionTicks = SessionTimeline.toTicks(lunge.number(SkillNumber.EXECUTION_LENGTH));

        // The stop tick is a tick of the execution, so the player stands when recovery begins.
        assertEquals(Step.STOP, plan.stepAt(plan.pushTicks()));
        assertTrue(plan.pushTicks() < executionTicks, plan.pushTicks() + " push ticks, " + executionTicks + " execution ticks");
    }

    @Test
    void aSkillThatDoesNotSayOtherwiseDoesNotMoveItsUser() {
        SkillBehavior plain = (context, window) -> { };

        assertNull(plain.motionAtExecution(null));
    }

    @Test
    void piercingLungeTakesItsMotionFromItsTwoTravelValuesAndStrikesFromWhereItBegan() {
        SkillDefinition lunge = BuiltInSkills.definitionOf("piercing_lunge", DefaultsData.loadBundled());
        List<HitShape> struck = new ArrayList<>();
        SkillContext context = new SkillContext() {
            @Override
            public EntityPlayer user() {
                return null;
            }

            @Override
            public ItemStack weapon() {
                return null;
            }

            @Override
            public SkillDefinition skill() {
                return lunge;
            }

            @Override
            public float facingYaw() {
                return 0;
            }

            @Override
            public double chargeBonus() {
                return 0;
            }

            @Override
            public double travelled() {
                return 2.5;
            }

            @Override
            public int hit(HitShape shape) {
                struck.add(shape);
                return 0;
            }
        };
        PiercingLunge behavior = new PiercingLunge();

        SkillMotion motion = behavior.motionAtExecution(context);
        assertEquals(4.0, motion.distance());
        assertEquals(0.3, motion.seconds());

        behavior.onHitWindow(context, 0);
        assertEquals(1, struck.size());
        HitShape shape = struck.get(0);
        // The user stands at z 2.5 after 2.5 blocks of travel from z 0, facing positive z.
        assertTrue(shape.contains(0, 2.5, 0, -0.3, 0.7, 0.3, 1.3), "a target the user ran through");
        assertTrue(shape.contains(0, 2.5, 0, -0.3, 5.7, 0.3, 6.3), "a target ahead, inside the reach");
        assertFalse(shape.contains(0, 2.5, 0, -0.3, -1.5, 0.3, -0.9), "a target behind where the thrust began");
        assertFalse(shape.contains(0, 2.5, 0, -0.3, 7.0, 0.3, 7.6), "a target beyond the reach");
        assertFalse(shape.contains(0, 2.5, 0, 1.0, 0.7, 1.6, 1.3), "a target beside the line");
    }
}
