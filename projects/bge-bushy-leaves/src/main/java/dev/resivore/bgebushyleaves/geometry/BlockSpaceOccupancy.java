package dev.resivore.bgebushyleaves.geometry;

import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A conservative 1/16 block-space occupancy reconstructed from BGE's complete authoritative
 * exposed-surface contract. This is deliberately shape data, not a collection of card anchors.
 */
public final class BlockSpaceOccupancy {
    public static final int UNITS = 16;
    private final boolean[][][] cells = new boolean[UNITS][UNITS][UNITS];
    private final Bounds bounds;

    private BlockSpaceOccupancy(List<SurfacePatch> patches) {
        for (int x = 0; x < UNITS; x++) for (int y = 0; y < UNITS; y++) for (int z = 0; z < UNITS; z++) {
            // A ray from block-space -X toggles at every X-facing boundary. BGE guarantees
            // complete exposed boundaries for supported models, so parity is exact for its
            // axis-aligned cuboid unions and does not need topology-name knowledge here.
            int crossings = 0;
            for (SurfacePatch patch : patches) if (patch.normal().getAxis() == Direction.Axis.X
                    && patch.plane16() <= x && covers(patch, x + 0.5F, y + 0.5F, z + 0.5F)) crossings++;
            cells[x][y][z] = (crossings & 1) == 1;
        }
        bounds = findBounds();
    }

    public static Optional<BlockSpaceOccupancy> fromSurfacePatches(List<SurfacePatch> patches) {
        Objects.requireNonNull(patches, "patches");
        if (patches.isEmpty()) return Optional.empty();
        BlockSpaceOccupancy occupancy = new BlockSpaceOccupancy(List.copyOf(patches));
        return occupancy.bounds == null ? Optional.empty() : Optional.of(occupancy);
    }

    public boolean contains(float x16, float y16, float z16) {
        int x = (int) Math.floor(x16), y = (int) Math.floor(y16), z = (int) Math.floor(z16);
        return x >= 0 && x < UNITS && y >= 0 && y < UNITS && z >= 0 && z < UNITS && cells[x][y][z];
    }

    public Bounds bounds() {
        if (bounds == null) throw new IllegalStateException("Empty BGE occupancy has no bounds");
        return bounds;
    }

    boolean fillsBounds() {
        if (bounds == null) return false;
        for (int x = bounds.minX(); x < bounds.maxX(); x++) for (int y = bounds.minY(); y < bounds.maxY(); y++)
            for (int z = bounds.minZ(); z < bounds.maxZ(); z++) if (!cells[x][y][z]) return false;
        return true;
    }

    private Bounds findBounds() {
        int minX = UNITS, minY = UNITS, minZ = UNITS, maxX = 0, maxY = 0, maxZ = 0;
        for (int x = 0; x < UNITS; x++) for (int y = 0; y < UNITS; y++) for (int z = 0; z < UNITS; z++) {
            if (!cells[x][y][z]) continue;
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x + 1); maxY = Math.max(maxY, y + 1); maxZ = Math.max(maxZ, z + 1);
        }
        return minX == UNITS ? null : new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean covers(SurfacePatch patch, float x, float y, float z) {
        float u = coordinate(patch.uAxis(), x, y, z), v = coordinate(patch.vAxis(), x, y, z);
        return u >= patch.uMin16() && u < patch.uMax16() && v >= patch.vMin16() && v < patch.vMax16();
    }

    private static float coordinate(Direction.Axis axis, float x, float y, float z) {
        return switch (axis) { case X -> x; case Y -> y; case Z -> z; };
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Bounds {
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > UNITS || maxY > UNITS || maxZ > UNITS
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) throw new IllegalArgumentException("Invalid bounds");
        }
    }
}
