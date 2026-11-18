package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WindowBudgetTest {

    @Test
    void theBudgetShrinksWithEveryTargetTheWindowDamaged() {
        WindowBudget budget = new WindowBudget(3);
        assertEquals(3, budget.left());

        budget.struck().add(11);
        budget.struck().add(12);
        assertEquals(1, budget.left());

        budget.struck().add(12);
        assertEquals(1, budget.left());

        budget.struck().add(13);
        assertEquals(0, budget.left());
    }

    @Test
    void aCapBelowZeroCountsAsNone() {
        assertEquals(0, new WindowBudget(-2).left());
    }
}
