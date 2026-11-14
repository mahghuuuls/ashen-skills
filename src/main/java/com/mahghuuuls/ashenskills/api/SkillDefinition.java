package com.mahghuuuls.ashenskills.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.ResourceLocation;

/**
 * What a skill is, as data. Immutable. The id is permanent: it is what a weapon stores, so it must
 * never change once players can attach the skill.
 */
public final class SkillDefinition {

    /** The player stands on the ground. */
    public static final String STATE_GROUNDED = "grounded";
    /** The player is in the air. */
    public static final String STATE_AIRBORNE = "airborne";
    /** The player is in water. */
    public static final String STATE_SWIMMING = "swimming";

    private static final SkillNumber[] PREPARATION_NUMBERS = {
        SkillNumber.PREPARATION_SPEED, SkillNumber.PREPARATION_STAMINA_REGEN};
    private static final SkillFlag[] PREPARATION_FLAGS = {
        SkillFlag.PREPARATION_CANCELED_BY_DAMAGE, SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK};

    private final ResourceLocation id;
    private final ActivationKind activation;
    private final Set<String> weaponTypes;
    private final Set<String> allowedStates;
    private final List<Double> hitWindowSeconds;
    private final Map<SkillNumber, Double> numbers;
    private final Map<SkillFlag, Boolean> flags;

    private SkillDefinition(Builder builder) {
        this.id = builder.id;
        this.activation = builder.activation;
        this.weaponTypes = Collections.unmodifiableSet(new LinkedHashSet<>(builder.weaponTypes));
        this.allowedStates = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedStates));
        this.hitWindowSeconds = Collections.unmodifiableList(new ArrayList<>(builder.hitWindowSeconds));
        this.numbers = new EnumMap<>(builder.numbers);
        this.flags = new EnumMap<>(builder.flags);
    }

    /** @param id namespaced id, for example {@code mymod:whirlwind} */
    public static Builder builder(ResourceLocation id, ActivationKind activation) {
        if (id == null || activation == null) {
            throw new IllegalArgumentException("A skill needs an id and an activation kind");
        }
        return new Builder(id, activation);
    }

    public ResourceLocation id() {
        return id;
    }

    public ActivationKind activation() {
        return activation;
    }

    /** The weapon types this skill fits; a weapon needs at least one of them. */
    public Set<String> weaponTypes() {
        return weaponTypes;
    }

    /** The player states (see the STATE constants) in which the skill may start and continue. */
    public Set<String> allowedStates() {
        return allowedStates;
    }

    /** When each hit window opens, in seconds from the start of execution. May be empty. */
    public List<Double> hitWindowSeconds() {
        return hitWindowSeconds;
    }

    /** @throws IllegalStateException when the skill was built without this value */
    public double number(SkillNumber key) {
        Double value = numbers.get(key);
        if (value == null) {
            throw new IllegalStateException("Skill " + id + " has no value for " + key.configName());
        }
        return value;
    }

    /** @throws IllegalStateException when the skill was built without this rule */
    public boolean flag(SkillFlag key) {
        Boolean value = flags.get(key);
        if (value == null) {
            throw new IllegalStateException("Skill " + id + " has no value for " + key.configName());
        }
        return value;
    }

    /** The numbers this skill has, by key. Not every skill has every optional number. */
    public Map<SkillNumber, Double> numbers() {
        return Collections.unmodifiableMap(numbers);
    }

    /** The yes-or-no rules this skill has, by key. */
    public Map<SkillFlag, Boolean> flags() {
        return Collections.unmodifiableMap(flags);
    }

    /**
     * Configuration names of the required numbers and rules this skill still lacks. Empty when
     * complete. A skill that is not instant also needs the numbers and rules of its preparation.
     */
    public List<String> missingValues() {
        List<String> missing = new ArrayList<>();
        for (SkillNumber key : SkillNumber.values()) {
            if (key.isRequiredForEverySkill() && !numbers.containsKey(key)) {
                missing.add(key.configName());
            }
        }
        for (SkillFlag key : SkillFlag.values()) {
            if (key.isRequiredForEverySkill() && !flags.containsKey(key)) {
                missing.add(key.configName());
            }
        }
        if (activation != ActivationKind.INSTANT) {
            // A skill with a preparation needs the values that rule it.
            for (SkillNumber key : PREPARATION_NUMBERS) {
                if (!numbers.containsKey(key)) {
                    missing.add(key.configName());
                }
            }
            for (SkillFlag key : PREPARATION_FLAGS) {
                if (!flags.containsKey(key)) {
                    missing.add(key.configName());
                }
            }
        }
        return missing;
    }

    /**
     * Collects the parts of a skill. Every {@link SkillFlag} and every {@link SkillNumber} that is
     * required for every skill must be set, and for a skill that is not instant the preparation
     * values too; {@link SkillDefinition#missingValues()} names what is still missing.
     */
    public static final class Builder {

        private final ResourceLocation id;
        private final ActivationKind activation;
        private final Set<String> weaponTypes = new LinkedHashSet<>();
        private final Set<String> allowedStates = new LinkedHashSet<>();
        private final List<Double> hitWindowSeconds = new ArrayList<>();
        private final Map<SkillNumber, Double> numbers = new EnumMap<>(SkillNumber.class);
        private final Map<SkillFlag, Boolean> flags = new EnumMap<>(SkillFlag.class);

        private Builder(ResourceLocation id, ActivationKind activation) {
            this.id = id;
            this.activation = activation;
        }

        public Builder weaponTypes(Set<String> types) {
            weaponTypes.addAll(types);
            return this;
        }

        public Builder allowedStates(Set<String> states) {
            allowedStates.addAll(states);
            return this;
        }

        /** When each hit window opens, in seconds from the start of execution. */
        public Builder hitWindows(List<Double> seconds) {
            hitWindowSeconds.addAll(seconds);
            return this;
        }

        public Builder number(SkillNumber key, double value) {
            numbers.put(key, value);
            return this;
        }

        public Builder flag(SkillFlag key, boolean value) {
            flags.put(key, value);
            return this;
        }

        public SkillDefinition build() {
            return new SkillDefinition(this);
        }
    }
}
