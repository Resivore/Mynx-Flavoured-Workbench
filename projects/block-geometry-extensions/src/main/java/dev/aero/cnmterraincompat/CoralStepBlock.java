package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.coral.CoralSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CoralStepBlock extends ProviderStepBlock {
    public CoralStepBlock(Properties properties) { super(properties); }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!CoralSemantics.survives(state, level, pos)) level.setBlock(pos,
                CoralSemantics.deadState(state, NibaruProviderAdapter.coralDeath(this)), Block.UPDATE_CLIENTS);
    }
    @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        CoralSemantics.scheduleIfDry(level, ticks, pos, this, random);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        CoralSemantics.scheduleIfDry(context.getLevel(), context.getLevel(), context.getClickedPos(), this,
                context.getLevel().getRandom());
        return super.getStateForPlacement(context);
    }
}
