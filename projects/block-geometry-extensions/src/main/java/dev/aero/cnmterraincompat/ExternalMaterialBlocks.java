package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/** Standard Slab/Stairs/Wall carriers used only by allowlisted external material families. */
public final class ExternalMaterialBlocks {
    private ExternalMaterialBlocks() {}

    public static StandardSet create(Block source, Block.Properties slabProperties,
            Block.Properties stairProperties, Block.Properties wallProperties, boolean leaves) {
        if (leaves) return new StandardSet(
                new LeafSlab(slabProperties),
                new LeafStairs(source.defaultBlockState(), stairProperties),
                new LeafWall(wallProperties));
        if (source.defaultBlockState().hasProperty(BlockStateProperties.AXIS)) return new StandardSet(
                new AxisSlab(slabProperties),
                new AxisStairs(source.defaultBlockState(), stairProperties),
                // A wall's connection state is its complete placement contract.  It is not a
                // rotated pillar merely because the material it is made from has an axis.
                new WallBlock(wallProperties));
        return new StandardSet(new SlabBlock(slabProperties),
                new StairBlock(source.defaultBlockState(), stairProperties), new WallBlock(wallProperties));
    }

    public record StandardSet(SlabBlock slab, StairBlock stairs, WallBlock wall) {}

    public static final class AxisSlab extends SlabBlock {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

        AxisSlab(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(AXIS);
        }

        @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
            BlockState placed = super.getStateForPlacement(context);
            return placed == null ? null : placed.setValue(AXIS,
                    MaterialAxisState.placementAxis(existing, this, context.getClickedFace()));
        }

        @Override public BlockState rotate(BlockState state, Rotation rotation) {
            return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
        }
    }

    public static final class AxisStairs extends StairBlock {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

        AxisStairs(BlockState source, Properties properties) {
            super(source, properties);
            registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(AXIS);
        }

        @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState placed = super.getStateForPlacement(context);
            return placed == null ? null : placed.setValue(AXIS, context.getClickedFace().getAxis());
        }

        @Override public BlockState rotate(BlockState state, Rotation rotation) {
            return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
        }
    }

    private abstract static class LeafSupport {
        static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
        static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

        static BlockState placed(BlockState state) { return LeafSemantics.applyPlayerPlacementState(state); }
        static void tick(BlockState state, ServerLevel level, BlockPos pos) {
            level.setBlock(pos, LeafSemantics.updateDistanceFromLogs(state, level, pos), 3);
        }
        static BlockState update(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Block block, BlockState neighbor) {
            if (state.getValue(BlockStateProperties.WATERLOGGED))
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            LeafSemantics.scheduleDistanceUpdate(state, level, ticks, pos, block, neighbor);
            return state;
        }
    }

    @SuppressWarnings("deprecation")
    public static final class LeafSlab extends SlabBlock implements LeafDistanceCarrier {
        LeafSlab(Properties properties) {
            super(properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LeafSupport.DISTANCE, LeafSupport.PERSISTENT);
            super.createBlockStateDefinition(builder);
        }
        @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : LeafSupport.placed(state);
        }
        @Override public boolean isRandomlyTicking(BlockState state) { return LeafSemantics.isRandomlyTicking(state); }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSemantics.decayIfNeeded(state, level, pos);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSupport.tick(state, level, pos);
        }
        public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
        @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            return LeafSupport.update(state, level, ticks, pos, this, neighbor);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            LeafSemantics.animateRainDrip(state, level, pos, random);
        }
    }

    @SuppressWarnings("deprecation")
    public static final class LeafStairs extends StairBlock implements LeafDistanceCarrier {
        LeafStairs(BlockState source, Properties properties) {
            super(source, properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LeafSupport.DISTANCE, LeafSupport.PERSISTENT);
            super.createBlockStateDefinition(builder);
        }
        @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : LeafSupport.placed(state);
        }
        @Override public boolean isRandomlyTicking(BlockState state) { return LeafSemantics.isRandomlyTicking(state); }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSemantics.decayIfNeeded(state, level, pos);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSupport.tick(state, level, pos);
        }
        public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
        @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            LeafSupport.update(state, level, ticks, pos, this, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            LeafSemantics.animateRainDrip(state, level, pos, random);
        }
    }

    @SuppressWarnings("deprecation")
    public static final class LeafWall extends WallBlock implements LeafDistanceCarrier {
        LeafWall(Properties properties) {
            super(properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LeafSupport.DISTANCE, LeafSupport.PERSISTENT);
            super.createBlockStateDefinition(builder);
        }
        @Override @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : LeafSupport.placed(state);
        }
        @Override public boolean isRandomlyTicking(BlockState state) { return LeafSemantics.isRandomlyTicking(state); }
        @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSemantics.decayIfNeeded(state, level, pos);
        }
        @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            LeafSupport.tick(state, level, pos);
        }
        public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
        @Override public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            LeafSupport.update(state, level, ticks, pos, this, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            LeafSemantics.animateRainDrip(state, level, pos, random);
        }
    }
}
