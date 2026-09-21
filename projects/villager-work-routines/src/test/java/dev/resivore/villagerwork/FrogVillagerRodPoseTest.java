package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pins the inspected JEM reference values independently of the renderer/camera. */
class FrogVillagerRodPoseTest {
    @Test
    void preservesTheActualFoldedArmsReferenceAndAuthoredGripOffset() {
        assertEquals(43.0F, FrogVillagerRodPose.JEM_ARMS_ROTATION_DEGREES, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.AUTHORED_GRIP_X_PIXELS, 0.000001F);
        assertEquals(-7.0F, FrogVillagerRodPose.AUTHORED_GRIP_Y_PIXELS, 0.000001F);
        assertEquals(-6.0F, FrogVillagerRodPose.AUTHORED_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(-9.5F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS, 0.000001F);

        FrogVillagerRodPose.ArmLocalPoint tip = FrogVillagerRodPose.outerShaftTipFromFoldedArms();
        assertEquals(0.0D, tip.x(), 0.000001D);
        assertEquals(-7.0D / 16.0D, tip.y(), 0.000001D);
        assertEquals((-6.0D - 9.5D) / 16.0D, tip.z(), 0.000001D,
                "the line starts at the physical shaft tip, not at a guessed hand endpoint");
    }

    @Test
    void outerTipUsesTheInspectedCrossedArmsChainAndNeverCameraState() {
        FrogVillagerRodPose.Point south = FrogVillagerRodPose.outerShaftTip(10.0D, 64.0D, 20.0D, 0.0F);
        FrogVillagerRodPose.Point east = FrogVillagerRodPose.outerShaftTip(10.0D, 64.0D, 20.0D, -90.0F);

        assertTrue(south.isFinite());
        assertTrue(east.isFinite());
        assertEquals(10.0D, south.x(), 0.000001D);
        assertEquals(20.473107D, south.z(), 0.000001D);
        assertEquals(66.293951D, south.y(), 0.000001D);
        assertEquals(10.473107D, east.x(), 0.000001D);
        assertEquals(20.0D, east.z(), 0.000001D);
        assertFalse(FrogVillagerRodPose.outerShaftTip(Double.NaN, 0.0D, 0.0D, 0.0F).isFinite());
    }
}
