package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OverlayFootprintPlanTest {
    @Test
    void distinctContributorBoundsRemainPartitionedInsteadOfBecomingOneCoarseCrop() {
        List<QuadSurface> regions = OverlayFootprintPlan.partition(List.of(
                surface(0, 16, 0, 4),
                surface(0, 16, 0, 8)));

        assertEquals(2, regions.size());
        assertTrue(regions.contains(surface(0, 16, 0, 4)));
        assertTrue(regions.contains(surface(0, 16, 4, 8)));
    }

    @Test
    void disjointStepAndQuarterPatchesDoNotFillTheirBoundingBoxGap() {
        List<QuadSurface> regions = OverlayFootprintPlan.partition(List.of(
                surface(0, 4, 0, 4),
                surface(12, 16, 8, 16)));

        assertEquals(2, regions.size());
        assertTrue(regions.contains(surface(0, 4, 0, 4)));
        assertTrue(regions.contains(surface(12, 16, 8, 16)));
    }

    @Test
    void ordinaryFullReceiverRemainsOneOriginalEquivalentRegion() {
        QuadSurface full = surface(0, 16, 0, 16);

        assertEquals(List.of(full), OverlayFootprintPlan.partition(List.of(full, full)));
        assertTrue(OverlayEmissionGeometry.originalUnitSquareMatches(full));
    }

    @Test
    void terrainNominalPlaneMovesOnlyPresentationDepthAndRetainsTheCrop() {
        QuadSurface physical = new QuadSurface(Direction.UP, 7, Direction.Axis.X,
                2, 14, Direction.Axis.Z, 3, 13);
        QuadSurface nominal = OverlayFootprintPlan.onPlane(physical, 8);

        assertEquals(8, nominal.plane16());
        assertEquals(2, nominal.uMin16());
        assertEquals(14, nominal.uMax16());
        assertEquals(3, nominal.vMin16());
        assertEquals(13, nominal.vMax16());
    }

    private static QuadSurface surface(int uMin, int uMax, int vMin, int vMax) {
        return new QuadSurface(Direction.NORTH, 0, Direction.Axis.X,
                uMin, uMax, Direction.Axis.Y, vMin, vMax);
    }
}
