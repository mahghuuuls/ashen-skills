package com.mahghuuuls.ashenskills.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class SkillSetTest {

    private static final DefaultsData DEFAULTS = DefaultsData.loadBundled();
    private static final SkillBehavior NOTHING = (context, window) -> { };

    @Test
    void aSkillIsKeptWithItsBehaviorInTheOrderItWasAdded() {
        SkillSet set = new SkillSet();
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DEFAULTS);
        SkillDefinition lunge = BuiltInSkills.definitionOf("piercing_lunge", DEFAULTS);

        set.add(slash, NOTHING);
        set.add(lunge, NOTHING);

        assertEquals("[ashenskills:powerful_slash, ashenskills:piercing_lunge]", set.skills().keySet().toString());
        assertSame(NOTHING, set.behaviorOf(slash.id()));
        assertNull(set.behaviorOf(new ResourceLocation("other", "skill")));
    }

    @Test
    void anIdCanBeTakenOnce() {
        SkillSet set = new SkillSet();
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DEFAULTS);
        set.add(slash, NOTHING);

        IllegalStateException taken = assertThrows(IllegalStateException.class, () -> set.add(slash, NOTHING));

        assertTrue(taken.getMessage().contains("already registered"), taken.getMessage());
        assertEquals(1, set.skills().size());
    }

    @Test
    void afterTheGameHasLoadedNoSkillCanBeAdded() {
        SkillSet set = new SkillSet();
        set.add(BuiltInSkills.definitionOf("powerful_slash", DEFAULTS), NOTHING);
        set.close();

        IllegalStateException late = assertThrows(IllegalStateException.class,
                () -> set.add(BuiltInSkills.definitionOf("piercing_lunge", DEFAULTS), NOTHING));

        assertTrue(late.getMessage().contains("ashenskills:piercing_lunge"), late.getMessage());
        assertTrue(late.getMessage().contains("before the game finishes loading"), late.getMessage());
        assertEquals(1, set.skills().size());
    }

    @Test
    void aSkillWithoutABehaviorIsRefused() {
        SkillSet set = new SkillSet();

        assertThrows(IllegalArgumentException.class,
                () -> set.add(BuiltInSkills.definitionOf("powerful_slash", DEFAULTS), null));
        assertTrue(set.skills().isEmpty());
    }
}
