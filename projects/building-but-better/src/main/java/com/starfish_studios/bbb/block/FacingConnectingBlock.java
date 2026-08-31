package com.starfish_studios.bbb.block;

import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import com.starfish_studios.bbb.block.properties.ColumnType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class FacingConnectingBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<ColumnType> TYPE = BBBBlockStateProperties.COLUMN_TYPE;

    public FacingConnectingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TYPE, ColumnType.NONE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockState state = defaultBlockState().setValue(AXIS, axis);
        return state.setValue(TYPE, getType(state,
                relative(context.getLevel(), context.getClickedPos(), axis, Direction.AxisDirection.POSITIVE),
                relative(context.getLevel(), context.getClickedPos(), axis, Direction.AxisDirection.NEGATIVE)));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        return state.setValue(TYPE, getType(state,
                relative(level, pos, axis, Direction.AxisDirection.POSITIVE),
                relative(level, pos, axis, Direction.AxisDirection.NEGATIVE)));
    }

    private static BlockState relative(LevelReader level, BlockPos pos, Direction.Axis axis,
                                       Direction.AxisDirection direction) {
        return level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, direction)));
    }

    public ColumnType getType(BlockState state, BlockState positive, BlockState negative) {
        boolean positiveSame = positive.is(state.getBlock()) && positive.getValue(AXIS) == state.getValue(AXIS);
        boolean negativeSame = negative.is(state.getBlock()) && negative.getValue(AXIS) == state.getValue(AXIS);
        if (positiveSame && !negativeSame) return ColumnType.BOTTOM;
        if (!positiveSame && negativeSame) return ColumnType.TOP;
        if (positiveSame) return ColumnType.MIDDLE;
        return ColumnType.NONE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, AXIS);
    }
}
