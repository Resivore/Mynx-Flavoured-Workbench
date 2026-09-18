package dev.resivore.bgectm.continuity;

import dev.resivore.bgectm.SurfaceContactResolver.QuadSurface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Controlled C6 regression coverage for the coordinates consumed by QuadEmitter.square. */
final class OverlayEmissionGeometryTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void bottomSlabUpProjectsTheAcceptedOverlayToPlaneEight() {
        QuadSurface slabTop = surface(Direction.UP, 8, 0, 16, 0, 16);
        var projected = OverlayEmissionGeometry.project(slabTop);

        assertFalse(OverlayEmissionGeometry.originalUnitSquareMatches(slabTop));
        assertPosition(projected.vertex(0), 0, .5F, 0);
        assertPosition(projected.vertex(2), 1, .5F, 1);
        assertEquals(0, projected.uvU(0), EPSILON);
        assertEquals(0, projected.uvV(0), EPSILON);
        assertEquals(1, projected.uvU(2), EPSILON);
        assertEquals(1, projected.uvV(2), EPSILON);
    }

    @Test
    void typedTerrainPresentationUsesNominalPlaneWithoutChangingItsCrop() {
        QuadSurface farmlandSlabPresentation = surface(Direction.UP, 8, 2, 14, 3, 13);
        var projected = OverlayEmissionGeometry.project(farmlandSlabPresentation);

        assertPosition(projected.vertex(0), .125F, .5F, .1875F);
        assertPosition(projected.vertex(2), .875F, .5F, .8125F);
        assertEquals(.125F, projected.uvU(0), EPSILON);
        assertEquals(.1875F, projected.uvV(0), EPSILON);
        assertEquals(.875F, projected.uvU(2), EPSILON);
        assertEquals(.8125F, projected.uvV(2), EPSILON);
    }

    @Test
    void fullAndTopSlabUpRemainTheOriginalUnitFace() {
        assertTrue(OverlayEmissionGeometry.originalUnitSquareMatches(
                surface(Direction.UP, 16, 0, 16, 0, 16)));
        assertTrue(OverlayEmissionGeometry.originalUnitSquareMatches(
                surface(Direction.UP, 16, 0, 16, 0, 16)),
                "A top slab's UP surface is geometry-equivalent to Continuity's original emitter");
    }

    @Test
    void slabSideGeometryAndUvsAreCroppedRatherThanStretched() {
        var bottom = OverlayEmissionGeometry.project(surface(Direction.NORTH, 0, 0, 16, 0, 8));
        var top = OverlayEmissionGeometry.project(surface(Direction.NORTH, 0, 0, 16, 8, 16));

        assertPosition(bottom.vertex(0), 1, .5F, 0);
        assertPosition(bottom.vertex(2), 0, 0, 0);
        assertEquals(.5F, bottom.uvV(0), EPSILON);
        assertEquals(1, bottom.uvV(2), EPSILON);

        assertPosition(top.vertex(0), 1, 1, 0);
        assertPosition(top.vertex(2), 0, .5F, 0);
        assertEquals(0, top.uvV(0), EPSILON);
        assertEquals(.5F, top.uvV(2), EPSILON);
    }

    @Test
    void layerAndVerticalSlabRegionsKeepTheirActualPlaneAndTextureSubregion() {
        var recessedLayer = OverlayEmissionGeometry.project(surface(Direction.UP, 12, 0, 16, 0, 16));
        assertPosition(recessedLayer.vertex(0), 0, .75F, 0);
        assertPosition(recessedLayer.vertex(2), 1, .75F, 1);
        assertEquals(0, recessedLayer.uvV(0), EPSILON);
        assertEquals(1, recessedLayer.uvV(2), EPSILON);

        var verticalSlab = OverlayEmissionGeometry.project(surface(Direction.UP, 16, 0, 8, 0, 16));
        assertPosition(verticalSlab.vertex(0), 0, 1, 0);
        assertPosition(verticalSlab.vertex(2), .5F, 1, 1);
        assertEquals(0, verticalSlab.uvU(0), EPSILON);
        assertEquals(.5F, verticalSlab.uvU(2), EPSILON);
    }

    @Test
    void everyFaceRetainsItsExpectedOutwardWindingAndBounds() {
        for (Direction face : Direction.values()) {
            QuadSurface surface = switch (face) {
                case UP -> surface(face, 8, 2, 14, 4, 12);
                case DOWN -> surface(face, 8, 2, 14, 4, 12);
                case NORTH, SOUTH -> surface(face, face == Direction.NORTH ? 0 : 16,
                        2, 14, 4, 12);
                case WEST, EAST -> surface(face, face == Direction.WEST ? 0 : 16,
                        4, 12, 2, 14);
            };
            var projected = OverlayEmissionGeometry.project(surface);
            assertNormal(projected, face);
            for (int vertex = 0; vertex < 4; vertex++) {
                var position = projected.vertex(vertex);
                assertEquals(surface.plane16() / 16.0F, coordinate(position, face.getAxis()), EPSILON);
                assertTrue(projected.uvU(vertex) >= 0 && projected.uvU(vertex) <= 1);
                assertTrue(projected.uvV(vertex) >= 0 && projected.uvV(vertex) <= 1);
            }
        }
    }

    private static QuadSurface surface(Direction face, int plane, int uMin, int uMax,
            int vMin, int vMax) {
        Direction.Axis[] axes = switch (face.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
        return new QuadSurface(face, plane, axes[0], uMin, uMax, axes[1], vMin, vMax);
    }

    private static void assertNormal(OverlayEmissionGeometry.Projection projection, Direction face) {
        var p0 = projection.vertex(0);
        var p1 = projection.vertex(1);
        var p2 = projection.vertex(2);
        float ax = p1.x() - p0.x();
        float ay = p1.y() - p0.y();
        float az = p1.z() - p0.z();
        float bx = p2.x() - p1.x();
        float by = p2.y() - p1.y();
        float bz = p2.z() - p1.z();
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        assertTrue(nx * face.getStepX() + ny * face.getStepY() + nz * face.getStepZ() > 0,
                () -> "wrong winding for " + face);
    }

    private static float coordinate(OverlayEmissionGeometry.Position position, Direction.Axis axis) {
        return switch (axis) {
            case X -> position.x();
            case Y -> position.y();
            case Z -> position.z();
        };
    }

    private static void assertPosition(OverlayEmissionGeometry.Position actual,
            float x, float y, float z) {
        assertEquals(x, actual.x(), EPSILON);
        assertEquals(y, actual.y(), EPSILON);
        assertEquals(z, actual.z(), EPSILON);
    }
}
