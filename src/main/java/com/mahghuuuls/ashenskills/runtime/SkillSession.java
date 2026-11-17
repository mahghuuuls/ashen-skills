package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import java.util.UUID;
import net.minecraft.item.Item;

/**
 * The server's record of one player's current skill use, from its start (the preparation, for a
 * skill that has one) until recovery ends.
 */
public final class SkillSession {

    private final UUID playerId;
    private final SkillDefinition skill;
    private final SessionTimeline timeline;
    private final int hotbarSlot;
    private final Item weaponItem;
    private float facingYaw;
    private double chargeBonus;
    private MotionPlan motion;
    private double motionStartX;
    private double motionStartZ;
    private boolean motionRunning;
    private boolean placeExcused;
    private boolean stopOwed;
    private final HitSchedule hitSchedule;
    private final int useId;
    private boolean attackKept;

    /** @param useId tells this use of a skill from every other use while the server runs */
    SkillSession(
            UUID playerId, SkillDefinition skill, int hotbarSlot, Item weaponItem, float facingYaw, int hitWindowCap,
            int useId) {
        this.playerId = playerId;
        this.skill = skill;
        this.hotbarSlot = hotbarSlot;
        this.weaponItem = weaponItem;
        this.facingYaw = facingYaw;
        this.useId = useId;
        this.hitSchedule = new HitSchedule(skill.hitWindowSeconds(), hitWindowCap);
        double execution = skill.number(SkillNumber.EXECUTION_LENGTH);
        double recovery = skill.number(SkillNumber.RECOVERY);
        double interruptedRecovery = skill.number(SkillNumber.INTERRUPTED_RECOVERY);
        this.timeline = skill.activation() == ActivationKind.INSTANT
                ? new SessionTimeline(execution, recovery, interruptedRecovery)
                : SessionTimeline.withPreparation(execution, recovery, interruptedRecovery);
    }

    int useId() {
        return useId;
    }

    public UUID playerId() {
        return playerId;
    }

    public SkillDefinition skill() {
        return skill;
    }

    public SkillPhase phase() {
        return timeline.phase();
    }

    /** Whether an attack press came during the wind-up of this stance and waits for its end. */
    boolean isAttackKept() {
        return attackKept;
    }

    void keepAttack() {
        attackKept = true;
    }

    void dropKeptAttack() {
        attackKept = false;
    }

    public int ticksInPhase() {
        return timeline.ticksInPhase();
    }

    public int phaseLengthTicks() {
        return timeline.phaseLengthTicks();
    }

    SessionTimeline timeline() {
        return timeline;
    }

    int hotbarSlot() {
        return hotbarSlot;
    }

    Item weaponItem() {
        return weaponItem;
    }

    /** The direction the player faced when the skill executed; strikes keep it. */
    float facingYaw() {
        return facingYaw;
    }

    /** Called at the moment the skill executes: the direction is fixed then, not when a stance began. */
    void fixFacing(float yawAtExecution) {
        facingYaw = yawAtExecution;
    }

    /** The extra share of damage this use earned by charging; 0 until a charged skill executes, and for other skills. */
    double chargeBonus() {
        return chargeBonus;
    }

    /** Called at the moment a charged skill executes, with what its charge is worth. */
    void fixChargeBonus(double bonusAtExecution) {
        chargeBonus = bonusAtExecution;
    }

    /** The motion of this skill use while it still moves or has to stop the player; null otherwise. */
    MotionPlan motion() {
        return motionRunning ? motion : null;
    }

    /** Called at the moment a skill with a motion executes. */
    void beginMotion(MotionPlan plan, double startX, double startZ) {
        motion = plan;
        motionRunning = true;
        placeExcused = plan.moves();
        motionStartX = startX;
        motionStartZ = startZ;
    }

    /**
     * Whether the place the player is in (in the air after a ledge, in water) still counts as this
     * skill's own doing. True from the start of a motion that carries the player somewhere, until
     * {@link #endPlaceExcuse}.
     */
    boolean placeExcused() {
        return placeExcused;
    }

    /** The motion is over and the player is back in a state the skill allows: the normal rule holds again. */
    void endPlaceExcuse() {
        placeExcused = false;
    }

    /** Whether the motion is over, and long enough ago that the server sees where it left the player. */
    boolean motionSettled() {
        return motion != null && !motionRunning && motion.settledAt(timeline.executionTicksElapsed());
    }

    /**
     * The execution ended in the tick of a push: the stop waits one tick, so that the push still
     * reaches the player.
     */
    void oweStop() {
        stopOwed = true;
    }

    boolean stopOwed() {
        return stopOwed && motionRunning;
    }

    /** The motion is over: the player was stopped. Where it began stays known. */
    void endMotion() {
        motionRunning = false;
    }

    /**
     * How far the player has come along the motion's direction since it began, also after the
     * motion ended; 0 without a motion, never below 0 and never above the motion's distance.
     */
    double travelled(double x, double z) {
        return motion == null ? 0.0 : motion.travelled(motionStartX, motionStartZ, x, z);
    }

    HitSchedule hitSchedule() {
        return hitSchedule;
    }

    /** Movement speed in the current phase, as a fraction of normal speed. */
    double speedFraction() {
        switch (phase()) {
            case PREPARATION:
                return skill.number(SkillNumber.PREPARATION_SPEED);
            case EXECUTION:
                return skill.number(SkillNumber.EXECUTION_SPEED);
            default:
                return skill.number(SkillNumber.RECOVERY_SPEED);
        }
    }
}
