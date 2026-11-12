package com.mahghuuuls.ashenskills.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParticleBudgetTest {

    @Test
    void oneUseNeverGetsMoreThanTheLimitHoweverManyImpacts() {
        ParticleBudget budget = new ParticleBudget();

        assertEquals(12, budget.take(7, 1, 12, 30));
        assertEquals(12, budget.take(7, 1, 12, 30));
        assertEquals(6, budget.take(7, 1, 12, 30));
        assertEquals(0, budget.take(7, 1, 12, 30));
    }

    @Test
    void aLimitOfZeroGivesNoParticles() {
        ParticleBudget budget = new ParticleBudget();

        assertEquals(0, budget.take(7, 1, 12, 0));
    }

    @Test
    void aNewUseAndAnotherPlayerStartFromZero() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(30, budget.take(7, 1, 40, 30));

        assertEquals(12, budget.take(7, 2, 12, 30));
        assertEquals(12, budget.take(8, 1, 12, 30));
        assertEquals(18, budget.take(7, 2, 40, 30));
    }

    @Test
    void forgettingStartsEveryCountAgain() {
        ParticleBudget budget = new ParticleBudget();
        assertEquals(30, budget.take(7, 1, 40, 30));

        budget.clear();

        assertEquals(30, budget.take(7, 1, 40, 30));
    }
}
