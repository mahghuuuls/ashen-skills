package com.mahghuuuls.ashenskills.client.hud;

import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which lines the mod adds to an item's tooltip, as facts without wording. An item with a weapon
 * type or a skill always gets its lines, and an item with a skill but no type says that it has no
 * type, because that is why its skill does not fit. An item with neither gets no line, except where
 * a player asks what the mod makes of an item: at an Ashen Forge and in advanced tooltips it is
 * marked as having no weapon type.
 */
public final class TooltipLines {

    /** What one added line says. */
    public enum Kind {
        /** The item's weapon types; the detail is the list. */
        TYPES,
        /** The item has no weapon type. */
        UNCLASSIFIED,
        /** The attached skill, usable; the detail is its id. */
        SKILL,
        /** The attached skill does not fit this item's types; the detail is its id. */
        SKILL_INCOMPATIBLE,
        /** No installed mod provides the attached skill; the detail is its id. */
        SKILL_UNAVAILABLE;

        /** The language key of this line's text. */
        public String languageKey() {
            return "tooltip.ashenskills." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** One added line. */
    public static final class Line {
        public final Kind kind;
        public final String detail;

        Line(Kind kind, String detail) {
            this.kind = kind;
            this.detail = detail;
        }
    }

    private TooltipLines() {
    }

    /**
     * @param types the item's effective weapon types; shown in alphabetical order
     * @param skill how the attached skill stands
     * @param skillId the id text of the attached skill, or null
     * @param askedInDetail whether the tooltip is shown at an Ashen Forge or in advanced mode
     */
    public static List<Line> of(Set<String> types, AttachedSkill.Status skill, String skillId, boolean askedInDetail) {
        List<Line> lines = new ArrayList<>();
        if (!types.isEmpty()) {
            lines.add(new Line(Kind.TYPES, String.join(", ", new TreeSet<>(types))));
        } else if (skill != AttachedSkill.Status.NONE || askedInDetail) {
            lines.add(new Line(Kind.UNCLASSIFIED, ""));
        }
        switch (skill) {
            case COMPATIBLE:
                lines.add(new Line(Kind.SKILL, skillId));
                break;
            case INCOMPATIBLE:
                lines.add(new Line(Kind.SKILL_INCOMPATIBLE, skillId));
                break;
            case UNAVAILABLE:
                lines.add(new Line(Kind.SKILL_UNAVAILABLE, skillId));
                break;
            default:
                break;
        }
        return lines;
    }
}
