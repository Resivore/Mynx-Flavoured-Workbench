package dev.aero.shulkertrowel.geometry;

import java.util.Optional;

public enum TargetGeometry {
    FULL,
    SLAB,
    STAIR,
    WALL,
    VERTICAL_SLAB,
    STEP;

    private static final TargetGeometry[] VALUES = values();

    public int networkId() {
        return ordinal();
    }

    public static TargetGeometry byNetworkId(int id) {
        return fromNetworkId(id).orElse(FULL);
    }

    public static Optional<TargetGeometry> fromNetworkId(int id) {
        return id >= 0 && id < VALUES.length
                ? Optional.of(VALUES[id])
                : Optional.empty();
    }
}
