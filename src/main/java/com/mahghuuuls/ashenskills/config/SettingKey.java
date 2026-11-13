package com.mahghuuuls.ashenskills.config;

/**
 * Every general setting of the mod. Defaults, ranges and the owning side are not written here: they
 * come from the bundled defaults file, under the same name as {@link #jsonName()}.
 */
public enum SettingKey {

    FORGE_ASH_SLOTS("forgeAshSlots", Kind.INTEGER, "Number of Ash slots in each Ashen Forge."),
    FORGE_RECIPE_ENABLED("forgeRecipeEnabled", Kind.BOOLEAN, "Whether the built-in Ashen Forge crafting recipe exists."),
    HYPERBOLIC_SCALE_POINTS("hyperbolicScalePoints", Kind.INTEGER,
            "Points of Haste or Efficiency that halve a cooldown or cost. Factor = scale / (scale + points)."),
    WAIVE_ABSENT_RESOURCE_COSTS("waiveAbsentResourceCosts", Kind.BOOLEAN,
            "True: a stamina or mana cost is free when its resource mod is not installed. False: such a skill cannot be used."),
    HIT_TARGET_CAP_PER_WINDOW("hitTargetCapPerWindow", Kind.INTEGER, "Most targets one hit window can damage, nearest first."),
    HIT_WINDOW_CAP_PER_ACTIVATION("hitWindowCapPerActivation", Kind.INTEGER, "Most hit windows one skill use can have."),
    CREATIVE_BYPASS("creativeBypass", Kind.BOOLEAN, "Whether Creative players skip skill costs and the cooldown."),
    DIAGNOSTIC_LOGGING("diagnosticLogging", Kind.BOOLEAN,
            "Writes skill decisions (activation, phases, hits, payments) to the log. For troubleshooting."),
    FAILURE_MESSAGE_MIN_INTERVAL("failureMessageMinInterval", Kind.DECIMAL,
            "Seconds before the same 'skill not available' message can show again."),
    SKILL_KEY("skillKey", Kind.KEY_BINDING, "Default skill key. Changed in the game's Controls screen, not in this file."),
    CAMERA_EFFECT_INTENSITY("cameraEffectIntensity", Kind.DECIMAL, "Strength of camera shake, tilt and recoil. 0 turns them off."),
    SKILL_HUD("skillHud", Kind.BOOLEAN, "Shows the skill of the held weapon near the hotbar."),
    CHARGE_BAR("chargeBar", Kind.BOOLEAN, "Shows the charge bar near the crosshair while charging a skill."),
    COOLDOWN_NUMBER("cooldownNumber", Kind.BOOLEAN, "Shows the seconds left on the skill cooldown."),
    MAX_IMPACT_PARTICLES_PER_SKILL("maxImpactParticlesPerSkill", Kind.INTEGER,
            "Most impact particles one skill use can spawn on this client."),
    ANIMATION_CHECK_CONTROLS("animationCheckControls", Kind.BOOLEAN,
            "Lets an operator hold a skill animation at a chosen time on this client, write its pose to the log,"
                    + " and set the camera view (/ashenskills pose). For checking animations.");

    /** How a setting is stored and read. */
    public enum Kind {
        BOOLEAN, INTEGER, DECIMAL,
        /** Lives in the game's key bindings, not in the configuration file. */
        KEY_BINDING
    }

    private final String jsonName;
    private final Kind kind;
    private final String comment;

    SettingKey(String jsonName, Kind kind, String comment) {
        this.jsonName = jsonName;
        this.kind = kind;
        this.comment = comment;
    }

    public String jsonName() {
        return jsonName;
    }

    public Kind kind() {
        return kind;
    }

    public String comment() {
        return comment;
    }

    public boolean isInConfigurationFile() {
        return kind != Kind.KEY_BINDING;
    }
}
