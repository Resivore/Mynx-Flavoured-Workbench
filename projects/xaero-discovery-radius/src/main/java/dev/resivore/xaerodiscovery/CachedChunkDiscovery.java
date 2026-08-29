package dev.resivore.xaerodiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

final class CachedChunkDiscovery {
    private static final int CHUNKS_PER_TILE_CHUNK = 4;
    private static final int PIXELS_PER_CHUNK = 16;

    private CachedChunkDiscovery() {
    }

    static List<Long> find(int tileChunkX, int tileChunkZ, IntBinaryOperator heightAtPixel) {
        List<Long> discovered = new ArrayList<>(16);
        int startChunkX = tileChunkX << 2;
        int startChunkZ = tileChunkZ << 2;
        for (int localX = 0; localX < CHUNKS_PER_TILE_CHUNK; localX++) {
            for (int localZ = 0; localZ < CHUNKS_PER_TILE_CHUNK; localZ++) {
                if (heightAtPixel.applyAsInt(localX * PIXELS_PER_CHUNK, localZ * PIXELS_PER_CHUNK) != Short.MAX_VALUE) {
                    discovered.add(ChunkRadius.pack(startChunkX + localX, startChunkZ + localZ));
                }
            }
        }
        return discovered;
    }
}
