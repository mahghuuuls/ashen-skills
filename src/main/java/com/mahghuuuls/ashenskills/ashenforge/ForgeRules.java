package com.mahghuuuls.ashenskills.ashenforge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** The rules of an Ashen Forge that need no world: which skills it offers, and when it attaches one. */
public final class ForgeRules {

    /** Why the Forge does not attach a skill. */
    public enum Refusal {
        NO_WEAPON, UNKNOWN_SKILL, INCOMPATIBLE, NO_ASH;

        /** The language key of the short message the player gets. */
        public String messageKey() {
            return "ashenskills.forge.refused." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private ForgeRules() {
    }

    /**
     * The skills to offer for the weapon in the input: each skill once, however many Ashes of it are
     * stored, in the order of the storage.
     *
     * @param storedSkillIds the skill id of every stored Ash stack; null for an Ash that names none
     * @param fitsTheWeapon whether the skill with that id exists and fits the weapon
     */
    public static List<String> offered(List<String> storedSkillIds, Predicate<String> fitsTheWeapon) {
        Set<String> distinct = new LinkedHashSet<>();
        for (String id : storedSkillIds) {
            if (id != null && !distinct.contains(id) && fitsTheWeapon.test(id)) {
                distinct.add(id);
            }
        }
        return new ArrayList<>(distinct);
    }

    /**
     * Decides a request to attach a skill, from what is true at the moment the server handles it.
     *
     * @return null when the skill is attached, otherwise the first reason why not
     */
    public static Refusal refusalToAttach(
            boolean weaponInInput, boolean skillExists, boolean skillFitsWeapon, boolean ashOfSkillStored) {
        if (!weaponInInput) {
            return Refusal.NO_WEAPON;
        }
        if (!skillExists) {
            return Refusal.UNKNOWN_SKILL;
        }
        if (!skillFitsWeapon) {
            return Refusal.INCOMPATIBLE;
        }
        return ashOfSkillStored ? null : Refusal.NO_ASH;
    }
}
