package com.mahghuuuls.ashenskills.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.client.gui.SkillRowTooltip.Kind;
import com.mahghuuuls.ashenskills.client.gui.SkillRowTooltip.Line;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SkillRowTooltipTest {

    private static List<String> shown(List<Line> lines) {
        List<String> shown = new ArrayList<>();
        for (Line line : lines) {
            boolean numbered = line.kind != Kind.NAME && line.kind != Kind.DESCRIPTION;
            shown.add(line.kind + (numbered ? " " + SkillRowTooltip.shown(line.number) : ""));
        }
        return shown;
    }

    @Test
    void aSkillWithEveryCostShowsNameDescriptionCooldownAndEachCost() {
        assertEquals(
                Arrays.asList("NAME", "DESCRIPTION", "COOLDOWN 8", "STAMINA 15", "MANA 7.5"),
                shown(SkillRowTooltip.of(true, 8, 15, 7.5)));
    }

    @Test
    void aCostOfZeroIsNotShown() {
        assertEquals(Arrays.asList("NAME", "DESCRIPTION", "COOLDOWN 8", "STAMINA 15"), shown(SkillRowTooltip.of(true, 8, 15, 0)));
        assertEquals(Arrays.asList("NAME", "DESCRIPTION", "COOLDOWN 8", "MANA 10"), shown(SkillRowTooltip.of(true, 8, 0, 10)));
    }

    @Test
    void aSkillWithoutADescriptionStillShowsItsNameAndCooldown() {
        assertEquals(Arrays.asList("NAME", "COOLDOWN 0.5"), shown(SkillRowTooltip.of(false, 0.5, 0, 0)));
    }

    @Test
    void everyKindOfLineHasItsOwnColor() {
        Set<Character> colors = new HashSet<>();
        for (Kind kind : Kind.values()) {
            colors.add(kind.colorCode);
        }

        assertEquals(Kind.values().length, colors.size());
    }

    @Test
    void theCooldownIsWhiteTheStaminaCostYellowAndTheManaCostBlue() {
        assertEquals('f', Kind.COOLDOWN.colorCode);
        assertEquals('e', Kind.STAMINA.colorCode);
        assertEquals('9', Kind.MANA.colorCode);
    }

    @Test
    void theLanguageFileHasATextForEveryNumberedLine() throws IOException {
        Path file = Paths.get("src", "main", "resources", "assets", "ashenskills", "lang", "en_us.lang");
        assertTrue(Files.isRegularFile(file), "run from the project folder: missing " + file.toAbsolutePath());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (Kind kind : Arrays.asList(Kind.COOLDOWN, Kind.STAMINA, Kind.MANA)) {
            String key = kind.languageKey() + "=";
            boolean found = false;
            for (String line : lines) {
                found |= line.startsWith(key) && line.contains("%s");
            }
            assertTrue(found, "no text with a number for " + key);
        }
    }
}
