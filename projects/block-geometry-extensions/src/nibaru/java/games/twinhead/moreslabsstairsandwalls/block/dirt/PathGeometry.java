package games.twinhead.moreslabsstairsandwalls.block.dirt;

import net.minecraft.world.level.block.state.BlockState;

/** Geometry-owned facts consumed by provider-owned Dirt Path material semantics. */
public interface PathGeometry {
    /** True when this state presents a Path surface at the upper obstruction boundary. */
    boolean pathSurfaceRequiresClearAbove(BlockState state);
}
