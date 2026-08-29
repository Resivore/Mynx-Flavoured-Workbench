package dev.resivore.xaerodiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CachedChunkDiscoveryTest {
    @Test
    void usesXaerosTopLeftHeightSentinelForEachChunk() {
        Set<String> sampled = new HashSet<>();
        List<Long> discovered = CachedChunkDiscovery.find(2, 3, (pixelX, pixelZ) -> {
            sampled.add(pixelX + "," + pixelZ);
            return (pixelX == 0 && pixelZ == 0) || (pixelX == 48 && pixelZ == 16)
                    ? 70
                    : Short.MAX_VALUE;
        });

        assertEquals(Set.of(ChunkRadius.pack(8, 12), ChunkRadius.pack(11, 13)), Set.copyOf(discovered));
        assertEquals(16, sampled.size());
        for (int coordinate : new int[]{0, 16, 32, 48}) {
            assertTrue(sampled.contains(coordinate + "," + coordinate));
        }
    }

    @Test
    void mapsNegativeTileChunkCoordinatesWithoutTruncation() {
        List<Long> discovered = CachedChunkDiscovery.find(-1, -1, (pixelX, pixelZ) -> 64);
        Set<Long> expected = new HashSet<>();
        for (int x = -4; x <= -1; x++) {
            for (int z = -4; z <= -1; z++) {
                expected.add(ChunkRadius.pack(x, z));
            }
        }
        assertEquals(expected, Set.copyOf(discovered));
    }
}
