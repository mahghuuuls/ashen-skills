package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

/**
 * Which skills exist. Skills are added while the game loads and the set is then closed, so a
 * skill id means the same thing for the whole run.
 */
public final class SkillRegistry {

    private static final Map<ResourceLocation, SkillDefinition> SKILLS = new LinkedHashMap<>();
    private static boolean closed;

    private SkillRegistry() {
    }

    /** @throws IllegalStateException when loading is over or the id is already taken */
    public static void register(SkillDefinition definition) {
        if (closed) {
            throw new IllegalStateException(
                    "Skill " + definition.id() + " was registered too late; register skills before the game finishes loading");
        }
        if (SKILLS.containsKey(definition.id())) {
            throw new IllegalStateException("A skill with id " + definition.id() + " is already registered");
        }
        SKILLS.put(definition.id(), definition);
    }

    /** Ends registration. Called once, when the game has finished loading mods. */
    public static void close() {
        closed = true;
    }

    /** The skill with that id, or null when no installed mod registered it. */
    public static SkillDefinition get(ResourceLocation id) {
        return SKILLS.get(id);
    }

    public static Collection<SkillDefinition> all() {
        return Collections.unmodifiableCollection(SKILLS.values());
    }
}
