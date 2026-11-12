package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import java.util.UUID;
import net.minecraft.item.Item;

/** The server's record of one player's current skill use, from its start until recovery ends. */
public final class SkillSession {

    private final UUID playerId;
    private final SkillDefinition skill;
    private final SessionTimeline timeline;
    private final int hotbarSlot;
    private final Item weaponItem;
    private final float facingYaw;
    private final HitSchedule hitSchedule;
    private final int useId;

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
        this.timeline = new SessionTimeline(
                skill.number(SkillNumber.EXECUTION_LENGTH),
                skill.number(SkillNumber.RECOVERY),
                skill.number(SkillNumber.INTERRUPTED_RECOVERY));
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

    HitSchedule hitSchedule() {
        return hitSchedule;
    }

    /** Movement speed in the current phase, as a fraction of normal speed. */
    double speedFraction() {
        return skill.number(phase() == SkillPhase.EXECUTION ? SkillNumber.EXECUTION_SPEED : SkillNumber.RECOVERY_SPEED);
    }
}
