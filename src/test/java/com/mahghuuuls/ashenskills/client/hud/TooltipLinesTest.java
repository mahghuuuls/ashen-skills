package com.mahghuuuls.ashenskills.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.client.hud.TooltipLines.Kind;
import com.mahghuuuls.ashenskills.client.hud.TooltipLines.Line;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill.Status;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TooltipLinesTest {

    private static final Set<String> NO_TYPES = Collections.emptySet();
    private static final String UNSHEATHE = "ashenskills:unsheathe";

    private static List<String> shown(List<Line> lines) {
        List<String> shown = new ArrayList<>();
        for (Line line : lines) {
            shown.add(line.kind + (line.detail.isEmpty() ? "" : " " + line.detail));
        }
        return shown;
    }

    private static Set<String> types(String... names) {
        return new LinkedHashSet<>(Arrays.asList(names));
    }

    @Test
    void aKatanaWithUnsheatheShowsItsTypesAndItsSkill() {
        // The types are shown in alphabetical order, whatever order the rules give them in.
        assertEquals(Arrays.asList("TYPES katana, sword", "SKILL " + UNSHEATHE),
                shown(TooltipLines.of(types("sword", "katana"), Status.COMPATIBLE, UNSHEATHE, false)));
    }

    @Test
    void aStickGetsNoLineInANormalTooltip() {
        assertEquals(Collections.emptyList(), shown(TooltipLines.of(NO_TYPES, Status.NONE, null, false)));
    }

    @Test
    void aStickIsMarkedAsUnclassifiedAtAForgeOrInAdvancedTooltips() {
        assertEquals(Collections.singletonList("UNCLASSIFIED"), shown(TooltipLines.of(NO_TYPES, Status.NONE, null, true)));
    }

    @Test
    void aWeaponWithoutASkillShowsOnlyItsTypes() {
        assertEquals(Collections.singletonList("TYPES sword"), shown(TooltipLines.of(types("sword"), Status.NONE, null, false)));
        assertEquals(Collections.singletonList("TYPES sword"), shown(TooltipLines.of(types("sword"), Status.NONE, null, true)));
    }

    @Test
    void aSkillThatDoesNotFitIsMarkedAndTheItemStillShowsWhatItIs() {
        assertEquals(Arrays.asList("TYPES axe", "SKILL_INCOMPATIBLE " + UNSHEATHE),
                shown(TooltipLines.of(types("axe"), Status.INCOMPATIBLE, UNSHEATHE, false)));
        // An item with a skill and no type says so in a normal tooltip too: that is why the skill does not fit.
        assertEquals(Arrays.asList("UNCLASSIFIED", "SKILL_INCOMPATIBLE " + UNSHEATHE),
                shown(TooltipLines.of(NO_TYPES, Status.INCOMPATIBLE, UNSHEATHE, false)));
    }

    @Test
    void aSkillThatNoModProvidesIsShownByItsId() {
        assertEquals(Arrays.asList("TYPES sword", "SKILL_UNAVAILABLE gone_addon:lost_skill"),
                shown(TooltipLines.of(types("sword"), Status.UNAVAILABLE, "gone_addon:lost_skill", false)));
    }

    @Test
    void everyKindOfLineHasATextInTheLanguageFile() throws IOException {
        Path file = Paths.get("src", "main", "resources", "assets", "ashenskills", "lang", "en_us.lang");
        assertTrue(Files.isRegularFile(file), "run from the project folder: missing " + file.toAbsolutePath());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (Kind kind : Kind.values()) {
            String key = kind.languageKey() + "=";
            boolean found = false;
            for (String line : lines) {
                found |= line.startsWith(key) && line.length() > key.length();
            }
            assertTrue(found, "no text for " + key);
        }
    }
}
