package com.mahghuuuls.ashenskills.client.animation.clip;

/** How a value moves from the key before it to a key: the shape of the motion between two keys. */
public enum Easing {

    /** Even speed. */
    LINEAR("linear") {
        @Override
        public double apply(double progress) {
            return progress;
        }
    },
    /** Starts slow, ends fast: a wind-up or the start of a strike. */
    EASE_IN("ease_in") {
        @Override
        public double apply(double progress) {
            return progress * progress * progress;
        }
    },
    /** Starts fast, ends slow: a strike that lands and settles. */
    EASE_OUT("ease_out") {
        @Override
        public double apply(double progress) {
            double rest = 1 - progress;
            return 1 - rest * rest * rest;
        }
    },
    /** Slow at both ends. */
    EASE_IN_OUT("ease_in_out") {
        @Override
        public double apply(double progress) {
            return progress < 0.5
                    ? 4 * progress * progress * progress
                    : 1 - Math.pow(-2 * progress + 2, 3) / 2;
        }
    };

    private final String fileName;

    Easing(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    /**
     * @param progress how much of the time between the two keys has passed, 0 to 1
     * @return how much of the way between the two values has been covered, 0 to 1
     */
    public abstract double apply(double progress);

    /** @return the easing with that clip-file name, or null */
    public static Easing byFileName(String name) {
        for (Easing easing : values()) {
            if (easing.fileName.equals(name)) {
                return easing;
            }
        }
        return null;
    }
}
