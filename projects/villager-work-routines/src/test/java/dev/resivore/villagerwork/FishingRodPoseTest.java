package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingRodPoseTest {
    @Test void correctedStickTranslationReversesC13sEffectiveDirectionByTwoTimesItsAttempt() {
        assertEquals(0.26F, FishingRodPose.STICK_VERTICAL_TRANSLATION, 0.000001F);
        assertEquals(0.36F, FishingRodPose.STICK_VERTICAL_TRANSLATION - -0.10F, 0.000001F,
                "C13's -0.10 transform raised the stick in runtime; +0.26 lowers it instead");
    }

    @Test void rodTipFacesTheVillagerBodyDirectionAndStaysFinite() {
        FishingRodPose.Point south = FishingRodPose.tip(10.0, 64.0, 20.0, 0.0F);
        FishingRodPose.Point east = FishingRodPose.tip(10.0, 64.0, 20.0, -90.0F);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertTrue(south.z() > 21.0, "yaw zero places the tip forward of the crossed arms");
        assertTrue(east.x() > 11.0, "yaw -90 places the tip forward of the crossed arms");
        assertEquals(65.60, south.y(), 0.000001);
    }

    @Test void invalidInputsNeverProduceAUsableLineOrigin() {
        assertFalse(FishingRodPose.tip(Double.NaN, 0.0, 0.0, 0.0F).isFinite());
        assertFalse(FishingRodPose.tip(0.0, 0.0, 0.0, Float.POSITIVE_INFINITY).isFinite());
    }
}
