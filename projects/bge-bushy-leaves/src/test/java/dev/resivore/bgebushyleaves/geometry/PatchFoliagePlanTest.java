package dev.resivore.bgebushyleaves.geometry;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic BGE-patch fixtures prove the generic path without importing provider assets/models. */
final class PatchFoliagePlanTest {
    private static final PatchFrame FULL_TOP = frame(Direction.UP, 16, 0, 16, 0, 16);

    @Test void fullSixteenBySixteenPatchGetsCrossedAuthoredCards() {
        List<PatchFoliagePlan.Card> cards = PatchFoliagePlan.plan(FULL_TOP, 11L);
        assertEquals(2, cards.size());
        assertTrue(cards.stream().allMatch(card -> card.support().equals(new Rect16(0, 16, 0, 16))));
        assertTrue(cards.stream().flatMap(card -> Arrays.stream(card.vertices()))
                .anyMatch(vertex -> vertex.outward16() > 1.0F));
    }

    @Test void deterministicPatchLocalOrientationDoesNotFlicker() {
        assertEquals(signature(PatchFoliagePlan.plan(FULL_TOP, 0x5EEDL)),
                signature(PatchFoliagePlan.plan(FULL_TOP, 0x5EEDL)));
        assertNotEquals(signature(PatchFoliagePlan.plan(FULL_TOP, 1L)),
                signature(PatchFoliagePlan.plan(FULL_TOP, 2L)));
    }

    @Test void bottomAndTopSlabPatchesStayOnTheirActualPlanes() {
        List<PatchFrame> frames = PatchMerger.mergeSurfacePatches(List.of(
                patch(Direction.UP, 8, 0, 16, 0, 16), patch(Direction.DOWN, 0, 0, 16, 0, 16),
                patch(Direction.UP, 16, 0, 16, 0, 16), patch(Direction.DOWN, 8, 0, 16, 0, 16)));
        assertEquals(List.of(0, 8, 8, 16), frames.stream().map(PatchFrame::plane16).sorted().toList());
        assertAllSupported(frames);
    }

