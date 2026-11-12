package com.mahghuuuls.ashenskills.client.animation.clip;

/** From where a clip is seen. A skill has one set of clips per view. */
public enum View {

    /** The user's own hands. */
    FIRST_PERSON("first"),
    /** The whole body, as other players see it. */
    THIRD_PERSON("third");

    private final String fileName;

    View(String fileName) {
        this.fileName = fileName;
    }

    /** The first part of a clip's file name. */
    public String fileName() {
        return fileName;
    }
}
