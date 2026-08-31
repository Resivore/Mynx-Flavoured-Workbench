package dev.aero.shulkertrowel.geometry;

import dev.aero.cnmterraincompat.BgeGeometryCatalog;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetGeometryCatalogTest {
    @Test
    void selectorOrderIsIndependentFromStableSavedAndNetworkIds() {
        List<String> expectedNames = List.of(
                "Full Block", "Slab", "Stair", "Wall", "Vertical Slab", "Step",
                "Corner", "Quarter Column", "Layer");
        List<Integer> expectedIds = List.of(0, 1, 2, 3, 4, 5, 7, 8, 6);
        List<TargetGeometry> ordered = TargetGeometry.ordered();

        assertEquals(expectedNames, ordered.stream().map(TargetGeometry::displayName).toList());
        assertEquals(expectedIds, ordered.stream().map(TargetGeometry::persistenceId).toList());
        for (int index = 0; index < ordered.size(); index++) {
            TargetGeometry geometry = ordered.get(index);
            assertEquals(index, geometry.selectorIndex());
            assertEquals(index, geometry.selectorOrder());
            assertSame(geometry, TargetGeometry.fromSelectorIndex(index).orElseThrow());
            assertSame(geometry, TargetGeometry.fromNetworkId(geometry.networkId()).orElseThrow());
        }
    }

    @Test
    void existingIdsZeroThroughSixRetainTheirExactIdentities() {
        List<TargetGeometry> expected = List.of(
                TargetGeometry.FULL,
                TargetGeometry.SLAB,
                TargetGeometry.STAIR,
                TargetGeometry.WALL,
                bge("vertical_slab"),
                bge("step"),
                bge("layer"));

        for (int id = 0; id <= 6; id++) {
            assertSame(expected.get(id), TargetGeometry.fromNetworkId(id).orElseThrow());
            assertEquals(id, expected.get(id).networkId());
        }
        assertEquals(8, expected.get(6).selectorIndex(), "Layer identity moved with presentation order");
    }

    @Test
    void everyBgeModeIsProjectedFromTheProviderCatalogDescriptor() {
        List<TargetGeometry> bgeModes = TargetGeometry.ordered().stream()
                .filter(mode -> mode.bgeDescriptor().isPresent())
                .toList();

        assertEquals(BgeGeometryCatalog.ordered().size(), bgeModes.size());
        for (int index = 0; index < bgeModes.size(); index++) {
            TargetGeometry mode = bgeModes.get(index);
            BgeGeometryCatalog.Descriptor descriptor = BgeGeometryCatalog.ordered().get(index);
            assertSame(descriptor, mode.bgeDescriptor().orElseThrow());
            assertSame(descriptor, BgeGeometryCatalog.byPersistenceId(
                    descriptor.persistenceId()).orElseThrow());
            assertSame(descriptor, BgeGeometryCatalog.byKey(descriptor.key()).orElseThrow());
            assertEquals(descriptor.key(), mode.key());
            assertEquals(descriptor.persistenceId(), mode.persistenceId());
            assertEquals(descriptor.selectorOrder(), mode.selectorOrder());
            assertEquals(descriptor.displayName(), mode.displayName());
            assertTrue(mode.nativeRole().isEmpty());
        }
    }

    @Test
    void newModesHaveStableKeyAndIntegerIdentityWithoutMovingLayer() {
        TargetGeometry corner = bge("corner");
        TargetGeometry quarterColumn = bge("quarter_column");
        TargetGeometry layer = bge("layer");

        assertEquals(7, corner.persistenceId());
        assertEquals(8, quarterColumn.persistenceId());
        assertEquals(6, layer.persistenceId());
        assertEquals(List.of(corner, quarterColumn, layer), TargetGeometry.ordered().subList(6, 9));
    }

    @Test
    void invalidSelectorIdsAndKeysRemainSafe() {
        for (int invalid : new int[]{-1, Integer.MAX_VALUE}) {
            assertTrue(TargetGeometry.fromSelectorIndex(invalid).isEmpty());
            assertTrue(TargetGeometry.fromNetworkId(invalid).isEmpty());
            assertSame(TargetGeometry.FULL, TargetGeometry.byNetworkId(invalid));
        }
        assertTrue(TargetGeometry.fromSelectorIndex(TargetGeometry.ordered().size()).isEmpty());
        assertTrue(TargetGeometry.byKey(
                Identifier.fromNamespaceAndPath("shulker_trowel", "unknown_geometry")).isEmpty());
    }

    private static TargetGeometry bge(String path) {
        return TargetGeometry.byKey(Identifier.fromNamespaceAndPath(
                "cnm_terrain_slabs_compat", path)).orElseThrow();
    }
}
