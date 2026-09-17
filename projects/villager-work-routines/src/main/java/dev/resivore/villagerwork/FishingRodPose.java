package dev.resivore.villagerwork;

/**
 * Shared, model-independent approximation for the visible Fisherman rod.  It deliberately uses
 * body yaw rather than player-arm bones, so frog-villager resource packs retain control of the
 * villager model while the rod and line still agree on a stable physical direction.
 */
public final class FishingRodPose {
    /**
     * `translateToArms` uses a model-local vertical axis whose effective screen direction was
     * established by C13 runtime evidence: changing +0.08 to -0.10 visibly raised the stick.
     * Move +0.36 from C13's value to lower it substantially without changing its rotation.
     */
    public static final float STICK_VERTICAL_TRANSLATION = 0.26F;
    // Keep this in step with VwrFishingRodLayer's further-forward grip transform.  The visible
    // line should originate at the stick's real outer end, not at the villager's arm plane.
    private static final double TIP_FORWARD = 1.45;
    private static final double TIP_RIGHT = 0.10;
    private static final double TIP_HEIGHT = 1.60;

    private FishingRodPose() {}

    public record Point(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    /** Returns the approximate outer tip of the custom-rendered, forward-held vanilla rod. */
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
