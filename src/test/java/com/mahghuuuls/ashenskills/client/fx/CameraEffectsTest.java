package com.mahghuuuls.ashenskills.client.fx;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class CameraEffectsTest {

    private static final float EXACT = 1.0e-6F;

    @Test
    void theCameraMoveIsTheClipValueTimesTheIntensity() {
        double[] clip = {2.0, -4.0, 8.0};

        assertArrayEquals(new float[] {2.0F, -4.0F, 8.0F}, CameraEffects.scaled(clip, 1.0), EXACT);
        assertArrayEquals(new float[] {1.0F, -2.0F, 4.0F}, CameraEffects.scaled(clip, 0.5), EXACT);
        assertArrayEquals(new float[] {0.0F, 0.0F, 0.0F}, CameraEffects.scaled(clip, 0.0), EXACT);
    }

    @Test
    void anIntensityOutsideZeroToOneIsHeldAtTheLimit() {
        double[] clip = {2.0, -4.0, 8.0};

        assertArrayEquals(new float[] {2.0F, -4.0F, 8.0F}, CameraEffects.scaled(clip, 3.0), EXACT);
        assertArrayEquals(new float[] {0.0F, 0.0F, 0.0F}, CameraEffects.scaled(clip, -1.0), EXACT);
    }
}
