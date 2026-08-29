package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import games.twinhead.moreslabsstairsandwalls.block.magma.MagmaSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
final class MagmaStepBlock extends StepBlock {
    MagmaStepBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        MagmaSemantics.hurtIfNeeded(level, entity);
        super.stepOn(level, pos, state, entity);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean notify) {
        MagmaSemantics.schedule(level, pos, this);
        super.onPlace(state, level, pos, old, notify);
    }
    @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        MagmaSemantics.scheduleForWaterAbove(ticks, pos, this, direction, neighbor);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        MagmaSemantics.updateBubbleColumn(level, pos, state);
    }
}
