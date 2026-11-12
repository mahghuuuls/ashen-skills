package com.mahghuuuls.ashenskills.client.animation.clip;

/** A clip file that cannot be used. The message says what is wrong and where. */
public final class ClipFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClipFormatException(String message) {
        super(message);
    }
}
