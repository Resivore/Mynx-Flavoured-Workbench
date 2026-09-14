package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;

/** Bounds a partial vanilla path endpoint before world and sight-line checks. */
final class SheepPathEndpoint {
    private SheepPathEndpoint() {}

    static boolean nearRequested(BlockPos requested, BlockPos endpoint) {
        if (requested == null || endpoint == null) return false;
        int dx = Math.abs(requested.getX() - endpoint.getX());
        int dy = Math.abs(requested.getY() - endpoint.getY());
        int dz = Math.abs(requested.getZ() - endpoint.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1 && dx * dx + dy * dy + dz * dz <= 2;
    }
}
