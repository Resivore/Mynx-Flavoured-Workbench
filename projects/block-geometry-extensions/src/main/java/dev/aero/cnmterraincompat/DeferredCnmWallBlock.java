package dev.aero.cnmterraincompat;

import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A registry-time normal Wall carrier for a CNM component whose parent is not available until
 * ShapeMap resolution.  It intentionally has only Wall topology: the bridge later binds its
 * canonical economy and resources from CNM's elected parent, or keeps it out of a component
 * which already supplied a Wall.
 */
final class DeferredCnmWallBlock extends WallBlock {
    DeferredCnmWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
