package com.mahghuuuls.ashenskills.api;

/** How the player starts a skill with the skill key. */
public enum ActivationKind {

    /** Executes when the key is pressed. */
    INSTANT("instant"),
    /** Holding the key enters a stance; pressing attack executes. */
    STANCE("stance"),
    /** Holding the key charges; releasing after the minimum charge executes. */
    CHARGED("charged");

    private final String configName;

    ActivationKind(String configName) {
        this.configName = configName;
    }

    /** The name used in configuration and data files. */
    public String configName() {
        return configName;
    }

    /** @throws IllegalArgumentException when no kind has that configuration name */
    public static ActivationKind fromConfigName(String name) {
        for (ActivationKind kind : values()) {
            if (kind.configName.equals(name)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown activation kind '" + name + "'");
    }
}
