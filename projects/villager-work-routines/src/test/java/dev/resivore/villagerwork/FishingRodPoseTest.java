package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingRodPoseTest {
    @Test void rodTipFacesTheVillagerBodyDirectionAndStaysFinite() {
        FishingRodPose.Point south = FishingRodPose.tip(10.0, 64.0, 20.0, 0.0F);
        FishingRodPose.Point east = FishingRodPose.tip(10.0, 64.0, 20.0, -90.0F);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertTrue(south.z() > 20.8, "yaw zero faces south/+Z");
        assertTrue(east.x() > 10.8, "yaw -90 faces east/+X");
        assertEquals(65.58, south.y(), 0.000001);
    }

    @Test void invalidInputsNeverProduceAUsableLineOrigin() {
        assertFalse(FishingRodPose.tip(Double.NaN, 0.0, 0.0, 0.0F).isFinite());
        assertFalse(FishingRodPose.tip(0.0, 0.0, 0.0, Float.POSITIVE_INFINITY).isFinite());
    }
}
