package dev.aero.cnmterraincompat;

import net.minecraft.world.level.block.Block;

/**
 * Compatibility facade for callers that only need the old recursion guard.
 * Canonical identity now lives in {@link BgeMaterialBindings}.
 */
public final class CanonicalGeometryRegistry {
    private CanonicalGeometryRegistry() {
    }

    public static void register(Block... blocks) {
        for (Block block : blocks) BgeMaterialBindings.noteDerivedGeometry(block);
    }

    public static boolean contains(Block block) {
        return BgeMaterialBindings.isDerivedGeometry(block);
    }
}
