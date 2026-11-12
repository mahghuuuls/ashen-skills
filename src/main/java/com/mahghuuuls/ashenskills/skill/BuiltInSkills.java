package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.config.DefaultsData.SkillEntry;
import net.minecraft.util.ResourceLocation;

/** The skills this mod ships. Their values come from the bundled defaults file. */
public final class BuiltInSkills {

    /** Names in the defaults file of the skills that are implemented so far. */
    private static final String[] IMPLEMENTED = {"powerful_slash"};

    private BuiltInSkills() {
    }

    public static void registerAll(DefaultsData defaults) {
        for (String name : IMPLEMENTED) {
            SkillRegistry.register(definitionOf(name, defaults));
        }
    }

    /** Builds the definition of one shipped skill from the defaults file. */
    public static SkillDefinition definitionOf(String name, DefaultsData defaults) {
        SkillEntry entry = defaults.skill(name);
        return new SkillDefinition(
                new ResourceLocation(Tags.MOD_ID, name),
                ActivationKind.fromConfigName(entry.activation()),
                entry.weaponTypes());
    }
}
