package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class HitScheduleTest {

    @Test
    void aWindowComesDueOnceWhenItsTickIsReached() {
        HitSchedule schedule = new HitSchedule(Arrays.asList(0.30), 8);
        assertEquals(-1, schedule.nextDue(5));
        assertEquals(0, schedule.nextDue(6));
        assertEquals(-1, schedule.nextDue(6));
        assertEquals(-1, schedule.nextDue(10));
    }

    @Test
    void severalWindowsComeDueInTimeOrderEvenWhenSkippedPast() {
        HitSchedule schedule = new HitSchedule(Arrays.asList(0.40, 0.10, 0.25), 8);
        assertEquals(0, schedule.nextDue(2));
        assertEquals(-1, schedule.nextDue(2));
        assertEquals(1, schedule.nextDue(9));
        assertEquals(2, schedule.nextDue(9));
        assertEquals(-1, schedule.nextDue(9));
    }

    @Test
    void windowsBeyondTheCapNeverComeDue() {
        HitSchedule schedule = new HitSchedule(Arrays.asList(0.05, 0.10, 0.15), 2);
        assertEquals(0, schedule.nextDue(20));
        assertEquals(1, schedule.nextDue(20));
        assertEquals(-1, schedule.nextDue(20));
    }

    @Test
    void aSkillWithoutWindowsNeverHits() {
        assertEquals(-1, new HitSchedule(Collections.<Double>emptyList(), 8).nextDue(100));
    }
}
