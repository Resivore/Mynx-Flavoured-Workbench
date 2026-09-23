package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.jupiter.api.Test;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(0.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_X_PIXELS, 0.000001F);
        assertEquals(1.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Y_PIXELS, 0.000001F);
        assertEquals(7.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Z_PIXELS, 0.000001F);
        assertEquals(-9.5F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.AUTHORED_GRIP_X, 0.000001F);
        assertEquals(-7.0F / 16.0F, FrogVillagerRodPose.AUTHORED_GRIP_Y, 0.000001F);
        assertEquals(-6.0F / 16.0F, FrogVillagerRodPose.AUTHORED_GRIP_Z, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.RUNTIME_GRIP_X, 0.000001F);
        assertEquals(7.0F / 16.0F, FrogVillagerRodPose.RUNTIME_GRIP_Y, 0.000001F);
        assertEquals(-6.0F / 16.0F, FrogVillagerRodPose.RUNTIME_GRIP_Z, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_X, 0.000001F);
        assertEquals(1.0F / 16.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Y, 0.000001F);
        assertEquals(7.0F / 16.0F, FrogVillagerRodPose.FINAL_ALIGNMENT_Z, 0.000001F);
        assertEquals(-9.5F / 16.0F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z,
                0.000001F);

        PoseStack poseStack = new PoseStack();
        FrogVillagerRodPose.applyC24ReferenceGrip(poseStack);
        Vector4f c24Origin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, c24Origin.x(), 0.000001F);
        assertEquals(7.0F / 16.0F, c24Origin.y(), 0.000001F);
        assertEquals(-6.0F / 16.0F, c24Origin.z(), 0.000001F);

        FrogVillagerRodPose.applyC30AlignmentCorrection(poseStack);
        Vector4f c30IncrementalOrigin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, c30IncrementalOrigin.x(), 0.000001F);
        assertEquals(8.0F / 16.0F, c30IncrementalOrigin.y(), 0.000001F);
        assertEquals(1.0F / 16.0F, c30IncrementalOrigin.z(), 0.000001F);
        // C30 is exactly -1px Y/up and +2px Z/inward relative to C29's (0,+2,+5)px correction.
        assertEquals(-1.0F / 16.0F,
                FrogVillagerRodPose.FINAL_ALIGNMENT_Y - (2.0F / 16.0F), 0.000001F);
        assertEquals(2.0F / 16.0F,
                FrogVillagerRodPose.FINAL_ALIGNMENT_Z - (5.0F / 16.0F), 0.000001F);

        poseStack = new PoseStack();
        FrogVillagerRodPose.applyC30ReferenceGrip(poseStack);
        Vector4f origin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(0.0F, origin.x(), 0.000001F);
        assertEquals(8.0F / 16.0F, origin.y(), 0.000001F);
        assertEquals(1.0F / 16.0F, origin.z(), 0.000001F);
    }

    @Test
    void c31ConvertsEntityParentUpThroughTheActualFoldedArmLinearBasis() {
        // Include unrelated pivots/translations and a parent rotation to prove that only the
        // relative 3x3 basis influences the vector conversion.
        Matrix4f parent = new Matrix4f().translation(23.0F, -17.0F, 11.0F)
                .rotateY(0.37F);
        Matrix4f effective = new Matrix4f(parent).translate(4.0F, -3.0F, 9.0F)
                .rotateX((float) Math.toRadians(FrogVillagerRodPose.JEM_ARMS_ROTATION_DEGREES));

        FrogVillagerRodPose.LocalTranslation local =
                FrogVillagerRodPose.deriveC31FoldedArmLocalDelta(parent, effective);
        assertEquals(0.0F, local.x(), 0.000001F);
        assertEquals(FrogVillagerRodPose.C31_REFERENCE_LOCAL_DELTA_Y_PIXELS / 16.0F,
                local.y(), 0.000001F);
        assertEquals(FrogVillagerRodPose.C31_REFERENCE_LOCAL_DELTA_Z_PIXELS / 16.0F,
                local.z(), 0.000001F);

        Matrix3f foldedToParent = new Matrix3f(parent).invert().mul(new Matrix3f(effective));
        Vector3f restoredParentDelta = foldedToParent.transform(
                new Vector3f(local.x(), local.y(), local.z()));
        assertEquals(FrogVillagerRodPose.C31_PARENT_DELTA_X, restoredParentDelta.x, 0.000001F);
        assertEquals(FrogVillagerRodPose.C31_PARENT_DELTA_Y, restoredParentDelta.y, 0.000001F);
        assertEquals(FrogVillagerRodPose.C31_PARENT_DELTA_Z, restoredParentDelta.z, 0.000001F);

        PoseStack poseStack = new PoseStack();
        FrogVillagerRodPose.applyC31ReferenceGrip(poseStack, parent, effective);
        Vector4f c31Origin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals((FrogVillagerRodPose.FINAL_ALIGNMENT_Y_PIXELS
                        + FrogVillagerRodPose.C31_REFERENCE_LOCAL_DELTA_Y_PIXELS) / 16.0F
                        + FrogVillagerRodPose.RUNTIME_GRIP_Y,
                c31Origin.y(), 0.000001F);
        assertEquals((FrogVillagerRodPose.FINAL_ALIGNMENT_Z_PIXELS
                        + FrogVillagerRodPose.C31_REFERENCE_LOCAL_DELTA_Z_PIXELS) / 16.0F
                        + FrogVillagerRodPose.RUNTIME_GRIP_Z,
                c31Origin.z(), 0.000001F);
    }

    @Test
    void c32MovesExactlyTwoPixelsAlongTheAuthoritativeGripToPhysicalTipAxis() {
        assertEquals(0.0F, FrogVillagerRodPose.ROD_LOCAL_GRIP_X_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.ROD_LOCAL_GRIP_Y_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.ROD_LOCAL_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.ROD_LOCAL_OUTER_SHAFT_TIP_X_PIXELS, 0.000001F);
        assertEquals(0.0F, FrogVillagerRodPose.ROD_LOCAL_OUTER_SHAFT_TIP_Y_PIXELS, 0.000001F);
        assertEquals(-9.5F, FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS, 0.000001F);
        assertEquals(2.0F, FrogVillagerRodPose.C32_SHAFT_TRANSLATION_PIXELS, 0.000001F);

        FrogVillagerRodPose.RodLocalTranslation translation =
                FrogVillagerRodPose.deriveC32ShaftAxisTranslation();
        // The normalized grip->tip shaft direction is exactly (0,0,-1), so C32 is (0,0,-2)px.
        assertEquals(0.0F, translation.x(), 0.000001F);
        assertEquals(0.0F, translation.y(), 0.000001F);
        assertEquals(-2.0F / 16.0F, translation.z(), 0.000001F);
        assertEquals(2.0F / 16.0F,
                (float) Math.sqrt(translation.x() * translation.x()
                        + translation.y() * translation.y() + translation.z() * translation.z()),
                0.000001F);
        // Positive dot product with the authoritative (0,0,-9.5px) tip vector proves its direction.
        assertTrue(translation.z() * FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z > 0.0F);

        PoseStack poseStack = new PoseStack();
        FrogVillagerRodPose.applyC32ShaftAxisTranslation(poseStack);
        Vector4f translatedOrigin = poseStack.last().pose().transform(new Vector4f(0, 0, 0, 1));
        assertEquals(translation.x(), translatedOrigin.x(), 0.000001F);
        assertEquals(translation.y(), translatedOrigin.y(), 0.000001F);
        assertEquals(translation.z(), translatedOrigin.z(), 0.000001F);
    }
}
