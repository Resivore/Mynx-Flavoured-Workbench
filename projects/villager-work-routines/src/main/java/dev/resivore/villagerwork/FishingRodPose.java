package dev.resivore.villagerwork;

/**
 * C17's model-independent stick fallback. The optional Ribbits rod path captures its line origin
 * from the rendered model transform instead of using these values.
 */
public final class FishingRodPose {
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
