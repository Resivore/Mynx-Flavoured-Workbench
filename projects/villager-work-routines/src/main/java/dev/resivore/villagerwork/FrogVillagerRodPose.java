package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The user-authored Frog Villager CEM reference expressed in Minecraft 26.2 model units.
 *
 * <p>The reference places {@code EDIT_THIS__VWR_RIBBITS_ROD_POSE} under the real folded-arm
 * drawing part. C24 reaches that part through EMF's authored-part metadata, then applies the
 * reference group's authored local translation through EMF's documented {@code invertAxis="xy"}
 * conversion. No corrective rotation or scale is introduced.</p>
 */
public final class FrogVillagerRodPose {
    public static final float JEM_ARMS_ROTATION_DEGREES = 43.0F;
    public static final float AUTHORED_GRIP_X_PIXELS = 0.0F;
    public static final float AUTHORED_GRIP_Y_PIXELS = -7.0F;
    public static final float AUTHORED_GRIP_Z_PIXELS = -6.0F;
    /** The rendered C27 rod's authoritative local grip; it is its shaft-axis origin. */
    public static final float ROD_LOCAL_GRIP_X_PIXELS = 0.0F;
    public static final float ROD_LOCAL_GRIP_Y_PIXELS = 0.0F;
    public static final float ROD_LOCAL_GRIP_Z_PIXELS = 0.0F;
    public static final float ROD_LOCAL_OUTER_SHAFT_TIP_X_PIXELS = 0.0F;
    public static final float ROD_LOCAL_OUTER_SHAFT_TIP_Y_PIXELS = 0.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F;
    public static final float C32_SHAFT_TRANSLATION_PIXELS = 2.0F;

    public static final String AUTHORED_INVERT_AXIS = "xy";
    public static final float RUNTIME_GRIP_X_PIXELS = -AUTHORED_GRIP_X_PIXELS;
    public static final float RUNTIME_GRIP_Y_PIXELS = -AUTHORED_GRIP_Y_PIXELS;
    public static final float RUNTIME_GRIP_Z_PIXELS = AUTHORED_GRIP_Z_PIXELS;
    /* C30: applied after the authored C24 grip in the live folded-arm local coordinate system. */
    public static final float FINAL_ALIGNMENT_X_PIXELS = 0.0F;
    public static final float FINAL_ALIGNMENT_Y_PIXELS = 1.0F;
    public static final float FINAL_ALIGNMENT_Z_PIXELS = 7.0F;
    /* C31 asks for this entity-parent displacement, not a direct local-axis adjustment. */
    public static final float C31_PARENT_DELTA_X_PIXELS = 0.0F;
    public static final float C31_PARENT_DELTA_Y_PIXELS = -2.0F;
    public static final float C31_PARENT_DELTA_Z_PIXELS = 0.0F;
    /* The inspected 43-degree reference basis gives these reporting values in the normal pose. */
    public static final float C31_REFERENCE_LOCAL_DELTA_X_PIXELS = 0.0F;
    public static final float C31_REFERENCE_LOCAL_DELTA_Y_PIXELS =
            -2.0F * (float) Math.cos(Math.toRadians(JEM_ARMS_ROTATION_DEGREES));
    public static final float C31_REFERENCE_LOCAL_DELTA_Z_PIXELS =
            2.0F * (float) Math.sin(Math.toRadians(JEM_ARMS_ROTATION_DEGREES));

    /* Minecraft model units are one sixteenth of a Blockbench/JEM model pixel. */
    public static final float AUTHORED_GRIP_X = AUTHORED_GRIP_X_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Y = AUTHORED_GRIP_Y_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Z = AUTHORED_GRIP_Z_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_X = RUNTIME_GRIP_X_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_Y = RUNTIME_GRIP_Y_PIXELS / 16.0F;
    public static final float RUNTIME_GRIP_Z = RUNTIME_GRIP_Z_PIXELS / 16.0F;
    public static final float FINAL_ALIGNMENT_X = FINAL_ALIGNMENT_X_PIXELS / 16.0F;
    public static final float FINAL_ALIGNMENT_Y = FINAL_ALIGNMENT_Y_PIXELS / 16.0F;
    public static final float FINAL_ALIGNMENT_Z = FINAL_ALIGNMENT_Z_PIXELS / 16.0F;
    public static final float C31_PARENT_DELTA_X = C31_PARENT_DELTA_X_PIXELS / 16.0F;
    public static final float C31_PARENT_DELTA_Y = C31_PARENT_DELTA_Y_PIXELS / 16.0F;
    public static final float C31_PARENT_DELTA_Z = C31_PARENT_DELTA_Z_PIXELS / 16.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z = OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS / 16.0F;

