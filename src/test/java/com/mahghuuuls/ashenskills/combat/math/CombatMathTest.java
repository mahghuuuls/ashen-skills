package com.mahghuuuls.ashenskills.combat.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.combat.math.NearestFirst.Ranked;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombatMathTest {

    @Test
    void weaponDamageTimesTheSkillMultiplier() {
        assertEquals(12.6F, DamageFormula.hitDamage(7, 0, 1.8, 0, 0), 0.001F);
    }

    @Test
    void theEnchantmentBonusIsAddedBeforeTheMultiplierAndOnlyOnce() {
        assertEquals(18.0F, DamageFormula.hitDamage(7, 3, 1.8, 0, 0), 0.001F);
    }

    @Test
    void chargeAndSkillDamageEachScaleTheWholeHit() {
        assertEquals(1.5F * 1.8F * 10, DamageFormula.hitDamage(10, 0, 1.5, 0.8, 0), 0.001F);
        assertEquals(1.5F * 10 * 1.5F, DamageFormula.hitDamage(10, 0, 1.5, 0, 50), 0.001F);
        assertEquals(10 * 2 * 1.5F * 1.25F, DamageFormula.hitDamage(8, 2, 2, 0.5, 25), 0.001F);
    }

    @Test
    void damageIsNeverNegative() {
        assertEquals(0F, DamageFormula.hitDamage(-4, 0, 1.8, 0, 0), 0F);
        assertEquals(0F, DamageFormula.hitDamage(5, 0, 1.8, 0, -150), 0F);
    }

    private static List<Ranked<String>> ranked(Object... nameThenDistance) {
        List<Ranked<String>> list = new ArrayList<>();
        for (int i = 0; i < nameThenDistance.length; i += 2) {
            list.add(new Ranked<>((String) nameThenDistance[i], ((Number) nameThenDistance[i + 1]).doubleValue()));
        }
        return list;
    }

    @Test
    void theNearestAreKeptUpToTheCapNearestFirst() {
        List<Ranked<String>> candidates = ranked("far", 9.0, "near", 1.0, "middle", 4.0, "farthest", 16.0);
        assertEquals(Arrays.asList("near", "middle"), NearestFirst.pick(candidates, 2));
        assertEquals(Arrays.asList("near", "middle", "far", "farthest"), NearestFirst.pick(candidates, 24));
        assertEquals(Collections.emptyList(), NearestFirst.pick(candidates, 0));
    }

    @Test
    void candidatesAtTheSameDistanceKeepTheirOrder() {
        assertEquals(Arrays.asList("first", "second"), NearestFirst.pick(ranked("first", 2.0, "second", 2.0, "third", 2.0), 2));
    }

    @Test
    void aStrikeReachesOneBlockBelowAndThreeAboveTheFeet() {
        // A box 1.8 high with its feet at the given height; the user's feet are at 10.
        assertTrue(VerticalReach.overlaps(10, 1.0, 3.0, 12.9, 14.7));
        assertFalse(VerticalReach.overlaps(10, 1.0, 3.0, 13.1, 14.9));
        assertTrue(VerticalReach.overlaps(10, 1.0, 3.0, 7.3, 9.1));
        assertFalse(VerticalReach.overlaps(10, 1.0, 3.0, 7.1, 8.9));
        assertFalse(VerticalReach.overlaps(10, 0.0, 3.0, 7.3, 9.1));
    }
}
