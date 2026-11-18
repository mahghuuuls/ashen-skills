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
     * How far this use's motion has carried the user so far, in blocks along the fixed direction;
     * 0 for a skill that does not move its user, never below 0 and never above the motion's
     * distance. It is what the server knows:
     * a player's own client moves the player, so the value runs a little behind what the player
     * sees.
     */
    double travelled();

    /**
     * Strikes every valid target inside the shape once, with the skill's damage. The mod applies
     * its rules: the target cap, walls, PvP and team rules, the damage formula and the skill's
     * allowed on-hit effects. A skill may strike more than once in one hit window, with different
     * shapes: the target cap then counts over the whole window, and a creature that took damage in
     * this window is passed over and takes no place under the cap.
     *
     * @return how many targets took damage
     * @throws IllegalStateException when called outside a hit window, or after the call this
     *     context was given to has returned
     */
    int hit(HitShape shape);
}
