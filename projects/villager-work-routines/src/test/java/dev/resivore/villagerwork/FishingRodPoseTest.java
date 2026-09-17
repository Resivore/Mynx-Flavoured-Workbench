package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingRodPoseTest {
    @Test void refinedStickTranslationKeepsC15sLowerPoseAndMovesFurtherInward() {
        assertEquals(0.35F, FishingRodPose.STICK_VERTICAL_TRANSLATION, 0.000001F);
        assertEquals(0.09F, FishingRodPose.STICK_VERTICAL_TRANSLATION - 0.26F, 0.000001F,
                "C14's positive direction lowered the stick, so C15 must continue positively");
        assertEquals(-0.46F, FishingRodPose.STICK_FORWARD_TRANSLATION, 0.000001F);
        assertEquals(0.08F, FishingRodPose.STICK_FORWARD_TRANSLATION - -0.54F, 0.000001F,
                "C16 pulls the visible stick one more small step inward without changing its lower pose");
    }

    @Test void rodTipFacesTheVillagerBodyDirectionAndStaysFinite() {
        FishingRodPose.Point south = FishingRodPose.tip(10.0, 64.0, 20.0, 0.0F);
        FishingRodPose.Point east = FishingRodPose.tip(10.0, 64.0, 20.0, -90.0F);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertEquals(21.29, south.z(), 0.000001,
                "the shared line origin moves further inward with the visible stick");
        assertEquals(11.29, east.x(), 0.000001,
                "the shared line origin moves further inward with the visible stick");
        assertEquals(65.51, south.y(), 0.000001,
                "the shared line origin lowers with the visible stick");
    }

    @Test void invalidInputsNeverProduceAUsableLineOrigin() {
        assertFalse(FishingRodPose.tip(Double.NaN, 0.0, 0.0, 0.0F).isFinite());
        assertFalse(FishingRodPose.tip(0.0, 0.0, 0.0, Float.POSITIVE_INFINITY).isFinite());
    }
}
