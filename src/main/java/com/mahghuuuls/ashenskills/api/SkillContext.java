package com.mahghuuuls.ashenskills.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** One running skill use, as a {@link SkillBehavior} sees it. Valid only during the call. */
public interface SkillContext {

    EntityPlayer user();

    /** The weapon the skill is on. */
    ItemStack weapon();

    SkillDefinition skill();

    /** The direction the user faced when the skill executed. Later turning does not change it. */
    float facingYaw();

    /**
     * The extra share of damage this use earned by charging: 0 for a skill that is not charged, the
     * skill's maximum for a full charge, and between the two for a shorter charge. {@link #hit}
     * already applies it to the damage; it is here for what else a skill scales by its charge.
     */
    double chargeBonus();

    /**
     * Strikes every valid target inside the shape once, with the skill's damage. The mod applies
     * its rules: the target cap, walls, PvP and team rules, the damage formula and the skill's
     * allowed on-hit effects.
     *
     * @return how many targets took damage
     */
    int hit(HitShape shape);
}
