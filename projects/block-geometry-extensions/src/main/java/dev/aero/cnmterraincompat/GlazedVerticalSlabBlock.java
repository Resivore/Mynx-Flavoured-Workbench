package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** CNM Vertical geometry with an independent Nibaru glazed-pattern direction. */
public final class GlazedVerticalSlabBlock extends ProviderVerticalSlabBlock {
    public static final EnumProperty<Direction> PATTERN_FACING = GlazedPatternState.PATTERN_FACING;

    public GlazedVerticalSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PATTERN_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PATTERN_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        BlockState placed = super.getStateForPlacement(context);
        if (placed == null) return null;
        Direction pattern = existing.is(this) && existing.hasProperty(PATTERN_FACING)
                ? existing.getValue(PATTERN_FACING)
                : context.getHorizontalDirection().getOpposite();
        return placed.setValue(PATTERN_FACING, pattern);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(VerticalSlabBlock.FACING, rotation.rotate(state.getValue(VerticalSlabBlock.FACING)))
                .setValue(PATTERN_FACING, rotation.rotate(state.getValue(PATTERN_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(VerticalSlabBlock.FACING, mirror.mirror(state.getValue(VerticalSlabBlock.FACING)))
                .setValue(PATTERN_FACING, mirror.mirror(state.getValue(PATTERN_FACING)));
    }
}
