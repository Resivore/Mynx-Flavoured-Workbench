package dev.aero.cnmterraincompat;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.world.level.block.Block;

/**
 * Blocks that are geometries owned by an existing material family, not new
 * material roots from which another set of shapes should be derived.
 */
public final class CanonicalGeometryRegistry {
    private static final Set<Block> GEOMETRIES = Collections.newSetFromMap(new IdentityHashMap<>());

    private CanonicalGeometryRegistry() {
    }

    public static void register(Block... blocks) {
        Collections.addAll(GEOMETRIES, blocks);
    }

    public static boolean contains(Block block) {
        return GEOMETRIES.contains(block);
    }
}
