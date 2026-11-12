package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CooldownStoreTest {

    private static final long START = 1_000_000L;
    private static final long EIGHT_SECONDS = 8_000L;

    @Test
    void theCooldownCountsDownInWallClockTime() {
        long expiry = START + EIGHT_SECONDS;
        assertEquals(8_000, CooldownStore.remaining(expiry, EIGHT_SECONDS, START));
        assertEquals(5_000, CooldownStore.remaining(expiry, EIGHT_SECONDS, START + 3_000));
        assertEquals(1, CooldownStore.remaining(expiry, EIGHT_SECONDS, expiry - 1));
    }

    @Test
    void itIsOverAtAndAfterItsEnd() {
        long expiry = START + EIGHT_SECONDS;
        assertEquals(0, CooldownStore.remaining(expiry, EIGHT_SECONDS, expiry));
        assertEquals(0, CooldownStore.remaining(expiry, EIGHT_SECONDS, expiry + 60_000));
    }

    @Test
    void aClockSetBackNeverMakesItLongerThanItsOwnLength() {
        long expiry = START + EIGHT_SECONDS;
        long oneHourEarlier = START - 3_600_000;
        assertEquals(8_000, CooldownStore.remaining(expiry, EIGHT_SECONDS, oneHourEarlier));
    }

    @Test
    void aPlayerWithNoStoredCooldownHasNone() {
        assertEquals(0, CooldownStore.remaining(0, 0, START));
        assertEquals(0, CooldownStore.remaining(START + 5_000, 0, START));
    }
}
