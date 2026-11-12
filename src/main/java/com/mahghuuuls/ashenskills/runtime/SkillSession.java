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

    SkillSession(UUID playerId, SkillDefinition skill, int hotbarSlot, Item weaponItem) {
        this.playerId = playerId;
        this.skill = skill;
        this.hotbarSlot = hotbarSlot;
        this.weaponItem = weaponItem;
        this.timeline = new SessionTimeline(
                skill.number(SkillNumber.EXECUTION_LENGTH),
                skill.number(SkillNumber.RECOVERY),
                skill.number(SkillNumber.INTERRUPTED_RECOVERY));
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

    /** Movement speed in the current phase, as a fraction of normal speed. */
    double speedFraction() {
        return skill.number(phase() == SkillPhase.EXECUTION ? SkillNumber.EXECUTION_SPEED : SkillNumber.RECOVERY_SPEED);
    }
}
