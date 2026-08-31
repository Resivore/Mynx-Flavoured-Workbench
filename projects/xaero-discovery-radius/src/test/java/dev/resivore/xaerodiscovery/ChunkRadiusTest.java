package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChunkRadiusTest {
    @Test
    void radiusTwoIsAnExactThirteenChunkCircle() {
        Set<Long> included = new HashSet<>();
        for (int deltaX = -2; deltaX <= 2; deltaX++) {
            for (int deltaZ = -2; deltaZ <= 2; deltaZ++) {
                if (ChunkRadius.contains(0, 0, deltaX, deltaZ, 2)) {
                    included.add(ChunkRadius.pack(deltaX, deltaZ));
                }
            }
        }

        assertEquals(13, included.size());
        assertTrue(included.contains(ChunkRadius.pack(-2, 0)));
        assertTrue(included.contains(ChunkRadius.pack(2, 0)));
        assertTrue(included.contains(ChunkRadius.pack(0, -2)));
        assertTrue(included.contains(ChunkRadius.pack(0, 2)));
        assertTrue(included.contains(ChunkRadius.pack(-1, -1)));
        assertTrue(included.contains(ChunkRadius.pack(1, 1)));
        assertFalse(included.contains(ChunkRadius.pack(2, 1)));
        assertFalse(included.contains(ChunkRadius.pack(1, 2)));
        assertFalse(included.contains(ChunkRadius.pack(2, 2)));
    }

    @Test
    void handlesNegativeCoordinatesAndRejectsNegativeRadius() {
        assertTrue(ChunkRadius.contains(-8, -8, -10, -8, 2));
        assertTrue(ChunkRadius.contains(-8, -8, -9, -9, 2));
        assertFalse(ChunkRadius.contains(-8, -8, -10, -9, 2));
        assertTrue(ChunkRadius.contains(-1, -1, 1, -1, 2));
        assertFalse(ChunkRadius.contains(-1, -1, 1, 0, 2));
        assertTrue(ChunkRadius.contains(0, 0, 0, 0, 0));
        assertFalse(ChunkRadius.contains(0, 0, 0, 0, -1));
    }

    @Test
    void rejectsExtremeCoordinateDifferencesWithoutOverflow() {
        assertFalse(ChunkRadius.contains(
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE
        ));
        assertTrue(ChunkRadius.contains(0, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE));
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
