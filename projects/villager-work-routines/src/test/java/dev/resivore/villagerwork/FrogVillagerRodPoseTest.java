package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;
import org.joml.Vector4f;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pins the inspected JEM reference values independently of the renderer/camera. */
class FrogVillagerRodPoseTest {
    @Test
    void preservesTheActualFoldedArmsReferenceAndAuthoredGripOffset() {
        assertEquals(43.0F, FrogVillagerRodPose.JEM_ARMS_ROTATION_DEGREES, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.AUTHORED_GRIP_X_PIXELS, 0.000001F);
        assertEquals(-7.0F, FrogVillagerRodPose.AUTHORED_GRIP_Y_PIXELS, 0.000001F);
        assertEquals(-6.0F, FrogVillagerRodPose.AUTHORED_GRIP_Z_PIXELS, 0.000001F);
        assertEquals("xy", FrogVillagerRodPose.AUTHORED_INVERT_AXIS);
        assertEquals(0.0F, FrogVillagerRodPose.RUNTIME_GRIP_X_PIXELS, 0.000001F);
        assertEquals(7.0F, FrogVillagerRodPose.RUNTIME_GRIP_Y_PIXELS, 0.000001F);
        assertEquals(-6.0F, FrogVillagerRodPose.RUNTIME_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(2.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Y_PIXELS, 0.000001F);
        assertEquals(1.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Z_PIXELS, 0.000001F);
        assertEquals(-9.5F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.AUTHORED_GRIP_X, 0.000001F);
        assertEquals(-7.0F / 16.0F, FrogVillagerRodPose.AUTHORED_GRIP_Y, 0.000001F);
        assertEquals(-6.0F / 16.0F, FrogVillagerRodPose.AUTHORED_GRIP_Z, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.RUNTIME_GRIP_X, 0.000001F);
        assertEquals(7.0F / 16.0F, FrogVillagerRodPose.RUNTIME_GRIP_Y, 0.000001F);
        assertEquals(-6.0F / 16.0F, FrogVillagerRodPose.RUNTIME_GRIP_Z, 0.000001F);
        assertEquals(2.0F / 16.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Y, 0.000001F);
        assertEquals(1.0F / 16.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Z, 0.000001F);
        assertEquals(-9.5F / 16.0F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z,
                0.000001F);

        PoseStack poseStack = new PoseStack();
        FrogVillagerRodPose.applyC24ReferenceGrip(poseStack);
        Vector4f c24Origin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, c24Origin.x(), 0.000001F);
        assertEquals(7.0F / 16.0F, c24Origin.y(), 0.000001F);
        assertEquals(-6.0F / 16.0F, c24Origin.z(), 0.000001F);

        FrogVillagerRodPose.applyC25AlignmentCorrection(poseStack);
        Vector4f c25IncrementalOrigin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, c25IncrementalOrigin.x(), 0.000001F);
        assertEquals(9.0F / 16.0F, c25IncrementalOrigin.y(), 0.000001F);
        assertEquals(-5.0F / 16.0F, c25IncrementalOrigin.z(), 0.000001F);

        poseStack = new PoseStack();
        FrogVillagerRodPose.applyReferenceGrip(poseStack);
        Vector4f origin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, origin.x(), 0.000001F);
        assertEquals(9.0F / 16.0F, origin.y(), 0.000001F);
        assertEquals(-5.0F / 16.0F, origin.z(), 0.000001F);
    }
}
