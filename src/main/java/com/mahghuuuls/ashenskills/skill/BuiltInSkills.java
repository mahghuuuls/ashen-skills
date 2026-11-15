package com.mahghuuuls.ashenskills.skill;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.config.DefaultsData.SkillEntry;
import com.mahghuuuls.ashenskills.skill.builtin.ArcStrike;
import com.mahghuuuls.ashenskills.skill.builtin.PiercingLunge;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

/** The skills this mod ships. Their values come from the bundled defaults file. */
public final class BuiltInSkills {

    /** The skills that are implemented so far, by their name in the defaults file. */
    private static final Map<String, SkillBehavior> IMPLEMENTED = new LinkedHashMap<>();

    static {
        IMPLEMENTED.put("powerful_slash", new ArcStrike());
        IMPLEMENTED.put("unsheathe", new ArcStrike());
        IMPLEMENTED.put("crushing_blow", new ArcStrike());
        IMPLEMENTED.put("piercing_lunge", new PiercingLunge());
    }

    private BuiltInSkills() {
    }

    public static void registerAll(DefaultsData defaults) {
        for (Map.Entry<String, SkillBehavior> skill : IMPLEMENTED.entrySet()) {
            SkillRegistry.register(definitionOf(skill.getKey(), defaults), skill.getValue());
        }
    }

    /** Builds the definition of one shipped skill from the defaults file. */
    public static SkillDefinition definitionOf(String name, DefaultsData defaults) {
        SkillEntry entry = defaults.skill(name);
        SkillDefinition.Builder builder = SkillDefinition
                .builder(new ResourceLocation(Tags.MOD_ID, name), ActivationKind.fromConfigName(entry.activation()))
                .weaponTypes(entry.weaponTypes())
                .allowedStates(entry.allowedStartStates())
                .hitWindows(entry.hitWindowSeconds());
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
