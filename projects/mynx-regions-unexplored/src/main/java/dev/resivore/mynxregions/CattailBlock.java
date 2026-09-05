package dev.resivore.mynxregions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Two-cell dry/waterlogged cattail with source-water restoration on cleanup. */
public final class CattailBlock extends DoublePlantBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CattailBlock> CODEC = simpleCodec(CattailBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CattailBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, false));
    }
    @Override public MapCodec<CattailBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HALF, WATERLOGGED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        BlockPos pos = context.getClickedPos();
        if (state == null || !context.getLevel().getFluidState(pos.above()).isEmpty()) return null;
        return state.setValue(WATERLOGGED, context.getLevel().getFluidState(pos).is(Fluids.WATER));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(WATERLOGGED, false), 3);
    }
    @Override protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.is(MynxRegionsUnexplored.CATTAIL_SUPPORTS);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER)
            return level.getBlockState(pos.below()).is(this) && level.getBlockState(pos.below()).getValue(HALF) == DoubleBlockHalf.LOWER;
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below()) && level.getFluidState(pos.above()).isEmpty();
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                                Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        return updated.isAir() && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : updated;
    }
    @Override protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return false; }
}
