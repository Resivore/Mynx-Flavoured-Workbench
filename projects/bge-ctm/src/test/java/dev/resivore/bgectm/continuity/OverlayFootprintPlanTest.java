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
    void duplicateRegionsWithinOneLogicalContributionEmitOnlyOneClippedCell() {
        QuadSurface clipped = surface(0, 16, 0, 4);

        assertEquals(List.of(clipped), OverlayFootprintPlan.partition(
                List.of(clipped, clipped, clipped)));
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

    @Test
    void receiverAndDifferentlySizedLogicalOverlaysShareIdenticalOverlapCells() {
        QuadSurface receiver = surface(0, 16, 0, 16);
        QuadSurface shortOverlay = surface(0, 16, 0, 4);
        QuadSurface tallOverlay = surface(0, 16, 0, 8);

        List<QuadSurface> cells = OverlayFootprintPlan.tessellateReceiver(
                receiver, List.of(shortOverlay, tallOverlay));
        List<QuadSurface> shortCells = OverlayFootprintPlan.coveredCells(
                cells, List.of(shortOverlay));
        List<QuadSurface> tallCells = OverlayFootprintPlan.coveredCells(
                cells, List.of(tallOverlay));

        assertEquals(List.of(
                surface(0, 16, 0, 4),
                surface(0, 16, 4, 8),
                surface(0, 16, 8, 16)), cells);
        assertEquals(List.of(surface(0, 16, 0, 4)), shortCells);
        assertEquals(List.of(surface(0, 16, 0, 4), surface(0, 16, 4, 8)), tallCells);
        assertEquals(shortCells.getFirst(), tallCells.getFirst(),
                "Intentional Continuity overlap must reuse the exact same triangle cell");
    }

    @Test
    void nominalPlaneLogicalOverlaysShareCellsWithoutMovingToThePhysicalPlane() {
        QuadSurface shortOverlay = new QuadSurface(Direction.UP, 8, Direction.Axis.X,
                0, 16, Direction.Axis.Z, 0, 4);
        QuadSurface tallOverlay = new QuadSurface(Direction.UP, 8, Direction.Axis.X,
                0, 16, Direction.Axis.Z, 0, 8);

        List<QuadSurface> cells = OverlayFootprintPlan.tessellateUnion(
                List.of(shortOverlay, tallOverlay));

        assertEquals(2, cells.size());
        assertTrue(cells.stream().allMatch(cell -> cell.plane16() == 8));
        assertEquals(OverlayFootprintPlan.coveredCells(cells, List.of(shortOverlay)).getFirst(),
                OverlayFootprintPlan.coveredCells(cells, List.of(tallOverlay)).getFirst());
    }

    @Test
    void distinctDisjointLogicalContributionsDoNotAcquireOverlappingCells() {
        QuadSurface receiver = surface(0, 16, 0, 16);
        QuadSurface left = surface(0, 8, 0, 8);
        QuadSurface right = surface(8, 16, 8, 16);
        List<QuadSurface> cells = OverlayFootprintPlan.tessellateReceiver(
                receiver, List.of(left, right));
        List<QuadSurface> leftCells = OverlayFootprintPlan.coveredCells(cells, List.of(left));
        List<QuadSurface> rightCells = OverlayFootprintPlan.coveredCells(cells, List.of(right));

        assertTrue(leftCells.stream().noneMatch(rightCells::contains),
                "Shared tessellation must not manufacture cross-contribution overlap");
    }

    private static QuadSurface surface(int uMin, int uMax, int vMin, int vMax) {
        return new QuadSurface(Direction.NORTH, 0, Direction.Axis.X,
                uMin, uMax, Direction.Axis.Y, vMin, vMax);
    }
}
