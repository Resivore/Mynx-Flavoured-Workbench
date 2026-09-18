package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.resivore.bgectm.SurfaceContactResolver.Interval;
import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import dev.resivore.bgectm.SurfaceContactResolver.SurfaceDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Behavioral identities for material-agnostic inducing-surface projection. */
final class SurfaceContactResolverFootprintTest {
    private static final BlockPos RECEIVER = BlockPos.ZERO;
    private static final BlockPos EAST = RECEIVER.east();

    @Test
    void shortLayerProjectsOnlyItsLowerBandOntoATallerReceiver() {
        QuadSurface footprint = project(
                surface(RECEIVER, 0, 16, 0, 8),
                surface(EAST, 0, 16, 0, 4));

        assertBounds(footprint, 0, 16, 0, 4);
    }

    @Test
    void bottomAndTopSlabsProjectToTheirCorrespondingHalves() {
        SurfaceDescriptor receiver = surface(RECEIVER, 0, 16, 0, 16);

        assertBounds(project(receiver, surface(EAST, 0, 16, 0, 8)),
                0, 16, 0, 8);
        assertBounds(project(receiver, surface(EAST, 0, 16, 8, 16)),
                0, 16, 8, 16);
    }

    @Test
    void tallerSourceIsNaturallyClippedByTheShorterReceiver() {
        QuadSurface footprint = project(
                surface(RECEIVER, 0, 16, 0, 8),
                surface(EAST, 0, 16, 0, 16));

        assertBounds(footprint, 0, 16, 0, 8);
    }

    @Test
    void quarterAndDiagonalCornerExtentsReflectAcrossContactedCellEdges() {
        assertBounds(project(
                surface(RECEIVER, 0, 16, 0, 16),
                surface(EAST, 0, 4, 0, 16)),
                12, 16, 0, 16);

        BlockPos eastUp = EAST.above();
        assertBounds(project(
                surface(RECEIVER, 0, 16, 0, 16),
                surface(eastUp, 0, 4, 0, 4)),
                12, 16, 12, 16);
    }

    @Test
    void canonicalSemanticMismatchCannotManufactureAFootprint() {
        SurfaceDescriptor receiver = surface(RECEIVER, 0, 16, 0, 16);
        SurfaceDescriptor wrongCanonicalFace = new SurfaceDescriptor(Direction.NORTH, 0,
                Direction.Axis.X, new Interval(16, 32), Direction.Axis.Y,
                new Interval(0, 16), EAST, Direction.SOUTH, PlaneRelation.EXACT);

        assertTrue(SurfaceContactResolver.projectContactFootprint(
                receiver, wrongCanonicalFace).isEmpty());
    }

    @Test
    void ordinaryFullFaceProjectionRemainsTheFullReceiver() {
        assertBounds(project(
                surface(RECEIVER, 0, 16, 0, 16),
                surface(EAST, 0, 16, 0, 16)),
                0, 16, 0, 16);
    }

    private static SurfaceDescriptor surface(BlockPos pos, int uMin, int uMax,
            int vMin, int vMax) {
        long xOrigin = (long) pos.getX() * 16;
        long yOrigin = (long) pos.getY() * 16;
        long zPlane = (long) pos.getZ() * 16;
        return new SurfaceDescriptor(Direction.NORTH, zPlane, Direction.Axis.X,
                new Interval(xOrigin + uMin, xOrigin + uMax), Direction.Axis.Y,
                new Interval(yOrigin + vMin, yOrigin + vMax), pos, Direction.NORTH,
                PlaneRelation.EXACT);
    }

    private static QuadSurface project(SurfaceDescriptor receiver, SurfaceDescriptor source) {
        Optional<QuadSurface> result =
                SurfaceContactResolver.projectContactFootprint(receiver, source);
        assertTrue(result.isPresent(), "Expected authoritative projected contact");
        return result.get();
    }

    private static void assertBounds(QuadSurface footprint, int uMin, int uMax,
            int vMin, int vMax) {
        assertEquals(uMin, footprint.uMin16());
        assertEquals(uMax, footprint.uMax16());
        assertEquals(vMin, footprint.vMin16());
        assertEquals(vMax, footprint.vMax16());
    }
}
