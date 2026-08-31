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

public class TwoFacingConnectingBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<ColumnType> TYPE = BBBBlockStateProperties.COLUMN_TYPE;

    public TwoFacingConnectingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(TYPE, ColumnType.NONE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockState state = defaultBlockState().setValue(AXIS, axis);
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return state;
        }
        BlockState positive = relative(context.getLevel(), context.getClickedPos(), axis, Direction.AxisDirection.POSITIVE);
        BlockState negative = relative(context.getLevel(), context.getClickedPos(), axis, Direction.AxisDirection.NEGATIVE);
        boolean positiveSingle = isSingle(positive, axis);
        boolean negativeSingle = isSingle(negative, axis);
        if (positiveSingle) return state.setValue(TYPE, ColumnType.BOTTOM);
        if (negativeSingle) return state.setValue(TYPE, ColumnType.TOP);
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        BlockState positive = relative(level, pos, axis, Direction.AxisDirection.POSITIVE);
        BlockState negative = relative(level, pos, axis, Direction.AxisDirection.NEGATIVE);
        ColumnType type = state.getValue(TYPE);
        if (type == ColumnType.BOTTOM && !isType(positive, axis, ColumnType.TOP)) {
            return state.setValue(TYPE, ColumnType.NONE);
        }
        if (type == ColumnType.TOP && !isType(negative, axis, ColumnType.BOTTOM)) {
            return state.setValue(TYPE, ColumnType.NONE);
        }
        if (type == ColumnType.NONE) {
            if (isType(positive, axis, ColumnType.TOP)) return state.setValue(TYPE, ColumnType.BOTTOM);
            if (isType(negative, axis, ColumnType.BOTTOM)) return state.setValue(TYPE, ColumnType.TOP);
        }
        return state;
    }

    private boolean isSingle(BlockState state, Direction.Axis axis) {
        return isType(state, axis, ColumnType.NONE);
    }

    private boolean isType(BlockState state, Direction.Axis axis, ColumnType type) {
        return state.is(this) && state.getValue(AXIS) == axis && state.getValue(TYPE) == type;
    }

    private static BlockState relative(LevelReader level, BlockPos pos, Direction.Axis axis,
                                       Direction.AxisDirection direction) {
        return level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, direction)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, AXIS);
    }
}
