package com.mahghuuuls.ashenskills.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttributeFormulasTest {

    private static final double EXACT = 1.0e-9;

    @Test
    void asManyPointsAsTheScaleHalveAndFurtherPointsRemoveLess() {
        assertEquals(1.0, AttributeFormulas.remainingShare(100, 0), EXACT);
        assertEquals(0.5, AttributeFormulas.remainingShare(100, 100), EXACT);
        assertEquals(1.0 / 3, AttributeFormulas.remainingShare(100, 200), EXACT);
        assertEquals(0.8, AttributeFormulas.remainingShare(100, 25), EXACT);
        assertEquals(0.5, AttributeFormulas.remainingShare(25, 25), EXACT);
        assertEquals(0.8, AttributeFormulas.remainingShare(400, 100), EXACT);
    }

    @Test
    void pointsBelowZeroCountAsZero() {
        assertEquals(1.0, AttributeFormulas.remainingShare(100, -50), EXACT);
        assertEquals(1.0, AttributeFormulas.remainingShare(100, -1000), EXACT);
    }
}
