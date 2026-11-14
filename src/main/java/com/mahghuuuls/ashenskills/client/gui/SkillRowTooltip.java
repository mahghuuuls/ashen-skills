package com.mahghuuuls.ashenskills.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Which lines the tooltip of a skill in the Forge's list has, as facts without wording: the name,
 * the description when the skill has one, the cooldown, and each cost the skill really has. Each
 * kind of line has its own color.
 */
public final class SkillRowTooltip {

    /** What one line says. */
    public enum Kind {
        NAME('6'),
        DESCRIPTION('7'),
        COOLDOWN('f'),
        STAMINA('e'),
        MANA('9');

        /** The game's formatting code of this kind's color. */
        public final char colorCode;

        Kind(char colorCode) {
            this.colorCode = colorCode;
        }

        /** The language key of a line with a number. The name and the description have their own texts. */
        public String languageKey() {
            return "gui.ashenskills.forge." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** One line; the number is meaningful for the cooldown and the costs only. */
    public static final class Line {
        public final Kind kind;
        public final double number;

        Line(Kind kind, double number) {
            this.kind = kind;
            this.number = number;
        }
    }

    private SkillRowTooltip() {
    }

    /**
     * @param hasDescription whether a description text exists for the skill
     * @param cooldownSeconds the skill's cooldown; always shown
     * @param staminaCost shown when above zero
     * @param manaCost shown when above zero
     */
    public static List<Line> of(boolean hasDescription, double cooldownSeconds, double staminaCost, double manaCost) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Kind.NAME, 0));
        if (hasDescription) {
            lines.add(new Line(Kind.DESCRIPTION, 0));
        }
        lines.add(new Line(Kind.COOLDOWN, cooldownSeconds));
        if (staminaCost > 0) {
            lines.add(new Line(Kind.STAMINA, staminaCost));
        }
        if (manaCost > 0) {
            lines.add(new Line(Kind.MANA, manaCost));
        }
        return lines;
    }

    /** A number as the tooltip shows it: whole numbers without a decimal point, others with one decimal. */
    public static String shown(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
