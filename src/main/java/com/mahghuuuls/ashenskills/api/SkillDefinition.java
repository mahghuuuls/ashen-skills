package com.mahghuuuls.ashenskills.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

/**
 * What a skill is, as data. Immutable. The id is permanent: it is what a weapon stores, so it must
 * never change once players can attach the skill.
 */
public final class SkillDefinition {

    private final ResourceLocation id;
    private final ActivationKind activation;
    private final Set<String> weaponTypes;

    /**
     * @param id namespaced id, for example {@code mymod:whirlwind}
     * @param weaponTypes the weapon types this skill fits; a weapon needs at least one of them
     */
    public SkillDefinition(ResourceLocation id, ActivationKind activation, Set<String> weaponTypes) {
        if (id == null || activation == null || weaponTypes == null) {
            throw new IllegalArgumentException("A skill needs an id, an activation kind and weapon types");
        }
        this.id = id;
        this.activation = activation;
        this.weaponTypes = Collections.unmodifiableSet(new LinkedHashSet<>(weaponTypes));
    }

    public ResourceLocation id() {
        return id;
    }

    public ActivationKind activation() {
        return activation;
    }

    public Set<String> weaponTypes() {
        return weaponTypes;
    }
}
