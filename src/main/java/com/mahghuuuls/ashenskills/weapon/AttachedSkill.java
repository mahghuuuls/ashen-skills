package com.mahghuuuls.ashenskills.weapon;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * The skill on a weapon and whether it can be used on that weapon right now. A skill that cannot
 * be used stays attached; only the Ashen Forge removes or replaces it.
 */
public final class AttachedSkill {

    /** Whether the attached skill can be used. */
    public enum Status {
        /** The item carries no skill. */
        NONE,
        /** The skill exists and fits one of the weapon's types. */
        COMPATIBLE,
        /** The skill exists but fits none of the weapon's types. */
        INCOMPATIBLE,
        /** No installed mod provides a skill with this id. */
        UNAVAILABLE
    }

    private final String skillId;
    private final SkillDefinition definition;
    private final Status status;

    private AttachedSkill(String skillId, SkillDefinition definition, Status status) {
        this.skillId = skillId;
        this.definition = definition;
        this.status = status;
    }

    public static AttachedSkill of(ItemStack stack) {
        String id = SkillTag.getSkillId(stack);
        return resolve(id, id == null ? null : lookUp(id), WeaponTypes.effectiveTypes(stack));
    }

    static AttachedSkill resolve(String skillId, SkillDefinition definition, Set<String> typesOfWeapon) {
        if (skillId == null) {
            return new AttachedSkill(null, null, Status.NONE);
        }
        if (definition == null) {
            return new AttachedSkill(skillId, null, Status.UNAVAILABLE);
        }
        Status status = WeaponTypes.shareAType(typesOfWeapon, definition.weaponTypes())
                ? Status.COMPATIBLE
                : Status.INCOMPATIBLE;
        return new AttachedSkill(skillId, definition, status);
    }

    /**
     * Finds the skill for the id text stored on an item. Only the exact registered spelling counts:
     * the game would silently lower-case other spellings, and one skill must not have two ids.
     */
    private static SkillDefinition lookUp(String skillId) {
        ResourceLocation id = new ResourceLocation(skillId);
        return id.toString().equals(skillId) ? SkillRegistry.get(id) : null;
    }

    /** The id as stored on the item, or null when the item carries no skill. */
    public String skillId() {
        return skillId;
    }

    /** The skill, or null when the item carries none or the skill is not available. */
    public SkillDefinition definition() {
        return definition;
    }

    public Status status() {
        return status;
    }

    public boolean isUsable() {
        return status == Status.COMPATIBLE;
    }
}
