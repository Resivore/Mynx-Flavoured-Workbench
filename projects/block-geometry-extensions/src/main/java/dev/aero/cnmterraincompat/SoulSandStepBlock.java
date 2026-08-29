package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import games.twinhead.moreslabsstairsandwalls.block.soulsand.SoulSandSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.*;

final class SoulSandStepBlock extends StepBlock {
    SoulSandStepBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(SLAB_TYPE)) {
            case BOTTOM -> insetShape(facing, 0, 6);
            case TOP -> insetShape(facing, 8, 14);
            case DOUBLE -> Shapes.or(insetShape(facing, 8, 14), insetShape(facing.getOpposite(), 0, 6));
        };
    }

    private static VoxelShape insetShape(Direction facing, double minY, double maxY) {
        return switch (facing) {
            case NORTH -> Block.box(0, minY, 0, 16, maxY, 8);
            case EAST -> Block.box(8, minY, 0, 16, maxY, 16);
            case SOUTH -> Block.box(0, minY, 8, 16, maxY, 16);
            case WEST -> Block.box(0, minY, 0, 8, maxY, 16);
            default -> throw new IllegalStateException("Non-horizontal Step facing");
        };
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean notify) {
        SoulSandSemantics.schedule(level, pos, this);
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        SoulSandSemantics.scheduleIfNeeded(state, ticks, pos, this, direction, neighbor);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SoulSandSemantics.updateBubbleColumn(state, level, pos);
    }
}
