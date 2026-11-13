package com.mahghuuuls.ashenskills.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudStateTest {

    @Test
    void anItemWithoutASkillHasNoHud() {
        assertEquals(HudState.HIDDEN, HudState.of(false, false, 0));
        assertEquals(HudState.HIDDEN, HudState.of(false, false, 5000));
    }

    @Test
    void aUsableSkillIsReadyOrOnCooldown() {
        assertEquals(HudState.READY, HudState.of(true, true, 0));
        assertEquals(HudState.ON_COOLDOWN, HudState.of(true, true, 1));
        assertEquals(HudState.ON_COOLDOWN, HudState.of(true, true, 8000));
    }

    @Test
    void aSkillThatCannotBeUsedIsShownAsUnusableAlsoDuringACooldown() {
        assertEquals(HudState.UNUSABLE, HudState.of(true, false, 0));
        assertEquals(HudState.UNUSABLE, HudState.of(true, false, 8000));
    }

    @Test
    void theFrameSheetHoldsReadyThenCooldownThenUnusable() {
        assertEquals(0, HudState.READY.frame());
        assertEquals(1, HudState.ON_COOLDOWN.frame());
        assertEquals(2, HudState.UNUSABLE.frame());
    }

    @Test
    void theNumberIsTheWholeSecondsLeftAndChangesOnceASecond() {
        assertEquals(8, HudState.secondsShown(8000));
        assertEquals(8, HudState.secondsShown(7001));
        assertEquals(7, HudState.secondsShown(7000));
        assertEquals(1, HudState.secondsShown(1000));
        assertEquals(1, HudState.secondsShown(1));
        assertEquals(0, HudState.secondsShown(0));
        assertEquals(0, HudState.secondsShown(-50));
    }

    @Test
    void theShadeCoversTheWholeIconAtTheStartAndNothingAtTheEnd() {
        assertEquals(16, HudState.rowsCovered(8000, 8000, 16));
        assertEquals(8, HudState.rowsCovered(4000, 8000, 16));
        assertEquals(1, HudState.rowsCovered(1, 8000, 16));
        assertEquals(0, HudState.rowsCovered(0, 8000, 16));
        // A report with no length, or more left than the length, must not draw outside the icon.
        assertEquals(0, HudState.rowsCovered(500, 0, 16));
        assertEquals(16, HudState.rowsCovered(9000, 8000, 16));
    }
}
