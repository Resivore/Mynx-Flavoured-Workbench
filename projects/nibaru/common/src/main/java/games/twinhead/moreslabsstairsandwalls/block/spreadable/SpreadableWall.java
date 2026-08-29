package games.twinhead.moreslabsstairsandwalls.block.spreadable;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.dirt.DirtWall;
import games.twinhead.moreslabsstairsandwalls.registry.ModTags;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

@SuppressWarnings("deprecation")
public class SpreadableWall extends DirtWall implements SimpleWaterloggedBlock, BonemealableBlock {

    public static final BooleanProperty SNOWY;

    public SpreadableWall(ModBlocks block,Properties settings) {
        super(block,settings);
    }

    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        SpreadableSemantics.randomTick(state, world, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        return world.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(ModTags.GRASS_BLOCKS)) {
            SpreadableSlab.growBoneMeal(world,random,pos);
        }
    }

    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    static {
        SNOWY = BlockStateProperties.SNOWY;
    }
}
