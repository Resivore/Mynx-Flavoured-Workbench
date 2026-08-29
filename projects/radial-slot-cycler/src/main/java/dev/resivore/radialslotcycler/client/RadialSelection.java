package dev.resivore.radialslotcycler.client;

public final class RadialSelection {
    private static final double TOP_DIRECTION = -Math.PI / 2.0D;

    private RadialSelection() {}

    public static int fromPointer(
            double deltaX,
            double deltaY,
            int entryCount,
            double deadZoneRadius
    ) {
        if (entryCount <= 0) {
            throw new IllegalArgumentException("entryCount must be positive");
        }
        if (deadZoneRadius < 0.0D) {
            throw new IllegalArgumentException("deadZoneRadius must not be negative");
        }
        if (deltaX * deltaX + deltaY * deltaY < deadZoneRadius * deadZoneRadius) {
            return -1;
        }

        double sector = Math.PI * 2.0D / entryCount;
        double clockwiseFromTop = normalize(Math.atan2(deltaY, deltaX) - TOP_DIRECTION);
        return Math.floorMod((int) Math.floor((clockwiseFromTop + sector / 2.0D) / sector), entryCount);
    }

    public static double entryAngle(int index, int entryCount) {
        if (index < 0 || index >= entryCount || entryCount <= 0) {
            throw new IllegalArgumentException("Invalid radial index " + index + " of " + entryCount);
        }
        return TOP_DIRECTION + (Math.PI * 2.0D * index / entryCount);
    }

    private static double normalize(double angle) {
        double fullTurn = Math.PI * 2.0D;
        double normalized = angle % fullTurn;
        return normalized < 0.0D ? normalized + fullTurn : normalized;
    }
}
