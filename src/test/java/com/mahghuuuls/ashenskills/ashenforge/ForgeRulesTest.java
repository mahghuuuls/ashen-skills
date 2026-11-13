package com.mahghuuuls.ashenskills.ashenforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ForgeRulesTest {

    private static final String SLASH = "ashenskills:powerful_slash";
    private static final String LUNGE = "ashenskills:piercing_lunge";
    private static final String BLOW = "ashenskills:crushing_blow";

    @Test
    void threeAshesOfOneSkillOfferThatSkillOnce() {
        List<String> stored = Arrays.asList(SLASH, SLASH, SLASH);

        assertEquals(Collections.singletonList(SLASH), ForgeRules.offered(stored, id -> true));
    }

    @Test
    void onlySkillsThatFitTheWeaponAreOfferedInStorageOrder() {
        List<String> stored = Arrays.asList(LUNGE, SLASH, BLOW, LUNGE, SLASH);
        Set<String> fitting = new HashSet<>(Arrays.asList(SLASH, LUNGE));

        assertEquals(Arrays.asList(LUNGE, SLASH), ForgeRules.offered(stored, fitting::contains));
    }

    @Test
    void anAshThatNamesNoSkillOffersNothing() {
        assertEquals(Collections.singletonList(SLASH), ForgeRules.offered(Arrays.asList(null, SLASH, null), id -> true));
    }

    @Test
    void aSkillIsAttachedOnlyWhenEverythingHoldsAndTheFirstMissingThingIsTheReason() {
        assertNull(ForgeRules.refusalToAttach(true, true, true, true));
        assertEquals(ForgeRules.Refusal.NO_WEAPON, ForgeRules.refusalToAttach(false, true, true, true));
        assertEquals(ForgeRules.Refusal.UNKNOWN_SKILL, ForgeRules.refusalToAttach(true, false, false, true));
        assertEquals(ForgeRules.Refusal.INCOMPATIBLE, ForgeRules.refusalToAttach(true, true, false, true));
        // The last Ash was taken out before the request was handled.
        assertEquals(ForgeRules.Refusal.NO_ASH, ForgeRules.refusalToAttach(true, true, true, false));
        assertEquals(ForgeRules.Refusal.NO_WEAPON, ForgeRules.refusalToAttach(false, false, false, false));
    }

    @Test
    void everyRefusalHasAMessageInTheLanguageFile() throws IOException {
        Path file = Paths.get("src", "main", "resources", "assets", "ashenskills", "lang", "en_us.lang");
        assertTrue(Files.isRegularFile(file), "run from the project folder: missing " + file.toAbsolutePath());
        Set<String> keys = new HashSet<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            int equals = line.indexOf('=');
            if (equals > 0 && equals < line.length() - 1) {
                keys.add(line.substring(0, equals));
            }
        }

        for (ForgeRules.Refusal refusal : ForgeRules.Refusal.values()) {
            assertTrue(keys.contains(refusal.messageKey()), "no text for " + refusal.messageKey());
        }
    }
}
