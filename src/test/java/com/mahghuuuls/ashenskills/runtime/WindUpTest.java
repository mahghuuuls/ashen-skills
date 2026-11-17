package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindUpTest {

    @Test
    void halfASecondIsTenTicks() {
        assertEquals(10, WindUp.ticks(0.5));
        assertFalse(WindUp.isOver(9, 0.5));
        assertTrue(WindUp.isOver(10, 0.5));
        assertTrue(WindUp.isOver(11, 0.5));
    }

    @Test
    void aPartOfATickCountsAsAWholeTick() {
        assertEquals(1, WindUp.ticks(0.01));
        assertEquals(11, WindUp.ticks(0.51));
        assertEquals(6, WindUp.ticks(0.3));
    }

    @Test
    void noWindUpIsOverFromTheFirstMoment() {
        assertEquals(0, WindUp.ticks(0));
        assertTrue(WindUp.isOver(0, 0));
        assertEquals(0, WindUp.ticks(-1));
    }
}
