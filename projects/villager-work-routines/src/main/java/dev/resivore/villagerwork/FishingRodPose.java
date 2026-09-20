package dev.resivore.villagerwork;

/**
 * C17's model-independent stick fallback. The optional Ribbits rod path captures its line origin
 * from the rendered model transform instead of using these values.
 */
public final class FishingRodPose {
    /*
     * Minecraft 26.2 renderer contract inspected from the mapped client classes:
     * LivingEntityRenderer applies Y(180 - bodyYaw), then scale(-1, -1, 1), then translates
     * (0, -1.501, 0). VillagerModel.translateToArms then applies the authored arms part pose
     * offset (0, 3, -1)/16 and X rotation -0.75. These constants convert an explicit arm-local
     * Ribbits tip to world space without relying on the camera-relative layer PoseStack.
     */
    static final double ADULT_RENDER_BASE_Y = -1.501D;
    static final double CROSSED_ARMS_PIVOT_Y = 3.0D / 16.0D;
    static final double CROSSED_ARMS_PIVOT_Z = -1.0D / 16.0D;
    static final double CROSSED_ARMS_X_ROTATION_RADIANS = -0.75D;

    /**
     * `translateToArms` uses a model-local vertical axis whose effective screen direction was
     * established by C13 runtime evidence: changing +0.08 to -0.10 visibly raised the stick.
     * C14's +0.26 value visibly lowered the stick.  Continue in that proven direction by a
     * deliberately narrow +0.09 refinement without changing its rotation.
     */
    public static final float STICK_VERTICAL_TRANSLATION = 0.35F;
    /**
     * C15's arm-local depth translation.  C16's -0.46 experiment is deliberately not retained:
     * runtime evidence showed its +0.08 local-Z delta moved the rod down rather than inward.
     */
    public static final float STICK_ARM_LOCAL_DEPTH_TRANSLATION = -0.54F;

    /**
     * The small C17 physical correction, expressed before {@code VillagerModel.translateToArms}.
     * In the unrotated villager model basis, positive Z is back toward the torso from the
     * forward-held rod.  Keeping Y at zero prevents the arms' -0.75-radian pitch from turning
     * this inward adjustment into the vertical movement seen in C16.
     */
    public static final BodySpaceOffset C17_INWARD_BODY_OFFSET = new BodySpaceOffset(0.0F, 0.0F, 0.08F);

    /** A displacement in the unrotated villager body/model basis. */
    public record BodySpaceOffset(float x, float y, float z) {
        public boolean hasNoVerticalComponent() {
            return y == 0.0F;
        }
    }

    // The fallback line origin starts from C15's verified approximation, then follows the same
    // body-space inward movement as the fallback stick. Positive model Z toward the torso
    // reduces its outward body-yaw distance without changing height.
    static final double C15_TIP_FORWARD = 1.37;
    static final double TIP_FORWARD = C15_TIP_FORWARD - C17_INWARD_BODY_OFFSET.z();
    private static final double TIP_RIGHT = 0.10;
    private static final double TIP_HEIGHT = 1.51;

    private FishingRodPose() {}

    public record Point(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    /** A physical point relative to the vanilla crossed-arms grip, in arm/model-local units. */
    public record ArmLocalPoint(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    /**
     * Converts a standalone rod point from the crossed-arms grip to world space. This matches
     * Minecraft 26.2's adult VillagerModel/LivingEntityRenderer transform sequence and receives
     * only the interpolated entity position and body orientation; camera state is not an input.
     */
    public static Point tipFromCrossedArms(double villagerX, double villagerY, double villagerZ,
                                            float bodyYawDegrees, ArmLocalPoint armLocalTip) {
        if (!Double.isFinite(villagerX) || !Double.isFinite(villagerY) || !Double.isFinite(villagerZ)
                || !Float.isFinite(bodyYawDegrees) || armLocalTip == null || !armLocalTip.isFinite())
            return new Point(Double.NaN, Double.NaN, Double.NaN);

        double armCos = Math.cos(CROSSED_ARMS_X_ROTATION_RADIANS);
        double armSin = Math.sin(CROSSED_ARMS_X_ROTATION_RADIANS);
        double armsX = armLocalTip.x();
        double armsY = armLocalTip.y() * armCos - armLocalTip.z() * armSin
                + CROSSED_ARMS_PIVOT_Y + ADULT_RENDER_BASE_Y;
        double armsZ = armLocalTip.y() * armSin + armLocalTip.z() * armCos + CROSSED_ARMS_PIVOT_Z;

        // LivingEntityRenderer's -1/-1/1 model flip precedes its Y(180 - bodyYaw) rotation.
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

    /** Returns the approximate outer tip of the fallback forward-held stick. */
    public static Point tip(double villagerX, double villagerY, double villagerZ, float bodyYawDegrees) {
        if (!Double.isFinite(villagerX) || !Double.isFinite(villagerY) || !Double.isFinite(villagerZ)
                || !Float.isFinite(bodyYawDegrees)) return new Point(Double.NaN, Double.NaN, Double.NaN);
        double yaw = Math.toRadians(bodyYawDegrees);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        return new Point(villagerX + forwardX * TIP_FORWARD + rightX * TIP_RIGHT,
                villagerY + TIP_HEIGHT,
                villagerZ + forwardZ * TIP_FORWARD + rightZ * TIP_RIGHT);
    }
}
