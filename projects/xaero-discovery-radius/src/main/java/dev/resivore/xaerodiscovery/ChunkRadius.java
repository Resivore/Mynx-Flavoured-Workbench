package dev.resivore.xaerodiscovery;

public final class ChunkRadius {
    private ChunkRadius() {
    }

    public static boolean contains(int centerChunkX, int centerChunkZ, int targetChunkX, int targetChunkZ, int radius) {
        if (radius < 0) {
            return false;
        }
        long deltaX = Math.abs((long) targetChunkX - centerChunkX);
        long deltaZ = Math.abs((long) targetChunkZ - centerChunkZ);
        return deltaX <= radius && deltaZ <= radius;
    }

    public static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }
}
