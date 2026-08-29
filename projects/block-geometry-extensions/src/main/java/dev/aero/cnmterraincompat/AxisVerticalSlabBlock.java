package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** CNM Vertical geometry with an independent vanilla-style material axis. */
public final class AxisVerticalSlabBlock extends ProviderVerticalSlabBlock {
    public static final EnumProperty<Direction.Axis> AXIS = MaterialAxisState.AXIS;

    public AxisVerticalSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        BlockState placed = super.getStateForPlacement(context);
        return placed == null ? null : placed.setValue(AXIS,
                MaterialAxisState.placementAxis(existing, this, context.getClickedFace()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState geometryRotated = state.setValue(VerticalSlabBlock.FACING,
                rotation.rotate(state.getValue(VerticalSlabBlock.FACING)));
        return RotatedPillarBlock.rotatePillar(geometryRotated, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(VerticalSlabBlock.FACING,
                mirror.mirror(state.getValue(VerticalSlabBlock.FACING)));
    }
}
