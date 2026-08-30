package dev.aero.shulkertrowel.geometry;

import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TargetGeometry {
    FULL(0, NativeRole.FULL),
    SLAB(1, NativeRole.SLAB),
    STAIR(2, NativeRole.STAIR),
    WALL(3, NativeRole.WALL),
    VERTICAL_SLAB(4, DerivedGeometrySupport.Geometry.VERTICAL_SLAB),
    STEP(5, DerivedGeometrySupport.Geometry.STEP),
    LAYER(6, DerivedGeometrySupport.Geometry.LAYER);

    private static final TargetGeometry[] VALUES = values();
    private static final List<TargetGeometry> ORDERED = List.copyOf(Arrays.asList(VALUES));

    private final int networkId;
    private final NativeRole nativeRole;
    private final DerivedGeometrySupport.Geometry derivedGeometry;

    TargetGeometry(int networkId, NativeRole nativeRole) {
        this.networkId = networkId;
        this.nativeRole = nativeRole;
        this.derivedGeometry = null;
    }

    TargetGeometry(int networkId, DerivedGeometrySupport.Geometry derivedGeometry) {
        this.networkId = networkId;
        this.nativeRole = null;
        this.derivedGeometry = derivedGeometry;
    }

    public int networkId() {
        return networkId;
    }

    public int selectorIndex() {
        return ORDERED.indexOf(this);
    }

    public Optional<NativeRole> nativeRole() {
        return Optional.ofNullable(nativeRole);
    }

    public Optional<DerivedGeometrySupport.Geometry> derivedGeometry() {
        return Optional.ofNullable(derivedGeometry);
    }

    /** Stable selector order, independent of persistent/network decoding. */
    public static List<TargetGeometry> ordered() {
        return ORDERED;
    }

    public static Optional<TargetGeometry> fromSelectorIndex(int index) {
        return index >= 0 && index < ORDERED.size()
                ? Optional.of(ORDERED.get(index))
                : Optional.empty();
    }

    public static TargetGeometry byNetworkId(int id) {
        return fromNetworkId(id).orElse(FULL);
    }

    public static Optional<TargetGeometry> fromNetworkId(int id) {
        return ORDERED.stream().filter(geometry -> geometry.networkId == id).findFirst();
    }

    public enum NativeRole { FULL, SLAB, STAIR, WALL }
}
