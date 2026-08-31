package com.starfish_studios.bbb.block;

import com.starfish_studios.bbb.registry.BBBContent;
import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import com.starfish_studios.bbb.block.properties.FrameStickDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

public class FrameBlock extends Block implements SimpleWaterloggedBlock, HammerableBlock {
    public static final BooleanProperty CORNERS = BooleanProperty.create("corners");
    public static final EnumProperty<FrameStickDirection> FRAME_CENTER = BBBBlockStateProperties.FRAME_CENTER;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TOP = BBBBlockStateProperties.TOP;
    public static final BooleanProperty BOTTOM = BBBBlockStateProperties.BOTTOM;
    public static final BooleanProperty LEFT = BBBBlockStateProperties.LEFT;
    public static final BooleanProperty RIGHT = BBBBlockStateProperties.RIGHT;


    // full-block outline shapes
    private static final VoxelShape NORTH = Block.box(0, 0, 8, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape WEST = Block.box(8, 0, 0, 16, 16, 16);

    // center-stick collision shapes
    private static final VoxelShape NORTH_CENTER = Block.box(4, 0, 13, 12, 16, 16);
    private static final VoxelShape EAST_CENTER = Block.box(0, 0, 4, 3, 16, 12);
    private static final VoxelShape SOUTH_CENTER = Block.box(4, 0, 0, 12, 16, 3);
    private static final VoxelShape WEST_CENTER = Block.box(13, 0, 4, 16, 16, 12);

    private static final VoxelShape FALLBACK = Block.box(0, 0, 0, 1, 1, 1);

    // side-sticks
    private static final VoxelShape[] NORTH_SIDES = {
            Block.box(0, 15, 13, 16, 16, 16),
            Block.box(0, -1, 13, 16, 0, 16),
            Block.box(15, 0, 13, 16, 16, 16),
            Block.box(0, 0, 13, 1, 16, 16)
    };

    private static final VoxelShape[] EAST_SIDES = {
            Block.box(0, 15, 0, 3, 16, 16),
            Block.box(0, -1, 0, 3, 0, 16),
            Block.box(0, 0, 15, 3, 16, 16),
            Block.box(0, 0, 0, 3, 16, 1)
    };

    private static final VoxelShape[] SOUTH_SIDES = {
            Block.box(0, 15, 0, 16, 16, 3),
            Block.box(0, -1, 0, 16, 0, 3),
            Block.box(0, 0, 0, 1, 16, 3),
            Block.box(15, 0, 0, 16, 16, 3)
    };

    private static final VoxelShape[] WEST_SIDES = {
            Block.box(13, 15, 0, 16, 16, 16),
            Block.box(13, -1, 0, 16, 0, 16),
            Block.box(13, 0, 0, 16, 16, 1),
            Block.box(13, 0, 15, 16, 16, 16)
    };
    private static final Map<Direction, VoxelShape> FULL_SHAPES = Map.of(
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST);

    private static final Map<Direction, VoxelShape> CENTER_SHAPES = Map.of(
            Direction.NORTH, NORTH_CENTER,
            Direction.EAST, EAST_CENTER,
            Direction.SOUTH, SOUTH_CENTER,
            Direction.WEST, WEST_CENTER
    );

    private static final Map<Direction, VoxelShape[]> SIDE_SHAPES = Map.of(
            Direction.NORTH, NORTH_SIDES,
            Direction.EAST, EAST_SIDES,
            Direction.SOUTH, SOUTH_SIDES,
            Direction.WEST, WEST_SIDES
    );

    private static final int TOP_INDEX = 0;
    private static final int BOTTOM_INDEX = 1;
    private static final int LEFT_INDEX = 2;
    private static final int RIGHT_INDEX = 3;

    public FrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(LEFT, true)
                .setValue(RIGHT, true)
                .setValue(CORNERS, true)
                .setValue(FRAME_CENTER, FrameStickDirection.NONE)
        );
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        FrameStickDirection[] cycle = {
                FrameStickDirection.LEFT,
                FrameStickDirection.VERTICAL,
                FrameStickDirection.RIGHT,
                FrameStickDirection.HORIZONTAL
        };
        FrameStickDirection current = state.getValue(FRAME_CENTER);
        int index = java.util.Arrays.asList(cycle).indexOf(current);
        FrameStickDirection next = cycle[(index + 1) % cycle.length];
        BlockState updated = state.setValue(FRAME_CENTER, next);
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (state.is(BBBContent.WOODEN_FRAMES)) {
            level.playSound(player, pos, Blocks.SCAFFOLDING.defaultBlockState().getSoundType().getPlaceSound(),
                    player.getSoundSource(), 1.0F, 1.0F);
        } else if (state.is(BBBContent.STONE_FRAMES)) {
            level.playSound(player, pos, Blocks.STONE.defaultBlockState().getSoundType().getPlaceSound(),
                    player.getSoundSource(), 1.0F, 1.0F);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }


    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        resetCenter(state, level, pos);
    }

    public void resetCenter(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(FRAME_CENTER) != FrameStickDirection.NONE) {
            BlockState reset = state.setValue(FRAME_CENTER, FrameStickDirection.NONE);
            level.setBlock(pos, reset, Block.UPDATE_ALL);
        }
    }

    private boolean showsFullOutline(CollisionContext context, BlockState state) {
        if (context instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player player) {
            return player.isShiftKeyDown() || player.isHolding(stack ->
                    stack.is(BBBContent.HAMMERS)
                            || stack.is(BBBContent.FRAME_ITEMS));
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        if (showsFullOutline(context, state)) {
            return FULL_SHAPES.get(state.getValue(FACING));
        }

        VoxelShape shape = buildShape(state);

        if (shape.isEmpty() && context == CollisionContext.empty()) {
            return FALLBACK;
        }
        return shape;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return buildShape(state);
    }

    private VoxelShape buildShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        VoxelShape shape = state.getValue(FRAME_CENTER) != FrameStickDirection.NONE
                ? CENTER_SHAPES.get(facing)
                : Shapes.empty();

        VoxelShape[] parts = SIDE_SHAPES.get(facing);
        if (state.getValue(TOP))    shape = Shapes.or(shape, parts[TOP_INDEX]);
        if (state.getValue(BOTTOM)) shape = Shapes.or(shape, parts[BOTTOM_INDEX]);
        if (state.getValue(LEFT))   shape = Shapes.or(shape, parts[LEFT_INDEX]);
        if (state.getValue(RIGHT))  shape = Shapes.or(shape, parts[RIGHT_INDEX]);

        return shape;
    }


    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighbor, net.minecraft.util.RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return getConnectedState(state, level, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, TOP, BOTTOM, LEFT, RIGHT, CORNERS, FRAME_CENTER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.LAND
                && !state.getValue(TOP)
                && !state.getValue(BOTTOM)
                && !state.getValue(LEFT)
                && !state.getValue(RIGHT);
    }

    private BlockState getConnectedState(BlockState state, LevelReader level, BlockPos pos) {
        EnumMap<Direction, Boolean> connections = new EnumMap<>(Direction.class);
        Direction facing = state.getValue(FACING);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);

            boolean canConnect;
            if (neighborState.getBlock() instanceof FrameBlock) {
                canConnect = neighborState.getValue(FACING) == facing;
            } else {
                canConnect = validConnection(neighborState, level, neighborPos, direction);
            }
            connections.put(direction, canConnect);
        }

        return state
                .setValue(TOP, !connections.get(Direction.UP))
                .setValue(BOTTOM, !connections.get(Direction.DOWN))
                .setValue(LEFT, !connections.get(facing.getClockWise()))
                .setValue(RIGHT, !connections.get(facing.getCounterClockWise()));
    }


    public boolean validConnection(BlockState state, BlockGetter getter, BlockPos pos, Direction direction) {
        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM && direction == Direction.UP) {
                return true;
            }
            return type == SlabType.TOP && direction == Direction.DOWN;
        }
        if (state.is(BBBContent.FRAMES)) {
            return true;
        }
        return state.isSolidRender();
    }
}
