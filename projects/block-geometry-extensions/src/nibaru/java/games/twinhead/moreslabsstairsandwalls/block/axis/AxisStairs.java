package games.twinhead.moreslabsstairsandwalls.block.axis;

import games.twinhead.moreslabsstairsandwalls.api.material.MaterialAxisSemantics;
import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseStairs;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** Native stair with material orientation independent of facing, half, and corner shape. */
public class AxisStairs extends BaseStairs {
    public static final EnumProperty<Direction.Axis> AXIS = MaterialAxisSemantics.AXIS;

    public AxisStairs(ModBlocks block, BlockState defaultState, Properties settings) {
        super(block, defaultState, settings);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = super.getStateForPlacement(context);
        return placed == null ? null : placed.setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
    }
}
