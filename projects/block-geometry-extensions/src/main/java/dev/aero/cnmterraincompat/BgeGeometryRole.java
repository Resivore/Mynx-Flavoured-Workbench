package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;

import java.util.Optional;

/**
 * BGE's complete geometry vocabulary.
 *
 * <p>The first three roles retain their exact Nibaru C46 identity. New BGE-owned
 * roles deliberately remain outside that frozen dependency contract.</p>
 */
public enum BgeGeometryRole {
    VERTICAL_SLAB(DerivedGeometrySupport.Geometry.VERTICAL_SLAB, 2),
    STEP(DerivedGeometrySupport.Geometry.STEP, 1),
    LAYER(DerivedGeometrySupport.Geometry.LAYER, 4),
    CORNER(null, 4),
    QUARTER_COLUMN(null, 4);

    private final DerivedGeometrySupport.Geometry legacyGeometry;
    private final int fuelDivisor;

    BgeGeometryRole(DerivedGeometrySupport.Geometry legacyGeometry, int fuelDivisor) {
        this.legacyGeometry = legacyGeometry;
        this.fuelDivisor = fuelDivisor;
    }

    public Optional<DerivedGeometrySupport.Geometry> legacyGeometry() {
        return Optional.ofNullable(legacyGeometry);
    }

    public int fuelDivisor() {
        return fuelDivisor;
    }

    /** Roles whose concrete state carrier and model contract are owned by BGE. */
    public boolean isBgeOwned() {
        return this == LAYER || this == CORNER || this == QUARTER_COLUMN;
    }

    public static BgeGeometryRole fromLegacy(DerivedGeometrySupport.Geometry geometry) {
        return switch (geometry) {
            case VERTICAL_SLAB -> VERTICAL_SLAB;
            case STEP -> STEP;
            case LAYER -> LAYER;
        };
    }
}
