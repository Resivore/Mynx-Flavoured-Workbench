package dev.resivore.bgebushyleaves.geometry;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic BGE surface-contract fixtures exercise volume composition without provider assets. */
final class BlockSpaceFoliagePlanTest {
    @Test void fullLeafGetsOneStableWorldAxisMotifRatherThanFaceMotifs() {
        BlockSpaceOccupancy occupancy = occupancy(box(0, 0, 0, 16, 16, 16));
        List<BlockSpaceFoliagePlan.Card> cards = BlockSpaceFoliagePlan.plan(occupancy, 11L);
        assertEquals(4, cards.size());
        assertEquals(signature(cards), signature(BlockSpaceFoliagePlan.plan(occupancy, 11L)));
        assertTrue(cards.stream().allMatch(card -> card.supportedCells() >= 16));
        // A vertical world-space plane has no normal/plane/U/V identity from any source patch.
        assertTrue(cards.stream().flatMap(card -> Arrays.stream(card.vertices()))
                .allMatch(vertex -> vertex.y16() >= -1.0F && vertex.y16() <= 17.0F));
    }

    @Test void slabAndLayerAdaptTheSameMotifToTheirOccupiedHeight() {
        for (int height : List.of(4, 8, 12)) {
            BlockSpaceOccupancy occupancy = occupancy(box(0, 0, 0, 16, height, 16));
            assertEquals(height, occupancy.bounds().maxY());
            assertFalse(BlockSpaceFoliagePlan.plan(occupancy, 1L).isEmpty());
            assertTrue(BlockSpaceFoliagePlan.plan(occupancy, 1L).stream().flatMap(card -> Arrays.stream(card.vertices()))
                    .allMatch(vertex -> vertex.y16() <= height + 1.0F));
        }
    }

    @Test void stairWallAndCornerUseOneBlockSpaceCoordinateSystemAcrossTheirCuboids() {
        BlockSpaceOccupancy stair = occupancy(box(0, 0, 0, 16, 8, 16), box(0, 8, 0, 8, 16, 16));
        BlockSpaceOccupancy wall = occupancy(box(4, 0, 4, 12, 16, 12), box(5, 0, 0, 11, 14, 8));
        BlockSpaceOccupancy corner = occupancy(box(0, 0, 0, 16, 16, 8), box(0, 0, 8, 8, 16, 16));
        for (BlockSpaceOccupancy form : List.of(stair, wall, corner)) {
            List<BlockSpaceFoliagePlan.Card> cards = BlockSpaceFoliagePlan.plan(form, 0x5EEDL);
            assertFalse(cards.isEmpty());
            assertTrue(cards.stream().allMatch(card -> card.supportedCells() > 0));
            assertEquals(signature(cards), signature(BlockSpaceFoliagePlan.plan(form, 0x5EEDL)));
        }
    }

    @Test void verticalAndDiagonalQuarterOccupanciesNeverBecomeAWholeBlockFallback() {
        BlockSpaceOccupancy vertical = occupancy(box(0, 0, 0, 8, 16, 16));
        BlockSpaceOccupancy diagonal = occupancy(box(0, 0, 0, 8, 16, 8), box(8, 0, 8, 16, 16, 16));
        assertEquals(8, vertical.bounds().maxX());
        List<BlockSpaceFoliagePlan.Card> diagonalCards = BlockSpaceFoliagePlan.plan(diagonal, 9L);
        assertFalse(diagonalCards.isEmpty());
        // A full cube gives each template a 16-high continuous panel. The diagonal's empty
        // quadrants split or reduce its support instead of silently substituting a cube motif.
        assertTrue(diagonalCards.stream().anyMatch(card -> card.supportedCells() < 16));
    }

    @Test void absentOrUnsupportedSurfaceDataProducesNoInventedVolume() {
        assertTrue(BlockSpaceOccupancy.fromSurfacePatches(List.of()).isEmpty());
        assertTrue(BlockSpaceOccupancy.fromSurfacePatches(List.of(
                new SurfacePatch(Direction.EAST, 16, Direction.Axis.Y, 0, 16, Direction.Axis.Z, 0, 16,
                        Direction.EAST, PlaneRelation.EXACT))).isEmpty());
    }

    private static BlockSpaceOccupancy occupancy(int[]... cuboids) {
        List<SurfacePatch> xBoundaries = new ArrayList<>();
        for (int[] cuboid : cuboids) {
            xBoundaries.add(west(cuboid[0], cuboid[1], cuboid[2], cuboid[4], cuboid[5]));
            xBoundaries.add(east(cuboid[3], cuboid[1], cuboid[2], cuboid[4], cuboid[5]));
        }
        return BlockSpaceOccupancy.fromSurfacePatches(xBoundaries).orElseThrow();
    }

    private static int[] box(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new int[] {x0, y0, z0, x1, y1, z1};
    }

    private static SurfacePatch west(int x, int y0, int z0, int y1, int z1) {
        return new SurfacePatch(Direction.WEST, x, Direction.Axis.Y, y0, y1, Direction.Axis.Z, z0, z1,
                Direction.WEST, PlaneRelation.EXACT);
    }

    private static SurfacePatch east(int x, int y0, int z0, int y1, int z1) {
        return new SurfacePatch(Direction.EAST, x, Direction.Axis.Y, y0, y1, Direction.Axis.Z, z0, z1,
                Direction.EAST, PlaneRelation.EXACT);
    }

    private static String signature(List<BlockSpaceFoliagePlan.Card> cards) {
        return cards.stream().map(card -> card.ordinal() + ":" + card.supportedCells() + Arrays.toString(card.vertices()))
                .toList().toString();
    }
}
