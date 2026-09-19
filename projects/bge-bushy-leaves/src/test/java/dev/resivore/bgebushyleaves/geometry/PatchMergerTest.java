package dev.resivore.bgebushyleaves.geometry;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PatchMergerTest {
    @Test void rectangleSubtractionDoesNotDuplicateCompoundSurfaceCells() {
        Rect16 surface = new Rect16(0, 16, 0, 16);
        List<Rect16> visible = RectSubtraction.subtract(surface, List.of(new Rect16(0, 8, 0, 8), new Rect16(0, 8, 0, 8)));
        assertEquals(List.of(new Rect16(8, 16, 0, 8), new Rect16(0, 8, 8, 16), new Rect16(8, 16, 8, 16)), visible);
    }
    @Test void resolvedTopologyFixturesKeepOnlyOccupiedBounds() {
        // Stair tread, Wall arm, Layer, Corner and diagonal Quarter Column footprints are deliberately not a full cube.
        List<Rect16> occupied = List.of(new Rect16(0, 16, 0, 8), new Rect16(0, 6, 8, 14),
                new Rect16(0, 16, 0, 4), new Rect16(0, 8, 0, 16), new Rect16(8, 16, 8, 16));
        assertTrue(occupied.stream().allMatch(rect -> rect.uMax() - rect.uMin() < 16 || rect.vMax() - rect.vMin() < 16));
        assertFalse(occupied.contains(new Rect16(0, 16, 0, 16)));
    }
    @Test void patchFrameRejectsAFrameThatCannotDescribeABlockSurface() {
        assertThrows(IllegalArgumentException.class, () -> new PatchFrame(Direction.NORTH, 0,
                Direction.Axis.Z, new Rect16(0, 16, 0, 16), Direction.Axis.Y, Direction.NORTH));
    }
}
