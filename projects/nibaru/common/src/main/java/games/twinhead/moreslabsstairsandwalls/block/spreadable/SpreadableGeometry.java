package games.twinhead.moreslabsstairsandwalls.block.spreadable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/** Geometry-owned override for material survival; it has no CNM dependency. */
public interface SpreadableGeometry {
    Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos);

    enum Exposure {
        DEFAULT,
        EXPOSED,
        BLOCKED
    }
}