    @Test void resolvedStairStraightInnerOuterAndHalfFixturesUseOnlyTheirPatches() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int halfPlane : List.of(8, 16)) {
                for (int member : List.of(0, 1, 2)) {
                    List<PatchFrame> frames = PatchMerger.mergeSurfacePatches(List.of(
                            patch(Direction.UP, halfPlane, 0, 16, 0, 8 + member * 4),
                            patch(facing, 16, 0, 8 + member * 4, 0, halfPlane),
                            patch(facing.getOpposite(), 0, 0, 8 + member * 4, 0, halfPlane)));
                    assertAllSupported(frames);
                }
            }
        }
    }

    @Test void wallPostLowTallAndCompoundFixturesDoNotCreateCubeTufts() {
        List<PatchFrame> frames = PatchMerger.mergeSurfacePatches(List.of(
                patch(Direction.UP, 16, 4, 12, 4, 12), // post
                patch(Direction.NORTH, 0, 5, 11, 0, 14), // low arm
                patch(Direction.EAST, 16, 5, 11, 0, 16), // tall arm
                patch(Direction.SOUTH, 16, 5, 11, 0, 14)));
        assertTrue(frames.stream().noneMatch(frame -> frame.bounds().equals(new Rect16(0, 16, 0, 16))));
        assertAllSupported(frames);
    }

    @Test void verticalSlabFixtureKeepsItsOccupiedHalf() {
        PatchFrame northHalf = frame(Direction.UP, 16, 0, 16, 0, 8);
        assertAllSupported(List.of(northHalf));
        assertEquals(new Rect16(0, 16, 0, 8), PatchFoliagePlan.plan(northHalf, 9L).getFirst().support());
    }

    @Test void singleAndDoubleStepFixturesRetainOnlyDeclaredMembers() {
        List<PatchFrame> frames = PatchMerger.mergeSurfacePatches(List.of(
                patch(Direction.UP, 8, 0, 16, 0, 8), patch(Direction.UP, 16, 0, 16, 8, 16)));
        assertAllSupported(frames);
        assertEquals(2, frames.size());
    }

    @Test void quarterLayersUseTheirActualThicknessAtOneTwoAndThreeQuarters() {
        for (int thickness : List.of(4, 8, 12)) {
            PatchFrame layer = frame(Direction.UP, thickness, 0, 16, 0, 16);
            assertAllSupported(List.of(layer));
            assertEquals(thickness, layer.plane16());
        }
    }

    @Test void eachCornerRotationFixtureKeepsItsOccupiedTopFootprint() {
        for (Rect16 bounds : List.of(new Rect16(0, 8, 0, 16), new Rect16(8, 16, 0, 16),
                new Rect16(0, 16, 0, 8), new Rect16(0, 16, 8, 16))) {
            assertAllSupported(List.of(new PatchFrame(Direction.UP, 16, Direction.Axis.X, bounds,
                    Direction.Axis.Z, Direction.UP)));
        }
    }

    @Test void everyQuarterColumnOccupancyFixtureStaysInItsQuarter() {
        List<Rect16> quarters = List.of(new Rect16(0, 8, 0, 8), new Rect16(8, 16, 0, 8),
                new Rect16(0, 8, 8, 16), new Rect16(8, 16, 8, 16));
        for (Rect16 occupied : quarters) assertAllSupported(List.of(new PatchFrame(Direction.UP, 16,
                Direction.Axis.X, occupied, Direction.Axis.Z, Direction.UP)));
        // The two diagonal BGE occupancies are represented by two independently supported patches.
        assertAllSupported(List.of(new PatchFrame(Direction.UP, 16, Direction.Axis.X, quarters.getFirst(),
                Direction.Axis.Z, Direction.UP), new PatchFrame(Direction.UP, 16, Direction.Axis.X,
                quarters.getLast(), Direction.Axis.Z, Direction.UP)));
    }

    @Test void smallPatchScalesAndTinyPatchOmitsByOneGenericRule() {
        PatchFrame scaled = frame(Direction.UP, 16, 0, 2, 0, 4);
        assertFalse(PatchFoliagePlan.plan(scaled, 4L).isEmpty());
        assertTrue(PatchFoliagePlan.plan(frame(Direction.UP, 16, 0, 1, 0, 4), 4L).isEmpty());
    }

    @Test void noCardClaimsSupportOverAnEmptyBlockspaceRegion() {
        PatchFrame narrow = frame(Direction.NORTH, 0, 0, 6, 0, 14);
        for (PatchFoliagePlan.Card card : PatchFoliagePlan.plan(narrow, 2L)) {
            assertEquals(narrow.bounds(), card.support());
            assertFalse(card.support().equals(new Rect16(0, 16, 0, 16)));
        }
    }

    @Test void coplanarTilesMergeBeforeMotifPlanning() {
        List<PatchFrame> merged = PatchMerger.mergeSurfacePatches(List.of(
                patch(Direction.UP, 16, 0, 8, 0, 8), patch(Direction.UP, 16, 8, 16, 0, 8),
                patch(Direction.UP, 16, 0, 8, 8, 16), patch(Direction.UP, 16, 8, 16, 8, 16)));
        assertEquals(List.of(FULL_TOP), merged);
        assertEquals(2, PatchFoliagePlan.plan(merged.getFirst(), 3L).size());
    }

    private static void assertAllSupported(List<PatchFrame> frames) {
        for (PatchFrame frame : frames) for (PatchFoliagePlan.Card card : PatchFoliagePlan.plan(frame, 37L)) {
            assertEquals(frame.bounds(), card.support());
            assertTrue(card.support().uMin() >= frame.bounds().uMin());
            assertTrue(card.support().uMax() <= frame.bounds().uMax());
            assertTrue(card.support().vMin() >= frame.bounds().vMin());
            assertTrue(card.support().vMax() <= frame.bounds().vMax());
        }
    }

    private static String signature(List<PatchFoliagePlan.Card> cards) {
        return cards.stream().map(card -> card.ordinal() + Arrays.toString(card.vertices())).toList().toString();
    }
    private static PatchFrame frame(Direction normal, int plane, int u0, int u1, int v0, int v1) {
        Direction.Axis[] axes = axes(normal);
        return new PatchFrame(normal, plane, axes[0], new Rect16(u0, u1, v0, v1), axes[1], normal);
    }
    private static SurfacePatch patch(Direction normal, int plane, int u0, int u1, int v0, int v1) {
        Direction.Axis[] axes = axes(normal);
        return new SurfacePatch(normal, plane, axes[0], u0, u1, axes[1], v0, v1, normal, PlaneRelation.EXACT);
    }
    private static Direction.Axis[] axes(Direction normal) {
        return switch (normal.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }
}
