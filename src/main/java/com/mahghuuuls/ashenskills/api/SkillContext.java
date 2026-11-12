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
     * Strikes every valid target inside the shape once, with the skill's damage. The mod applies
     * its rules: the target cap, walls, PvP and team rules, the damage formula and the skill's
     * allowed on-hit effects.
     *
     * @return how many targets took damage
     */
    int hit(HitShape shape);
}
