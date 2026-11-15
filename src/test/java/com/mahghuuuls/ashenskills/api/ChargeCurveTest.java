package com.mahghuuuls.ashenskills.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.combat.math.DamageFormula;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import org.junit.jupiter.api.Test;

class ChargeCurveTest {

    private static final double EXACT = 1.0e-9;

    /** Minimum 0.5 s, full 2.0 s, 80 percent more at full: the shipped Crushing Blow. */
    private static ChargeCurve curve() {
        return new ChargeCurve(0.5, 2.0, 0.8);
    }

    @Test
    void aChargeBelowTheMinimumDoesNotExecuteAndTheMinimumItselfDoes() {
        assertFalse(curve().reachesMinimum(0.0));
        assertFalse(curve().reachesMinimum(0.45));
        assertTrue(curve().reachesMinimum(0.5));
        assertTrue(curve().reachesMinimum(3.0));
    }

    @Test
    void theBonusIsZeroAtTheMinimumAndTheWholeBonusAtTheFullCharge() {
        assertEquals(0.0, curve().bonus(0.5), EXACT);
        assertEquals(0.8, curve().bonus(2.0), EXACT);
    }

    @Test
    void theBonusGrowsInAStraightLineBetweenTheTwo() {
        assertEquals(0.4, curve().bonus(1.25), EXACT);
        assertEquals(0.2, curve().bonus(0.875), EXACT);
        assertEquals(0.6, curve().bonus(1.625), EXACT);
    }

    @Test
    void holdingPastTheFullChargeKeepsTheWholeBonusAndNoMore() {
        assertEquals(0.8, curve().bonus(2.05), EXACT);
        assertEquals(0.8, curve().bonus(60.0), EXACT);
        assertTrue(curve().isFull(2.0));
        assertFalse(curve().isFull(1.95));
    }

    @Test
    void aChargeBelowTheMinimumIsWorthNothing() {
        assertEquals(0.0, curve().bonus(0.0), EXACT);
        assertEquals(0.0, curve().bonus(0.3), EXACT);
    }

    /** The server plays the full-charge cue in the one tick where "full" turns true. */
    @Test
    void aChargeBecomesFullInExactlyOneTickAlsoWhenTheFullTimeIsNotAWholeTick() {
        for (ChargeCurve tested : new ChargeCurve[] {curve(), new ChargeCurve(0.5, 2.02, 0.8), new ChargeCurve(0.0, 0.0, 0.8)}) {
            int turns = 0;
            // The server asks once per tick of a charge, from tick 1.
            for (int tick = 1; tick <= 200; tick++) {
                if (tested.becomesFullAtTick(tick, 20)) {
                    turns++;
                }
            }
            assertEquals(1, turns);
        }
        assertTrue(curve().becomesFullAtTick(40, 20));
        assertFalse(curve().becomesFullAtTick(39, 20));
        assertFalse(curve().becomesFullAtTick(41, 20));
        // A full charge of no time is full at the first tick, and that tick gets the cue.
        assertTrue(new ChargeCurve(0.0, 0.0, 0.8).becomesFullAtTick(1, 20));
        assertTrue(new ChargeCurve(0.5, 2.02, 0.8).becomesFullAtTick(41, 20));
        assertTrue(new ChargeCurve(0.5, 2.02, 0.8).isFull(41 / 20.0));
        assertFalse(new ChargeCurve(0.5, 2.02, 0.8).isFull(40 / 20.0));
        assertEquals(0.8, new ChargeCurve(0.5, 2.02, 0.8).bonus(41 / 20.0), EXACT);
    }

    @Test
    void theBarRunsFromEmptyToFullOverTheFullChargeWithTheMinimumAQuarterIn() {
        assertEquals(0.0, curve().progress(0.0), EXACT);
        assertEquals(0.25, curve().progress(0.5), EXACT);
        assertEquals(0.5, curve().progress(1.0), EXACT);
        assertEquals(1.0, curve().progress(2.0), EXACT);
        assertEquals(1.0, curve().progress(9.0), EXACT);
        assertEquals(0.25, curve().minimumProgress(), EXACT);
    }

    @Test
    void aFullTimeBelowTheMinimumCountsAsTheMinimum() {
        ChargeCurve odd = new ChargeCurve(1.0, 0.4, 0.5);

        // Between the stated full time and the minimum the charge is neither full nor at the bar's end.
        assertFalse(odd.isFull(0.7));
        assertEquals(0.7, odd.progress(0.7), EXACT);
        assertFalse(odd.reachesMinimum(0.9));
        assertEquals(0.0, odd.bonus(0.9), EXACT);
        // At the minimum itself the charge is full, and worth the whole bonus.
        assertTrue(odd.isFull(1.0));
        assertEquals(0.5, odd.bonus(1.0), EXACT);
        assertEquals(1.0, odd.minimumProgress(), EXACT);
    }

    @Test
    void theShippedCrushingBlowAtFullChargeDealsItsMultiplierTimesOnePointEight() {
        SkillDefinition blow = BuiltInSkills.definitionOf("crushing_blow", DefaultsData.loadBundled());
        ChargeCurve shipped = ChargeCurve.of(blow);
        double multiplier = blow.number(SkillNumber.DAMAGE_MULTIPLIER);

        assertEquals(1.5, multiplier, EXACT);
        assertEquals(10.0 * 1.5 * 1.8, DamageFormula.hitDamage(10.0, 0.0, multiplier, shipped.bonus(2.0), 0.0), 0.01);
        assertEquals(10.0 * 1.5 * 1.4, DamageFormula.hitDamage(10.0, 0.0, multiplier, shipped.bonus(1.25), 0.0), 0.01);
        assertEquals(10.0 * 1.5, DamageFormula.hitDamage(10.0, 0.0, multiplier, shipped.bonus(0.5), 0.0), 0.01);
    }
}
