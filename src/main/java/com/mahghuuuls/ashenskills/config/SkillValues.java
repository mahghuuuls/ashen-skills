package com.mahghuuuls.ashenskills.config;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every value of a skill that configuration can change, apart from the skill itself (its id, what
 * it does, how it is activated). Used to change a skill's values and to carry them to clients.
 */
public final class SkillValues {

    final Map<SkillNumber, Double> numbers = new EnumMap<>(SkillNumber.class);
    final Map<SkillFlag, Boolean> flags = new EnumMap<>(SkillFlag.class);
    final Set<String> weaponTypes = new LinkedHashSet<>();
    final Set<String> allowedStates = new LinkedHashSet<>();
    final List<Double> hitWindowSeconds = new ArrayList<>();

    SkillValues() {
    }

    public static SkillValues of(SkillDefinition skill) {
        SkillValues values = new SkillValues();
        values.numbers.putAll(skill.numbers());
        values.flags.putAll(skill.flags());
        values.weaponTypes.addAll(skill.weaponTypes());
        values.allowedStates.addAll(skill.allowedStates());
        values.hitWindowSeconds.addAll(skill.hitWindowSeconds());
        return values;
    }

    /**
     * These values laid over others: what is stated here wins, and what is not stated here (a
     * number or rule that is missing, a list that is empty) is taken from the others.
     */
    SkillValues over(SkillValues others) {
        SkillValues merged = new SkillValues();
        merged.numbers.putAll(others.numbers);
        merged.numbers.putAll(numbers);
        merged.flags.putAll(others.flags);
        merged.flags.putAll(flags);
        merged.weaponTypes.addAll(weaponTypes.isEmpty() ? others.weaponTypes : weaponTypes);
        merged.allowedStates.addAll(allowedStates.isEmpty() ? others.allowedStates : allowedStates);
        merged.hitWindowSeconds.addAll(hitWindowSeconds.isEmpty() ? others.hitWindowSeconds : hitWindowSeconds);
        return merged;
    }

    /** The skill with these values in place of its own. */
    public SkillDefinition appliedTo(SkillDefinition skill) {
        SkillDefinition.Builder builder = SkillDefinition.builder(skill.id(), skill.activation())
                .weaponTypes(weaponTypes)
                .allowedStates(allowedStates)
                .hitWindows(hitWindowSeconds);
        for (Map.Entry<SkillNumber, Double> number : numbers.entrySet()) {
            builder.number(number.getKey(), number.getValue());
        }
        for (Map.Entry<SkillFlag, Boolean> flag : flags.entrySet()) {
            builder.flag(flag.getKey(), flag.getValue());
        }
        return builder.build();
    }
}
