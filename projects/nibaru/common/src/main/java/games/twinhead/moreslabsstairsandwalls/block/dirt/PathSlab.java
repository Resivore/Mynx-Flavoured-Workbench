package games.twinhead.moreslabsstairsandwalls.block.dirt;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseSlab;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
@SuppressWarnings("deprecation")
public class PathSlab extends BaseSlab implements PathGeometry {

    public static final VoxelShape BOTTOM_SHAPE;
    public static final VoxelShape TOP_SHAPE;
    public static final VoxelShape FULL_SHAPE;

    public PathSlab(ModBlocks modBlocks, Properties settings) {
        super(modBlocks,settings);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos blockPos = ctx.getClickedPos();
        BlockState blockState = ctx.getLevel().getBlockState(blockPos);
        if (blockState.is(this)) {
            return PathSemantics.placementState(blockState.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false),
                    ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB).getStateForPlacement(ctx), ctx.getLevel(), blockPos);
        } else {
            FluidState fluidState = ctx.getLevel().getFluidState(blockPos);
            BlockState blockState2 = this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
            Direction direction = ctx.getClickedFace();
            BlockState placed = direction != Direction.DOWN && (direction == Direction.UP || !(ctx.getClickLocation().y - (double)blockPos.getY() > 0.5)) ? blockState2 : blockState2.setValue(TYPE, SlabType.TOP);
            return PathSemantics.placementState(placed,
                    ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB).getStateForPlacement(ctx), ctx.getLevel(), blockPos);
        }
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        SlabType slabType = state.getValue(TYPE);
        return switch (slabType) {
            case DOUBLE -> FULL_SHAPE;
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        PathSemantics.scheduleConversionIfNeeded(state, world, tickAccess, pos, direction);

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }


    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        PathSemantics.revertIfObstructed(world, pos, state, ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB).defaultBlockState());
    }

    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return PathSemantics.canSurvive(state, world, pos);
    }

    @Override
    public boolean pathSurfaceRequiresClearAbove(BlockState state) {
        return state.getValue(TYPE) != SlabType.BOTTOM;
    }

    static {
        BOTTOM_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
        TOP_SHAPE = Block.box(0.0, 7.0, 0.0, 16.0, 15.0, 16.0);
        FULL_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);
    }


}
