package com.mahghuuuls.ashenskills.attribute;

import java.util.UUID;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/**
 * What food can give: points of one of the four attributes. A food mod sums the points of a
 * player's active foods and hands the sum over; the sum sits on the attribute as one modifier that
 * is not saved with the player, so it never outlasts the food mod: that mod gives the sum again at
 * every login.
 */
public enum FoodProperty {

    SKILL_DAMAGE("skill_damage", SkillAttributes.SKILL_DAMAGE, "red", "f0a6b1c2-5d3e-4a71-9c08-2b7d4e6a9101"),
    SKILL_HASTE("skill_haste", SkillAttributes.SKILL_HASTE, "aqua", "f0a6b1c2-5d3e-4a71-9c08-2b7d4e6a9102"),
    SKILL_STAMINA_EFFICIENCY("skill_stamina_efficiency", SkillAttributes.SKILL_STAMINA_EFFICIENCY, "green",
            "f0a6b1c2-5d3e-4a71-9c08-2b7d4e6a9103"),
    SKILL_MANA_EFFICIENCY("skill_mana_efficiency", SkillAttributes.SKILL_MANA_EFFICIENCY, "blue",
            "f0a6b1c2-5d3e-4a71-9c08-2b7d4e6a9104");

    private static final String NAMESPACE = "ashenskills";
    private static final String MODIFIER_NAME = "Ashen Skills food";
    private static final int ADD = 0;

    private final String path;
    private final IAttribute attribute;
    private final String color;
    private final UUID modifierId;

    FoodProperty(String path, IAttribute attribute, String color, String modifierId) {
        this.path = path;
        this.attribute = attribute;
        this.color = color;
        this.modifierId = UUID.fromString(modifierId);
    }

    /** The id a food definition names, for example {@code ashenskills:skill_damage}. */
    public String id() {
        return NAMESPACE + ":" + path;
    }

    public IAttribute attribute() {
        return attribute;
    }

    /** The language key of the property's name: the attribute's own name. */
    public String nameKey() {
        return "attribute.name." + attribute.getName();
    }

    /** The name of the game's text color the food mod shows the property in. */
    public String color() {
        return color;
    }

    /** The fewest points a food definition may give. */
    public double leastPoints() {
        return SkillAttributes.leastPoints(attribute);
    }

    /** The most points a food definition may give. */
    public double mostPoints() {
        return SkillAttributes.mostPoints();
    }

    /**
     * Makes the food points on this attribute equal the sum. The same sum again changes nothing;
     * 0 takes the food points away.
     *
     * @param instance the player's value of this property's attribute
     */
    public void setPoints(IAttributeInstance instance, double sum) {
        AttributeModifier now = instance.getModifier(modifierId);
        if (now != null) {
            if (now.getAmount() == sum) {
                return;
            }
            instance.removeModifier(modifierId);
        }
        if (sum != 0) {
            instance.applyModifier(new AttributeModifier(modifierId, MODIFIER_NAME, sum, ADD).setSaved(false));
        }
    }
}
