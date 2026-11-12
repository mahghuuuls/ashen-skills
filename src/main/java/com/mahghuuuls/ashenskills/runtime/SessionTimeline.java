package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillPhase;

/**
 * The phases of one skill use and how long each lasts, counted in server ticks. Knows nothing
 * about the game, so its timing can be tested alone.
 */
final class SessionTimeline {

    /** What one tick did. */
    enum Step {
        /** Still in the same phase. */
        CONTINUE,
        /** Execution is over; recovery began this tick. */
        EXECUTION_ENDED,
        /** The last phase is over; the skill use has ended. */
        FINISHED
    }

    static final int TICKS_PER_SECOND = 20;

    private final int executionTicks;
    private final int recoveryTicks;
    private final int interruptedRecoveryTicks;

    private SkillPhase phase = SkillPhase.EXECUTION;
    private int ticksInPhase;
    private int executionTicksElapsed;
    private boolean startsCountingNextTick;

    SessionTimeline(double executionSeconds, double recoverySeconds, double interruptedRecoverySeconds) {
        this.executionTicks = toTicks(executionSeconds);
        this.recoveryTicks = toTicks(recoverySeconds);
        this.interruptedRecoveryTicks = toTicks(interruptedRecoverySeconds);
    }

    /** Every phase lasts at least one tick, so a skill use always ends. */
    static int toTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    SkillPhase phase() {
        return phase;
    }

    int ticksInPhase() {
        return ticksInPhase;
    }

    /** Ticks of execution that have run so far; stays at its last value after execution ends. */
    int executionTicksElapsed() {
        return executionTicksElapsed;
    }

    int phaseLengthTicks() {
        switch (phase) {
            case EXECUTION:
                return executionTicks;
            case RECOVERY:
                return recoveryTicks;
            default:
                return interruptedRecoveryTicks;
        }
    }

    Step tick() {
        if (startsCountingNextTick) {
            startsCountingNextTick = false;
            return Step.CONTINUE;
        }
        ticksInPhase++;
        if (phase == SkillPhase.EXECUTION) {
            executionTicksElapsed++;
        }
        if (ticksInPhase < phaseLengthTicks()) {
            return Step.CONTINUE;
        }
        if (phase == SkillPhase.EXECUTION) {
            enter(SkillPhase.RECOVERY);
            return Step.EXECUTION_ENDED;
        }
        return Step.FINISHED;
    }

    /**
     * Cuts the execution short. Recovery is never interrupted: it cannot be shortened or left. The
     * interrupted recovery starts counting with the server tick after the interruption, so it lasts
     * its full length whether the interruption came before or during this tick's {@link #tick()}.
     *
     * @return true when the execution was interrupted, false when there was none to interrupt
     */
    boolean interrupt() {
        if (phase != SkillPhase.EXECUTION) {
            return false;
        }
        enter(SkillPhase.INTERRUPTED_RECOVERY);
        startsCountingNextTick = true;
        return true;
    }

    private void enter(SkillPhase next) {
        phase = next;
        ticksInPhase = 0;
    }
}
