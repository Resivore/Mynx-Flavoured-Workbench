package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Geometry-aware exposure of canonical upward material surfaces. */
public final class GeometrySurfaceExposure {
    public static final int NORTH_WEST = 1;
    public static final int NORTH_EAST = 1 << 1;
    public static final int SOUTH_WEST = 1 << 2;
    public static final int SOUTH_EAST = 1 << 3;
    public static final int FULL = NORTH_WEST | NORTH_EAST | SOUTH_WEST | SOUTH_EAST;

    private static final double SURFACE_SAMPLE_HEIGHT = 1.0 / 1024.0;
    private static final VoxelShape[] TOP_FOOTPRINTS = buildFootprints();

    private GeometrySurfaceExposure() {}

    /**
     * Classifies whether any of the geometry's real top footprint remains exposed to air.
     *
     * <p>Waterlogging the geometry itself is intentionally irrelevant: water in unused volume is
     * not above the material-owned top surface. Coverage is sampled from the actual collision
     * footprint of the block above. A fluid in that cell covers its bottom plane, including the
     * unused volume of a waterlogged partial block. Partial blocks only block a footprint when
     * their real coverage contains the entire relevant surface.</p>
     */
    public static SpreadableGeometry.Exposure topExposure(
            LevelReader level, BlockPos pos, int topFootprintMask) {
        if ((topFootprintMask & ~FULL) != 0 || topFootprintMask == 0) {
            throw new IllegalArgumentException("Invalid top-footprint mask: " + topFootprintMask);
        }
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (above.is(Blocks.SNOW) && above.getValue(SnowLayerBlock.LAYERS) == 1) {
            return SpreadableGeometry.Exposure.EXPOSED;
        }
        VoxelShape coverage = above.getCollisionShape(level, abovePos, CollisionContext.empty());
        if (!above.getFluidState().isEmpty()) coverage = Shapes.block();
        boolean hasExposedSurface = Shapes.joinIsNotEmpty(
                TOP_FOOTPRINTS[topFootprintMask], coverage, BooleanOp.ONLY_FIRST);
        return hasExposedSurface
                ? SpreadableGeometry.Exposure.EXPOSED
                : SpreadableGeometry.Exposure.BLOCKED;
    }

    /** Exact quarter-grid top footprint, exposed for focused deterministic tests. */
    public static VoxelShape topFootprint(int topFootprintMask) {
        if ((topFootprintMask & ~FULL) != 0 || topFootprintMask == 0) {
            throw new IllegalArgumentException("Invalid top-footprint mask: " + topFootprintMask);
        }
        return TOP_FOOTPRINTS[topFootprintMask];
    }

    private static VoxelShape[] buildFootprints() {
        VoxelShape[] result = new VoxelShape[FULL + 1];
        result[0] = Shapes.empty();
        for (int mask = 1; mask <= FULL; mask++) {
            VoxelShape shape = Shapes.empty();
            if ((mask & NORTH_WEST) != 0) shape = Shapes.or(shape, quadrant(0, 0));
            if ((mask & NORTH_EAST) != 0) shape = Shapes.or(shape, quadrant(8, 0));
            if ((mask & SOUTH_WEST) != 0) shape = Shapes.or(shape, quadrant(0, 8));
            if ((mask & SOUTH_EAST) != 0) shape = Shapes.or(shape, quadrant(8, 8));
            result[mask] = shape.optimize();
        }
        return result;
    }

    private static VoxelShape quadrant(double x, double z) {
        return Block.box(x, 0, z, x + 8, SURFACE_SAMPLE_HEIGHT, z + 8);
    }
}
