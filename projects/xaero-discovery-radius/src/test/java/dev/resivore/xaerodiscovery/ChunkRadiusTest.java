package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChunkRadiusTest {
    @Test
    void usesAnInclusiveChebyshevSquare() {
        assertTrue(ChunkRadius.contains(10, -10, 14, -6, 4));
        assertTrue(ChunkRadius.contains(10, -10, 6, -14, 4));
        assertFalse(ChunkRadius.contains(10, -10, 15, -10, 4));
        assertFalse(ChunkRadius.contains(10, -10, 10, -15, 4));
    }

    @Test
    void handlesNegativeCoordinatesAndRejectsNegativeRadius() {
        assertTrue(ChunkRadius.contains(-8, -8, -12, -4, 4));
        assertFalse(ChunkRadius.contains(-8, -8, -13, -4, 4));
        assertFalse(ChunkRadius.contains(0, 0, 0, 0, -1));
    }

    @Test
    void packedCoordinatesRoundTripAcrossSignedIntRange() {
        int[][] coordinates = {
                {0, 0},
                {-1, -1},
                {Integer.MIN_VALUE, Integer.MAX_VALUE},
                {Integer.MAX_VALUE, Integer.MIN_VALUE}
        };
        for (int[] coordinate : coordinates) {
            long packed = ChunkRadius.pack(coordinate[0], coordinate[1]);
            assertEquals(coordinate[0], ChunkRadius.unpackX(packed));
            assertEquals(coordinate[1], ChunkRadius.unpackZ(packed));
        }
    }
}
