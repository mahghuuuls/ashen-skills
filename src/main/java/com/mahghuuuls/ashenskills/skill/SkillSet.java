package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

/** The skills mods add while the game loads, in the order they were added. Closed after loading. */
final class SkillSet {

    private final Map<ResourceLocation, SkillDefinition> skills = new LinkedHashMap<>();
    private final Map<ResourceLocation, SkillBehavior> behaviors = new LinkedHashMap<>();
    private boolean closed;

    /**
     * @throws IllegalStateException when loading is over, the id is taken, or the skill is incomplete
     * @throws IllegalArgumentException when the behavior is null
     */
    void add(SkillDefinition definition, SkillBehavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("Skill " + definition.id() + " needs a behavior");
        }
        if (closed) {
            throw new IllegalStateException(
                    "Skill " + definition.id() + " was registered too late; register skills before the game finishes loading");
        }
        if (!definition.missingValues().isEmpty()) {
            throw new IllegalStateException("Skill " + definition.id() + " lacks values: " + definition.missingValues());
        }
        if (skills.containsKey(definition.id())) {
            throw new IllegalStateException("A skill with id " + definition.id() + " is already registered");
        }
        skills.put(definition.id(), definition);
        behaviors.put(definition.id(), behavior);
    }

    void close() {
        closed = true;
    }

    Map<ResourceLocation, SkillDefinition> skills() {
        return Collections.unmodifiableMap(skills);
    }

    SkillBehavior behaviorOf(ResourceLocation id) {
        return behaviors.get(id);
    }
}
