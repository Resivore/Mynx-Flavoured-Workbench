package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.block.soulsand.SoulSandSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class SoulSandVerticalSlabBlock extends VerticalSlabBlock {
    SoulSandVerticalSlabBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(DOUBLE)) return Block.box(0, 0, 0, 16, 14, 16);
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 0, 16, 14, 8);
            case EAST -> Block.box(8, 0, 0, 16, 14, 16);
            case SOUTH -> Block.box(0, 0, 8, 16, 14, 16);
            case WEST -> Block.box(0, 0, 0, 8, 14, 16);
            default -> throw new IllegalStateException("Non-horizontal Vertical facing");
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
