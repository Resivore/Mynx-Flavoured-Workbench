package dev.resivore.mapmarkerextension.core;

public final class ChunkRelativePosition {
    public static final int CHUNK_SIZE = 16;

    private ChunkRelativePosition() {
    }

    public static int relative(int blockCoordinate) {
        return Math.floorMod(blockCoordinate, CHUNK_SIZE);
    }

    public static String format(int blockX, int blockZ) {
        return "Chunk: " + relative(blockX) + ", " + relative(blockZ);
    }
}