    private FrogVillagerRodPose() {
    }

    /** Applies C24's immutable authored grip with its proven EMF live-axis mapping. */
    public static void applyC24ReferenceGrip(PoseStack poseStack) {
        poseStack.translate(RUNTIME_GRIP_X, RUNTIME_GRIP_Y, RUNTIME_GRIP_Z);
    }

    /**
     * Applies C30's final local alignment correction at the already-authored C24 grip.
     * Runtime positive Y is down; positive Z moves the rod inward toward the folded hands.
     */
    public static void applyC30AlignmentCorrection(PoseStack poseStack) {
        poseStack.translate(FINAL_ALIGNMENT_X, FINAL_ALIGNMENT_Y, FINAL_ALIGNMENT_Z);
    }

    /** Applies the exact retained C30 pose, including its immutable post-C24 correction. */
    public static void applyC30ReferenceGrip(PoseStack poseStack) {
        applyC24ReferenceGrip(poseStack);
        applyC30AlignmentCorrection(poseStack);
    }

    /**
     * Applies C31 at C30's existing correction seam. The requested displacement is specified in
     * the parent of the authored folded-arm rotation: one model-space vertical direction, rather
     * than a camera direction. Only the relative matrices' linear 3x3 portions participate, so
     * pivots/translations cannot contaminate the vector conversion.
     */
    public static void applyC31ReferenceGrip(PoseStack poseStack, Matrix4f afterTranslateToArms,
                                              Matrix4f afterEffectiveFoldedArms) {
        applyC30ReferenceGrip(poseStack);
        LocalTranslation delta = deriveC31FoldedArmLocalDelta(afterTranslateToArms,
                afterEffectiveFoldedArms);
        poseStack.translate(delta.x(), delta.y(), delta.z());
    }

    /**
     * Converts C31's exact `(0,-2,0)` model-pixel parent vector through the live folded-arm basis.
     * This is intentionally a vector transformation (3x3 rotation/scale only), not a point
     * transformation. The returned values are Minecraft model/block units.
     */
    public static LocalTranslation deriveC31FoldedArmLocalDelta(Matrix4f afterTranslateToArms,
                                                                  Matrix4f afterEffectiveFoldedArms) {
        Matrix3f parentLinear = new Matrix3f(afterTranslateToArms);
        Matrix3f foldedLinear = new Matrix3f(afterEffectiveFoldedArms);
        Matrix3f foldedToParent = parentLinear.invert().mul(foldedLinear);
        Vector3f parentDelta = new Vector3f(C31_PARENT_DELTA_X, C31_PARENT_DELTA_Y,
                C31_PARENT_DELTA_Z);
        Vector3f localDelta = foldedToParent.invert().transform(parentDelta);
        return new LocalTranslation(localDelta.x, localDelta.y, localDelta.z);
    }

    /**
     * Derives C32's rigid rod-local shift from the authoritative grip-to-physical-tip vector.
     * This is intentionally not a villager-space Y/Z correction: it is exactly two model pixels
     * in the normalized shaft direction, converted to Minecraft model/block units.
     */
    public static RodLocalTranslation deriveC32ShaftAxisTranslation() {
        Vector3f gripToPhysicalTip = new Vector3f(
                ROD_LOCAL_OUTER_SHAFT_TIP_X_PIXELS - ROD_LOCAL_GRIP_X_PIXELS,
                ROD_LOCAL_OUTER_SHAFT_TIP_Y_PIXELS - ROD_LOCAL_GRIP_Y_PIXELS,
                OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS - ROD_LOCAL_GRIP_Z_PIXELS);
        float lengthPixels = gripToPhysicalTip.length();
        if (!(lengthPixels > 0.0F) || !Float.isFinite(lengthPixels)) {
            throw new IllegalStateException("C32 rod shaft axis must have a finite positive length");
        }
        gripToPhysicalTip.mul(C32_SHAFT_TRANSLATION_PIXELS / (lengthPixels * 16.0F));
        return new RodLocalTranslation(gripToPhysicalTip.x, gripToPhysicalTip.y, gripToPhysicalTip.z);
    }

    /** Applies only C32's complete-rod shaft-axis translation at the post-C31 rod seam. */
    public static void applyC32ShaftAxisTranslation(PoseStack poseStack) {
        RodLocalTranslation translation = deriveC32ShaftAxisTranslation();
        poseStack.translate(translation.x(), translation.y(), translation.z());
    }

    /** The derived folded-arm-local C31 delta in Minecraft model/block units. */
    public record LocalTranslation(float x, float y, float z) {
    }

    /** A translation expressed in the C27 rod's local model/block coordinate basis. */
    public record RodLocalTranslation(float x, float y, float z) {
    }
}
