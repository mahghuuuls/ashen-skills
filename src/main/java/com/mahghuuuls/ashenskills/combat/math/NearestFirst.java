package com.mahghuuuls.ashenskills.combat.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Keeps the nearest candidates when a strike could reach more targets than its cap allows. */
public final class NearestFirst {

    private NearestFirst() {
    }

    /** One candidate with its distance (any consistent measure, such as distance squared). */
    public static final class Ranked<T> {

        private final T candidate;
        private final double distance;

        public Ranked(T candidate, double distance) {
            this.candidate = candidate;
            this.distance = distance;
        }

        public T candidate() {
            return candidate;
        }

        double distance() {
            return distance;
        }
    }

    /**
     * @param cap the most candidates to keep
     * @return the nearest candidates, nearest first; candidates at equal distance keep their order
     */
    public static <T> List<T> pick(List<Ranked<T>> candidates, int cap) {
        List<Ranked<T>> sorted = new ArrayList<>(candidates);
        Collections.sort(sorted, new Comparator<Ranked<T>>() {
            @Override
            public int compare(Ranked<T> first, Ranked<T> second) {
                return Double.compare(first.distance(), second.distance());
            }
        });
        List<T> picked = new ArrayList<>();
        for (int i = 0; i < sorted.size() && i < cap; i++) {
            picked.add(sorted.get(i).candidate());
        }
        return picked;
    }
}
