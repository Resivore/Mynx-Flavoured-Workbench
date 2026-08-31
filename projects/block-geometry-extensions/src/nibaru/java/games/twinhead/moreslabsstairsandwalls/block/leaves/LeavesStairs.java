package games.twinhead.moreslabsstairsandwalls.block.leaves;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class LeavesStairs extends BaseStairs implements LeafDistanceCarrier {

    public static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

    public LeavesStairs(ModBlocks block,  BlockState baseBlockState, Properties settings) {
        super(block, baseBlockState, settings);
        registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : LeafSemantics.applyPlayerPlacementState(state);
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return super.getShape(state, world, pos, context);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return LeafSemantics.isRandomlyTicking(state);
    }

    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        LeafSemantics.decayIfNeeded(state, world, pos);
    }

    public boolean shouldDecay(BlockState state) {
        return LeafSemantics.shouldDecay(state);
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        world.setBlock(pos, LeafSemantics.updateDistanceFromLogs(state, world, pos), 3);
    }

    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 1;
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        LeafSemantics.scheduleDistanceUpdate(state, world, tickAccess, pos, this, neighborState);

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }


    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        LeafSemantics.animateRainDrip(state, world, pos, random);
    }

    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, PERSISTENT);
        super.createBlockStateDefinition(builder);
    }
}
