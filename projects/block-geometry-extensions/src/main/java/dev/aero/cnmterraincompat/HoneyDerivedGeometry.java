package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** CNM-owned physical shapes with the native Honey one-pixel side/top inset applied. */
public final class HoneyDerivedGeometry {
    private HoneyDerivedGeometry() {}

    public static VoxelShape vertical(BlockState state) {
        if (state.getValue(VerticalSlabBlock.DOUBLE)) return Block.box(1, 0, 1, 15, 15, 15);
        return insetHalf(state.getValue(VerticalSlabBlock.FACING), 0, 15);
    }

    public static VoxelShape step(BlockState state) {
        Direction facing = state.getValue(StepBlock.FACING);
        return switch (state.getValue(StepBlock.SLAB_TYPE)) {
            case BOTTOM -> insetHalf(facing, 0, 7);
            case TOP -> insetHalf(facing, 8, 15);
            case DOUBLE -> Shapes.or(insetHalf(facing, 8, 15),
                    insetHalf(facing.getOpposite(), 0, 7));
        };
    }

    private static VoxelShape insetHalf(Direction facing, double minY, double maxY) {
        return switch (facing) {
            case NORTH -> Block.box(1, minY, 1, 15, maxY, 7);
            case EAST -> Block.box(9, minY, 1, 15, maxY, 15);
            case SOUTH -> Block.box(1, minY, 9, 15, maxY, 15);
            case WEST -> Block.box(1, minY, 1, 7, maxY, 15);
            default -> throw new IllegalStateException("Honey geometry must face horizontally");
        };
    }
}
