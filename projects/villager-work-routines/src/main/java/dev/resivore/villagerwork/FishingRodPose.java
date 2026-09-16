package dev.resivore.villagerwork;

/**
 * Shared, model-independent approximation for the visible Fisherman rod.  It deliberately uses
 * body yaw rather than player-arm bones, so frog-villager resource packs retain control of the
 * villager model while the rod and line still agree on a stable physical direction.
 */
public final class FishingRodPose {
    private static final double TIP_FORWARD = 1.10;
    private static final double TIP_RIGHT = 0.10;
    private static final double TIP_HEIGHT = 1.82;

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
