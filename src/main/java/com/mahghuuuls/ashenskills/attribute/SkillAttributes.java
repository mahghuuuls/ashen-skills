package com.mahghuuuls.ashenskills.attribute;

import com.mahghuuuls.ashenskills.Tags;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The four attributes every player has. Each is 0 unless something adds points, and other mods
 * change them the way they change any attribute: by name, with attribute modifiers on items,
 * potions or the player. What the points do is in {@link AttributeFormulas} and in the damage
 * formula.
 */
public final class SkillAttributes {

    /**
     * Far beyond any sensible bonus; only keeps a broken modifier from producing absurd values.
     * A limit of the attribute type, not a tuning value, so it is not in the defaults file.
     */
    private static final double MOST_POINTS = 1024.0;
    /** At -100 points of Skill Damage a hit deals nothing; less has no meaning. */
    private static final double LEAST_DAMAGE_POINTS = -100.0;

    /** Percent points added to the damage of every skill hit. */
    public static final IAttribute SKILL_DAMAGE = attribute("skillDamage", LEAST_DAMAGE_POINTS);
    /** Points that shorten the exclusive cooldown. */
    public static final IAttribute SKILL_HASTE = attribute("skillHaste", -MOST_POINTS);
    /** Points that lower the stamina cost of skills. */
    public static final IAttribute SKILL_STAMINA_EFFICIENCY = attribute("skillStaminaEfficiency", -MOST_POINTS);
    /** Points that lower the mana cost of skills. */
    public static final IAttribute SKILL_MANA_EFFICIENCY = attribute("skillManaEfficiency", -MOST_POINTS);

    private static final IAttribute[] ALL = {
        SKILL_DAMAGE, SKILL_HASTE, SKILL_STAMINA_EFFICIENCY, SKILL_MANA_EFFICIENCY
    };

    private SkillAttributes() {
    }

    /** Starts giving the attributes to every player. Called once while the game loads. */
    public static void install() {
        MinecraftForge.EVENT_BUS.register(new SkillAttributes());
    }

    /** The player's current points of one of the four attributes. */
    public static double points(EntityPlayer player, IAttribute attribute) {
        return player.getEntityAttribute(attribute).getAttributeValue();
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntity();
        for (IAttribute attribute : ALL) {
            player.getAttributeMap().registerAttribute(attribute);
        }
    }

    private static IAttribute attribute(String name, double least) {
        // Watched, so the values reach the clients (the own player's, and those of players in view).
        return new RangedAttribute(null, Tags.MOD_ID + "." + name, 0.0, least, MOST_POINTS).setShouldWatch(true);
    }
}
