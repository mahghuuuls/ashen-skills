package com.mahghuuuls.ashenskills.client.hud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.runtime.RefusalReason;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefusalDisplayTest {

    @Test
    void aReasonShowsTheFirstTimeAndThenOnlyAfterTheInterval() {
        assertTrue(RefusalDisplay.isDue(null, 5_000, 1_000));
        assertFalse(RefusalDisplay.isDue(5_000L, 5_001, 1_000));
        assertFalse(RefusalDisplay.isDue(5_000L, 5_999, 1_000));
        assertTrue(RefusalDisplay.isDue(5_000L, 6_000, 1_000));
        assertTrue(RefusalDisplay.isDue(5_000L, 9_000, 1_000));
    }

    @Test
    void aKeyHeldForFiveSecondsShowsAtMostFiveMessages() {
        Long last = null;
        int shown = 0;
        // One refusal every 50 milliseconds, from 0 up to but not including 5 seconds.
        for (long now = 0; now < 5_000; now += 50) {
            if (RefusalDisplay.isDue(last, now, 1_000)) {
                last = now;
                shown++;
            }
        }
        assertTrue(shown == 5, "shown " + shown);
    }

    @Test
    void everyReasonAPlayerSeesHasATextInTheLanguageFile() throws IOException {
        Path file = Paths.get("src", "main", "resources", "assets", "ashenskills", "lang", "en_us.lang");
        assertTrue(Files.isRegularFile(file), "run from the project folder: missing " + file.toAbsolutePath());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        for (RefusalReason reason : RefusalReason.values()) {
            if (!reason.isShownToPlayer()) {
                continue;
            }
            String key = reason.messageKey() + "=";
            boolean found = false;
            for (String line : lines) {
                found |= line.startsWith(key) && line.length() > key.length();
            }
            assertTrue(found, "no text for " + key);
        }
    }
}
