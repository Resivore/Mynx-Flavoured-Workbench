package dev.resivore.villagerwork;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The user-authored Frog Villager CEM reference expressed in Minecraft 26.2 model units.
 *
 * <p>The reference places {@code EDIT_THIS__VWR_RIBBITS_ROD_POSE} under the real folded-arm
 * {@code arms_rotation} transform. Minecraft's {@code VillagerModel.translateToArms} supplies
 * that inspected folded-arm transform; this class supplies only the reference group's authored
 * local translation. The Ribbits shaft itself is deliberately not given another rotation.</p>
 */
public final class FrogVillagerRodPose {
    public static final float JEM_ARMS_ROTATION_DEGREES = 43.0F;
    public static final float AUTHORED_GRIP_X_PIXELS = 0.0F;
    public static final float AUTHORED_GRIP_Y_PIXELS = -7.0F;
    public static final float AUTHORED_GRIP_Z_PIXELS = -6.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F;

    /* Minecraft model units are one sixteenth of a Blockbench/JEM model pixel. */
    public static final float AUTHORED_GRIP_X = AUTHORED_GRIP_X_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Y = AUTHORED_GRIP_Y_PIXELS / 16.0F;
    public static final float AUTHORED_GRIP_Z = AUTHORED_GRIP_Z_PIXELS / 16.0F;
    public static final float OUTER_SHAFT_TIP_FROM_GRIP_Z = OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS / 16.0F;

    /* Inspected Minecraft 26.2 LivingEntityRenderer/VillagerModel crossed-arms chain. */
    static final double ADULT_RENDER_BASE_Y = -1.501D;
    static final double CROSSED_ARMS_PIVOT_Y = 3.0D / 16.0D;
    static final double CROSSED_ARMS_PIVOT_Z = -1.0D / 16.0D;
    static final double CROSSED_ARMS_X_ROTATION_RADIANS = -0.75D;

    private FrogVillagerRodPose() {
    }

    /** Applies exactly the reference group's local translation after the visible folded arms. */
    public static void applyReferenceGrip(PoseStack poseStack) {
        poseStack.translate(AUTHORED_GRIP_X, AUTHORED_GRIP_Y, AUTHORED_GRIP_Z);
    }

    /** The physical shaft tip under the same reference group used for the visible rod. */
    public static ArmLocalPoint outerShaftTipFromFoldedArms() {
        return new ArmLocalPoint(AUTHORED_GRIP_X, AUTHORED_GRIP_Y,
                AUTHORED_GRIP_Z + OUTER_SHAFT_TIP_FROM_GRIP_Z);
    }

    /** A physical point relative to the vanilla crossed-arms origin, in model-local units. */
    public record ArmLocalPoint(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    public record Point(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    /**
     * Converts the reference physical shaft tip through the exact adult Villager crossed-arms
     * transform. This is deliberately independent of the renderer's camera-relative PoseStack.
     */
    public static Point outerShaftTip(double villagerX, double villagerY, double villagerZ,
                                      float bodyYawDegrees) {
        ArmLocalPoint armLocalTip = outerShaftTipFromFoldedArms();
        if (!Double.isFinite(villagerX) || !Double.isFinite(villagerY) || !Double.isFinite(villagerZ)
                || !Float.isFinite(bodyYawDegrees) || !armLocalTip.isFinite()) {
            return new Point(Double.NaN, Double.NaN, Double.NaN);
        }

        double armCos = Math.cos(CROSSED_ARMS_X_ROTATION_RADIANS);
        double armSin = Math.sin(CROSSED_ARMS_X_ROTATION_RADIANS);
        double armsX = armLocalTip.x();
        double armsY = armLocalTip.y() * armCos - armLocalTip.z() * armSin
                + CROSSED_ARMS_PIVOT_Y + ADULT_RENDER_BASE_Y;
        double armsZ = armLocalTip.y() * armSin + armLocalTip.z() * armCos + CROSSED_ARMS_PIVOT_Z;

        double yaw = Math.toRadians(180.0D - bodyYawDegrees);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double flippedX = -armsX;
        double flippedY = -armsY;
        double flippedZ = armsZ;
        return new Point(villagerX + cos * flippedX + sin * flippedZ,
                villagerY + flippedY,
                villagerZ - sin * flippedX + cos * flippedZ);
    }
}
