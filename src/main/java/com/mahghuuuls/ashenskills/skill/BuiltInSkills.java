package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
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
        SkillDefinition.Builder builder = SkillDefinition
                .builder(new ResourceLocation(Tags.MOD_ID, name), ActivationKind.fromConfigName(entry.activation()))
                .weaponTypes(entry.weaponTypes())
                .allowedStates(entry.allowedStartStates());
        for (SkillNumber key : SkillNumber.values()) {
            Double value = entry.number(key.configName());
            if (value != null) {
                builder.number(key, value);
            }
        }
        for (SkillFlag key : SkillFlag.values()) {
            Boolean value = entry.flag(key.configName());
            if (value != null) {
                builder.flag(key, value);
            }
        }
        return builder.build();
    }
}
