package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingRodPoseTest {
    @Test void c17RestoresTheC15ArmLocalBaselineAndUsesAnUnrotatedBodySpaceInwardOffset() {
        assertEquals(0.35F, FishingRodPose.STICK_VERTICAL_TRANSLATION, 0.000001F);
        assertEquals(0.09F, FishingRodPose.STICK_VERTICAL_TRANSLATION - 0.26F, 0.000001F,
                "C14's positive direction lowered the stick, so C15 must continue positively");
        assertEquals(-0.54F, FishingRodPose.STICK_ARM_LOCAL_DEPTH_TRANSLATION, 0.000001F,
                "C16's failed -0.46 arm-local-Z experiment must not remain");
        assertEquals(0.0F, FishingRodPose.C17_INWARD_BODY_OFFSET.x(), 0.000001F);
        assertEquals(0.0F, FishingRodPose.C17_INWARD_BODY_OFFSET.y(), 0.000001F,
                "the body-space inward adjustment must not intentionally alter rod height");
        assertEquals(0.08F, FishingRodPose.C17_INWARD_BODY_OFFSET.z(), 0.000001F);
        assertTrue(FishingRodPose.C17_INWARD_BODY_OFFSET.hasNoVerticalComponent());
    }

    @Test void fallbackStickTipFacesTheVillagerBodyDirectionAndStaysFinite() {
        FishingRodPose.Point south = FishingRodPose.tip(10.0, 64.0, 20.0, 0.0F);
        FishingRodPose.Point east = FishingRodPose.tip(10.0, 64.0, 20.0, -90.0F);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertEquals(21.29, south.z(), 0.000001,
                "the fallback line origin follows the corrected physical body-space inward movement");
        assertEquals(11.29, east.x(), 0.000001,
                "the fallback line origin follows the corrected physical body-space inward movement");
        assertEquals(0.08, FishingRodPose.C15_TIP_FORWARD - (south.z() - 20.0), 0.000001,
                "the fallback line tip must move inward by exactly the rod's body-space correction");
        assertEquals(65.51, south.y(), 0.000001,
                "the fallback line origin keeps C15's visible height");
    }

    @Test void invalidInputsNeverProduceAUsableLineOrigin() {
        assertFalse(FishingRodPose.tip(Double.NaN, 0.0, 0.0, 0.0F).isFinite());
        assertFalse(FishingRodPose.tip(0.0, 0.0, 0.0, Float.POSITIVE_INFINITY).isFinite());
        assertFalse(FishingRodPose.tipFromCrossedArms(0.0, 0.0, 0.0, 0.0F,
                new FishingRodPose.ArmLocalPoint(Double.NaN, 0.0, 0.0)).isFinite());
    }

    @Test void explicitArmLocalTipUsesTheInspectedVillagerTransformWithoutCameraState() {
        FishingRodPose.ArmLocalPoint grip = new FishingRodPose.ArmLocalPoint(0.0, 0.0, 0.0);
        FishingRodPose.Point south = FishingRodPose.tipFromCrossedArms(10.0, 64.0, 20.0,
                0.0F, grip);
        FishingRodPose.Point east = FishingRodPose.tipFromCrossedArms(10.0, 64.0, 20.0,
                -90.0F, grip);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertEquals(10.0, south.x(), 0.000001,
                "the grip is centered on crossed arms before body-yaw conversion");
        assertEquals(65.3135, south.y(), 0.000001,
                "the exact 26.2 adult renderer and crossed-arms offsets determine grip height");
        assertEquals(20.0625, south.z(), 0.000001,
                "at body yaw zero, model-forward crossed arms map south in world space");
        assertEquals(10.0625, east.x(), 0.000001,
                "the same arm-local grip follows the actual interpolated body orientation");
        assertEquals(20.0, east.z(), 0.000001);

        FishingRodPose.ArmLocalPoint physicalTip = new FishingRodPose.ArmLocalPoint(0.0, 0.0, -1.0);
        FishingRodPose.Point forwardSouth = FishingRodPose.tipFromCrossedArms(10.0, 64.0, 20.0,
                0.0F, physicalTip);
        assertTrue(forwardSouth.z() > south.z(),
                "a shaft tip in the shared arm basis extends in front of the villager at yaw zero");
    }
}
