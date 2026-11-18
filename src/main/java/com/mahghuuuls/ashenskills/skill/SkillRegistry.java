package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

/**
 * Which skills exist, and with which values. Skills are added while the game loads and the set is
 * then closed, so a skill id means the same thing for the whole run. Configuration can change a
 * skill's values but never which skills there are.
 */
public final class SkillRegistry {

    private static final SkillSet SET = new SkillSet();
    /** The skills with this installation's configured values; null until configuration was read. */
    private static Map<ResourceLocation, SkillDefinition> configured;
    /** On a client playing on a server: the skills with that server's values. Otherwise null. */
    private static volatile Map<ResourceLocation, SkillDefinition> fromServer;

    private SkillRegistry() {
    }

    /** @throws IllegalStateException when loading is over, the id is taken, or the skill is incomplete */
    public static void register(SkillDefinition definition, SkillBehavior behavior) {
        SET.add(definition, behavior);
    }

    /** Ends registration. Called once, when the game has finished loading mods. */
    public static void close() {
        SET.close();
    }

    /** The skills exactly as the mods registered them, by id, before any configuration. */
    public static Map<ResourceLocation, SkillDefinition> registered() {
        return SET.skills();
    }

    /**
     * Sets the values this installation's configuration gives the skills. Called once, when the
     * game has loaded.
     *
     * @param withConfiguredValues every registered skill, by id
     */
    public static void useConfigured(Map<ResourceLocation, SkillDefinition> withConfiguredValues) {
        configured = new LinkedHashMap<>(withConfiguredValues);
    }

    /** On a client: the values of the server it plays on take the place of its own. */
    public static void useServerValues(Map<ResourceLocation, SkillDefinition> withServerValues) {
        fromServer = new LinkedHashMap<>(withServerValues);
    }

    /** On a client that left a server: back to its own values. */
    public static void restoreOwnValues() {
        fromServer = null;
    }

    /** The skill with that id and its values in force, or null when no installed mod registered it. */
    public static SkillDefinition get(ResourceLocation id) {
        return inForce().get(id);
    }

    private static Map<ResourceLocation, SkillDefinition> inForce() {
        Map<ResourceLocation, SkillDefinition> server = fromServer;
        if (server != null) {
            return server;
        }
        return configured != null ? configured : SET.skills();
    }

    /** What the skill with that id does, or null when no installed mod registered it. */
    public static SkillBehavior behaviorOf(ResourceLocation id) {
        return SET.behaviorOf(id);
    }

    public static Collection<SkillDefinition> all() {
        return Collections.unmodifiableCollection(inForce().values());
    }
}
