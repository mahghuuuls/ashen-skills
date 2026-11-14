package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.runtime.SessionTimeline.Step;
import org.junit.jupiter.api.Test;

class SessionTimelineTest {

    /** 0.5 s execution, 0.6 s recovery, 0.4 s interrupted recovery: 10, 12 and 8 ticks. */
    private static SessionTimeline timeline() {
        return new SessionTimeline(0.5, 0.6, 0.4);
    }

    private static int ticksUntil(SessionTimeline timeline, Step wanted) {
        for (int ticks = 1; ticks <= 1000; ticks++) {
            if (timeline.tick() == wanted) {
                return ticks;
            }
        }
        throw new AssertionError("never reached " + wanted);
    }

    @Test
    void secondsBecomeTicksAndNeverLessThanOne() {
        assertEquals(10, SessionTimeline.toTicks(0.5));
        assertEquals(12, SessionTimeline.toTicks(0.6));
        assertEquals(7, SessionTimeline.toTicks(0.35));
        assertEquals(1, SessionTimeline.toTicks(0.0));
        assertEquals(1, SessionTimeline.toTicks(0.01));
    }

    @Test
    void executionThenRecoveryLastTheirConfiguredTicks() {
        SessionTimeline timeline = timeline();
        assertEquals(SkillPhase.EXECUTION, timeline.phase());
        assertEquals(10, timeline.phaseLengthTicks());

        assertEquals(10, ticksUntil(timeline, Step.EXECUTION_ENDED));
        assertEquals(SkillPhase.RECOVERY, timeline.phase());
        assertEquals(0, timeline.ticksInPhase());
        assertEquals(12, timeline.phaseLengthTicks());

        assertEquals(12, ticksUntil(timeline, Step.FINISHED));
    }

    @Test
    void executionTicksAreCountedThroughTheLastOneAndNoFurther() {
        SessionTimeline timeline = timeline();
        assertEquals(0, timeline.executionTicksElapsed());
        timeline.tick();
        assertEquals(1, timeline.executionTicksElapsed());

        for (int tick = 2; tick <= 9; tick++) {
            assertEquals(Step.CONTINUE, timeline.tick());
        }
        assertEquals(Step.EXECUTION_ENDED, timeline.tick());
        assertEquals(10, timeline.executionTicksElapsed());

        timeline.tick();
        timeline.tick();
        assertEquals(10, timeline.executionTicksElapsed());
    }

    @Test
    void anInterruptedExecutionGoesToTheShorterInterruptedRecovery() {
        SessionTimeline timeline = timeline();
        timeline.tick();
        timeline.tick();
        timeline.tick();

        assertTrue(timeline.interrupt());

        assertEquals(SkillPhase.INTERRUPTED_RECOVERY, timeline.phase());
        assertEquals(0, timeline.ticksInPhase());
        // The tick in which the interruption happened does not count: one uncounted, then eight.
        assertEquals(Step.CONTINUE, timeline.tick());
        assertEquals(0, timeline.ticksInPhase());
        assertEquals(8, ticksUntil(timeline, Step.FINISHED));
    }

    @Test
    void recoveryCannotBeInterruptedOrShortened() {
        SessionTimeline timeline = timeline();
        ticksUntil(timeline, Step.EXECUTION_ENDED);
        timeline.tick();

        assertFalse(timeline.interrupt());

        assertEquals(SkillPhase.RECOVERY, timeline.phase());
        assertEquals(1, timeline.ticksInPhase());
        assertEquals(11, ticksUntil(timeline, Step.FINISHED));
    }

    @Test
    void anInterruptedRecoveryCannotBeInterruptedAgain() {
        SessionTimeline timeline = timeline();
        timeline.interrupt();
        timeline.tick();
        timeline.tick();

        assertFalse(timeline.interrupt());
        assertEquals(1, timeline.ticksInPhase());
        assertEquals(7, ticksUntil(timeline, Step.FINISHED));
    }

    /** 0.35 s execution, 0.7 s recovery, 0.4 s interrupted recovery: 7, 14 and 8 ticks. */
    private static SessionTimeline stanceTimeline() {
        return SessionTimeline.withPreparation(0.35, 0.7, 0.4);
    }

    /** The timeline's "no length" is sent as it is, and clients compare it with the message's own constant. */
    @Test
    void theLengthOfAPreparationIsTheValueTheMessageNamesForNoLength() {
        assertEquals(com.mahghuuuls.ashenskills.network.PhaseUpdateMessage.NO_FIXED_LENGTH,
                SessionTimeline.NO_FIXED_LENGTH);
    }

    @Test
    void aPreparationNeverEndsByTimeAndHasNoLength() {
        SessionTimeline timeline = stanceTimeline();
        assertEquals(SkillPhase.PREPARATION, timeline.phase());
        assertEquals(SessionTimeline.NO_FIXED_LENGTH, timeline.phaseLengthTicks());

        for (int tick = 1; tick <= 2000; tick++) {
            assertEquals(Step.CONTINUE, timeline.tick());
        }

        assertEquals(SkillPhase.PREPARATION, timeline.phase());
        assertEquals(2000, timeline.ticksInPhase());
        assertEquals(0, timeline.executionTicksElapsed());
    }

    @Test
    void executionBeginsOnlyFromThePreparationAndThenLastsItsOwnLength() {
        SessionTimeline timeline = stanceTimeline();
        for (int tick = 0; tick < 33; tick++) {
            timeline.tick();
        }

        assertTrue(timeline.beginExecution());

        assertEquals(SkillPhase.EXECUTION, timeline.phase());
        assertEquals(0, timeline.ticksInPhase());
        assertEquals(7, timeline.phaseLengthTicks());
        assertFalse(timeline.beginExecution());
        for (int tick = 1; tick < 7; tick++) {
            assertEquals(Step.CONTINUE, timeline.tick());
        }
        assertEquals(Step.EXECUTION_ENDED, timeline.tick());
        assertEquals(7, timeline.executionTicksElapsed());
        assertEquals(14, timeline.phaseLengthTicks());
    }

    @Test
    void aSkillUseThatStartsInExecutionHasNoPreparationToLeave() {
        SessionTimeline instant = new SessionTimeline(0.5, 0.6, 0.4);

        assertFalse(instant.beginExecution());
        assertEquals(SkillPhase.EXECUTION, instant.phase());
    }

    @Test
    void aPreparationIsNotAnExecutionThatCanBeInterrupted() {
        SessionTimeline timeline = stanceTimeline();

        assertFalse(timeline.interrupt());
        assertEquals(SkillPhase.PREPARATION, timeline.phase());
    }
}
