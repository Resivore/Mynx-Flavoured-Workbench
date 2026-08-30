package dev.aero.shulkertrowel.geometry;

import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetGeometryCatalogTest {
    @Test
    void legacyNetworkIdsStayStableAndLayerIsAppended() {
        List<TargetGeometry> expected = List.of(
                TargetGeometry.FULL,
                TargetGeometry.SLAB,
                TargetGeometry.STAIR,
                TargetGeometry.WALL,
                TargetGeometry.VERTICAL_SLAB,
                TargetGeometry.STEP,
                TargetGeometry.LAYER
        );

        assertEquals(expected, TargetGeometry.ordered());
        for (int index = 0; index < expected.size(); index++) {
            TargetGeometry geometry = expected.get(index);
            assertEquals(index, geometry.selectorIndex());
            assertEquals(index, geometry.networkId());
            assertEquals(geometry, TargetGeometry.fromSelectorIndex(index).orElseThrow());
            assertEquals(geometry, TargetGeometry.fromNetworkId(index).orElseThrow());
        }
    }

    @Test
    void invalidSelectorAndNetworkIdsRemainSafe() {
        for (int invalid : new int[]{-1, 7, Integer.MAX_VALUE}) {
            assertTrue(TargetGeometry.fromSelectorIndex(invalid).isEmpty());
            assertTrue(TargetGeometry.fromNetworkId(invalid).isEmpty());
            assertEquals(TargetGeometry.FULL, TargetGeometry.byNetworkId(invalid));
        }
    }

    @Test
    void providerOwnedModesCarryOnlyTypedDerivedGeometry() {
        assertEquals(
                DerivedGeometrySupport.Geometry.VERTICAL_SLAB,
                TargetGeometry.VERTICAL_SLAB.derivedGeometry().orElseThrow()
        );
        assertEquals(
                DerivedGeometrySupport.Geometry.STEP,
                TargetGeometry.STEP.derivedGeometry().orElseThrow()
        );
        assertEquals(
                DerivedGeometrySupport.Geometry.LAYER,
                TargetGeometry.LAYER.derivedGeometry().orElseThrow()
        );
        assertTrue(TargetGeometry.ordered().subList(4, 7).stream()
                .allMatch(geometry -> geometry.nativeRole().isEmpty()
                        && geometry.derivedGeometry().isPresent()));
    }
}
