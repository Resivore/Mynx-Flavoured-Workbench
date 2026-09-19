package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.continuity.ContinuityQuadContext.OverlaySpriteContribution;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exact Continuity 3.0.1 collector-index to successful-probe provenance identities. */
final class ContinuityOverlayContributionTest {
    private static final BlockPos RECEIVER = BlockPos.ZERO;
    // Continuity's fixed Standard Overlay order: left, down, right, up.
    private static final Direction[] DIRECTIONS = {
            Direction.WEST, Direction.DOWN, Direction.EAST, Direction.UP
    };

    @Test
    void oneShortLayerSideGivesTheSelectedEdgeOnlyItsShortFootprint() {
        QuadSurface shortLayer = surface(0, 16, 0, 4);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(shortLayer));

        capture.beginPendingSprites(9);
        capture.addPendingSprite(0, true);

        assertEquals(List.of(contribution(9, shortLayer)), capture.overlaySprites());
    }

    @Test
    void twoSideSpritesKeepIndependentFourAndEightHighProvenance() {
        QuadSurface shortLayer = surface(0, 16, 0, 4);
        QuadSurface bottomSlab = surface(0, 16, 0, 8);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(shortLayer));
        capture.addOverlayProbe(RECEIVER.east(), List.of(bottomSlab));

        capture.beginPendingSprites(9, 7);
        capture.addPendingSprite(0, true);
        capture.addPendingSprite(1, true);

        assertEquals(List.of(
                contribution(9, shortLayer),
                contribution(7, bottomSlab)), capture.overlaySprites());
    }

    @Test
    void sideAndCornerSpritesUseOnlyTheirOwnSuccessfulProbePositions() {
        QuadSurface side = surface(0, 16, 0, 4);
        QuadSurface diagonalCorner = surface(12, 16, 12, 16);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(side));
        capture.addOverlayProbe(RECEIVER.east().above(), List.of(diagonalCorner));

        capture.beginPendingSprites(9, 14);
        capture.addPendingSprite(0, true);
        capture.addPendingSprite(1, true);

        assertEquals(List.of(
                contribution(9, side),
                contribution(14, diagonalCorner)), capture.overlaySprites());
    }

    @Test
    void skippedNullCollectorSpriteDoesNotShiftTheFollowingContribution() {
        QuadSurface side = surface(0, 16, 0, 4);
        QuadSurface secondCorner = surface(12, 16, 12, 16);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(side));
        capture.addOverlayProbe(RECEIVER.east().above(), List.of(secondCorner));

        capture.beginPendingSprites(9, 0, 14);
        capture.addPendingSprite(0, true);
        capture.addPendingSprite(1, false);
        capture.addPendingSprite(2, true);

        assertEquals(List.of(
                contribution(9, side),
                contribution(14, secondCorner)), capture.overlaySprites());
    }

    @Test
    void combinedSpriteUsesExactlyItsRepresentedSuccessfulSideProbes() {
        QuadSurface left = surface(0, 16, 0, 4);
        QuadSurface down = surface(0, 8, 0, 16);
        QuadSurface unrelatedUp = surface(8, 16, 0, 16);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(left));
        capture.addOverlayProbe(RECEIVER.below(), List.of(down));
        capture.addOverlayProbe(RECEIVER.above(), List.of(unrelatedUp));

        capture.beginPendingSprites(4);
        capture.addPendingSprite(0, true);

        assertEquals(List.of(contribution(4, left, down)), capture.overlaySprites());
    }

    @Test
    void duplicateProbePatchesProduceOnlyOneRegionForOneLogicalContribution() {
        QuadSurface overlap = surface(0, 16, 0, 8);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(overlap, overlap));
        capture.addOverlayProbe(RECEIVER.below(), List.of(overlap));

        capture.beginPendingSprites(4);
        capture.addPendingSprite(0, true);

        List<QuadSurface> footprints = capture.overlaySprites().getFirst().footprints();
        assertEquals(List.of(overlap), footprints);
        assertEquals(List.of(overlap), OverlayFootprintPlan.partition(footprints));
    }

    @Test
    void fullFaceContributionRetainsNativeEquivalentSingleRegion() {
        QuadSurface full = surface(0, 16, 0, 16);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(full));

        capture.beginPendingSprites(9);
        capture.addPendingSprite(0, true);

        List<QuadSurface> regions = OverlayFootprintPlan.partition(
                capture.overlaySprites().getFirst().footprints());
        assertEquals(List.of(full), regions);
        assertTrue(OverlayEmissionGeometry.originalUnitSquareMatches(regions.getFirst()));
    }

    @Test
    void typedTerrainPresentationMovesOnlyThisContributionsPhysicalFootprint() {
        QuadSurface physical = new QuadSurface(Direction.UP, 7, Direction.Axis.X,
                2, 14, Direction.Axis.Z, 0, 8);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(physical));
        capture.beginPendingSprites(9);
        capture.addPendingSprite(0, true);

        QuadSurface retained = capture.overlaySprites().getFirst().footprints().getFirst();
        QuadSurface nominal = OverlayFootprintPlan.onPlane(retained, 8);
        assertEquals(8, nominal.plane16());
        assertEquals(retained.uMin16(), nominal.uMin16());
        assertEquals(retained.uMax16(), nominal.uMax16());
        assertEquals(retained.vMin16(), nominal.vMin16());
        assertEquals(retained.vMax16(), nominal.vMax16());
    }

    @Test
    void semanticNegativeCannotCreateSpriteProvenance() {
        ContinuityQuadContext.Capture capture = capture();
        capture.beginPendingSprites(9);
        capture.addPendingSprite(0, false);

        assertTrue(capture.overlaySprites().isEmpty());
        assertTrue(capture.nextOverlaySprite().isEmpty());
    }

    @Test
    void emissionCursorReturnsEachCollectorContributionExactlyOnce() {
        QuadSurface shortLayer = surface(0, 16, 0, 4);
        QuadSurface bottomSlab = surface(0, 16, 0, 8);
        ContinuityQuadContext.Capture capture = capture();
        capture.addOverlayProbe(RECEIVER.west(), List.of(shortLayer));
        capture.addOverlayProbe(RECEIVER.east(), List.of(bottomSlab));
        capture.beginPendingSprites(9, 7);
        capture.addPendingSprite(0, true);
        capture.addPendingSprite(1, true);

        assertEquals(contribution(9, shortLayer), capture.nextOverlaySprite().orElseThrow());
        assertEquals(contribution(7, bottomSlab), capture.nextOverlaySprite().orElseThrow());
        assertTrue(capture.nextOverlaySprite().isEmpty());
    }

    private static ContinuityQuadContext.Capture capture() {
        ContinuityQuadContext.Capture capture = new ContinuityQuadContext.Capture(
                surface(0, 16, 0, 16), null, RECEIVER);
        capture.beginOverlayAssembly(DIRECTIONS);
        return capture;
    }

    private static OverlaySpriteContribution contribution(int spriteIndex,
            QuadSurface... footprints) {
        return new OverlaySpriteContribution(spriteIndex, List.of(footprints));
    }

    private static QuadSurface surface(int uMin, int uMax, int vMin, int vMax) {
        return new QuadSurface(Direction.NORTH, 0, Direction.Axis.X,
                uMin, uMax, Direction.Axis.Y, vMin, vMax);
    }
}
