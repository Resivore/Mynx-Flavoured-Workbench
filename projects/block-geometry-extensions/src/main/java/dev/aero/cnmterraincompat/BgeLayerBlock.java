package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * BGE-owned quarter-layer geometry composed with one canonical Nibaru material profile.
 *
 * <p>The stored {@link #FACING} is the exposed face: each additional item grows the
 * layer four pixels toward that face while the opposite face remains anchored.</p>
 */
public class BgeLayerBlock extends Block implements SimpleWaterloggedBlock, BlockspaceFundedGeometry {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 4);
    /**
     * Retained as an inert compatibility carrier for C54 blockstates and generated resources.
     * C55 economy is owned by {@link BlockspaceFundedGeometry}, not CNM's DOUBLE seam.
     */
    public static final BooleanProperty DOUBLE = VerticalSlabBlock.DOUBLE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final int LAYER_DEPTH = 4;
    private static final VoxelShape[][] SHAPES = createShapes();

    private final NibaruMaterialProfile materialProfile;

    /**
     * Creates the narrowest Layer state carrier required by the canonical material.
     * Registration should use this factory so axis and glazed-pattern properties are
     * present only for profiles that own those states.
     */
    public static BgeLayerBlock create(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
        return create(profile, properties,
                BgeLayerBlock::new,
                AxisLayerBlock::new,
                PatternLayerBlock::new,
                AxisPatternLayerBlock::new);
    }

    /**
     * Extensible form of {@link #create(NibaruMaterialProfile, BlockBehaviour.Properties)}.
     * A specialized Layer owner can supply one constructor for each material-state
     * carrier without duplicating the carrier-selection policy.
     */
    protected static BgeLayerBlock create(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties, LayerConstructor ordinary,
            LayerConstructor axisConstructor, LayerConstructor patternConstructor,
            LayerConstructor axisPatternConstructor) {
        boolean hasAxis = MaterialAxisState.applies(profile);
        boolean hasPattern = profile.capabilities().contains(BehaviorCapability.GLAZED_ORIENTATION);
        if (hasAxis && hasPattern) return axisPatternConstructor.create(profile, properties);
        if (hasAxis) return axisConstructor.create(profile, properties);
        if (hasPattern) return patternConstructor.create(profile, properties);
        return ordinary.create(profile, properties);
    }

    @FunctionalInterface
    protected interface LayerConstructor {
        BgeLayerBlock create(NibaruMaterialProfile profile, BlockBehaviour.Properties properties);
    }

    public BgeLayerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
        super(properties);
        materialProfile = Objects.requireNonNull(profile, "profile");
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(LAYERS, 1)
                .setValue(DOUBLE, false)
                .setValue(WATERLOGGED, false));
    }

    public final NibaruMaterialProfile materialProfile() {
        return materialProfile;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return geometryShape(state);
    }

    /** Exact BGE Layer volume, reusable by material-behavior subclasses. */
    protected static VoxelShape geometryShape(BlockState state) {
        return SHAPES[state.getValue(FACING).ordinal()][state.getValue(LAYERS) - 1];
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context.getClickedFace() != state.getValue(FACING)) return false;
        return expandedState(state, context) != null
                || super.canBeReplaced(state, context);
    }

    static boolean canStack(BlockState state, Direction clickedFace, ItemStack stack) {
        return clickedFace == state.getValue(FACING)
                && stack.is(state.getBlock().asItem())
                && state.getValue(LAYERS) < 4;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(pos);
        if (existing.is(this)) {
            return expandedState(existing, context);
        }

        Direction facing = placementFacing(context, pos);
        boolean waterlogged = context.getLevel().getFluidState(pos).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    @Nullable
    public BlockState expandedState(BlockState existing, BlockPlaceContext context) {
        return canStack(existing, context.getClickedFace(), context.getItemInHand())
                ? stackedState(existing)
                : null;
    }

    private static Direction placementFacing(BlockPlaceContext context, BlockPos pos) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            Direction nearest = context.getNearestLookingDirections()[0];
            if (nearest.getAxis() == Direction.Axis.Y) {
                nearest = context.getNearestLookingVerticalDirection();
            }
            return placementFacing(context.getClickedFace(), 0.0, true, nearest);
        }

        Direction clickedFace = context.getClickedFace();
        double localY = context.getClickLocation().y - pos.getY();
        return placementFacing(clickedFace, localY, false, Direction.UP);
    }

    static Direction placementFacing(Direction clickedFace, double localY,
            boolean secondaryUse, Direction nearestLook) {
        if (secondaryUse) return nearestLook.getOpposite();
        return clickedFace == Direction.DOWN
                || clickedFace != Direction.UP && localY > 0.5
                ? Direction.DOWN
                : Direction.UP;
    }

    static BlockState stackedState(BlockState state) {
        BlockState stacked = state
                .setValue(LAYERS, Math.min(4, state.getValue(LAYERS) + 1))
                .setValue(DOUBLE, false);
        return withoutWaterWhenFull(stacked);
    }

    /**
     * Equal partial Layers meet on every full-span side except the anchored side: the neighboring
     * equal Layer is anchored away from that shared block boundary. Full Layers meet everywhere.
     */
    static boolean equalStateFaceCanCull(BlockState state, Direction direction) {
        return state.getValue(LAYERS) == 4
                || direction != state.getValue(FACING).getOpposite();
    }

    /** Applies typed translucent-material adjacency without hiding an exposed partial boundary. */
    protected final boolean cullsBoundTranslucent(BlockState state, BlockState neighbor,
            Direction direction) {
        if (!NibaruProviderAdapter.cullsBoundTranslucent(this, state, neighbor)) return false;
        return neighbor.getBlock() != this || !state.equals(neighbor)
                || equalStateFaceCanCull(state, direction);
    }

    private static BlockState withoutWaterWhenFull(BlockState state) {
        return state.getValue(LAYERS) == 4 && state.getValue(WATERLOGGED)
                ? state.setValue(WATERLOGGED, false)
                : state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos,
            BlockState state, Fluid fluid) {
        return state.getValue(LAYERS) < 4
                && SimpleWaterloggedBlock.super.canPlaceLiquid(entity, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return state.getValue(LAYERS) < 4
                && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return NibaruProviderAdapter.useComposedCapabilities(this, stack, state, level, pos, player, hand)
                .orElseGet(() -> super.useItemOn(stack, state, level, pos, player, hand, hit));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAYERS, DOUBLE, WATERLOGGED);
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] result = new VoxelShape[Direction.values().length][4];
        for (Direction direction : Direction.values()) {
            for (int layers = 1; layers <= 4; layers++) {
                int depth = layers * LAYER_DEPTH;
                result[direction.ordinal()][layers - 1] = switch (direction) {
                    case UP -> Block.box(0, 0, 0, 16, depth, 16);
                    case DOWN -> Block.box(0, 16 - depth, 0, 16, 16, 16);
                    case NORTH -> Block.box(0, 0, 16 - depth, 16, 16, 16);
                    case SOUTH -> Block.box(0, 0, 0, 16, 16, depth);
                    case EAST -> Block.box(0, 0, 0, depth, 16, 16);
                    case WEST -> Block.box(16 - depth, 0, 0, 16, 16, 16);
                };
            }
        }
        return result;
    }

    /** Base carrier for specialized Layer blocks whose canonical material owns an axis. */
    public static class AxisLayerBlock extends BgeLayerBlock {
        private static final EnumProperty<Direction.Axis> AXIS = MaterialAxisState.AXIS;

        protected AxisLayerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
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
            return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
        }
    }

    /** Base carrier for specialized Layer blocks whose canonical material owns a pattern facing. */
    public static class PatternLayerBlock extends BgeLayerBlock {
        private static final EnumProperty<Direction> PATTERN_FACING = GlazedPatternState.PATTERN_FACING;

        protected PatternLayerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
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
            Direction pattern = existing.is(this)
                    ? existing.getValue(PATTERN_FACING)
                    : context.getHorizontalDirection().getOpposite();
            return placed.setValue(PATTERN_FACING, pattern);
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return super.rotate(state, rotation).setValue(PATTERN_FACING,
                    rotation.rotate(state.getValue(PATTERN_FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return super.mirror(state, mirror).setValue(PATTERN_FACING,
                    mirror.mirror(state.getValue(PATTERN_FACING)));
        }
    }

    /** Base carrier for the uncommon material profile that owns both axis and pattern facing. */
    public static class AxisPatternLayerBlock extends AxisLayerBlock {
        private static final EnumProperty<Direction> PATTERN_FACING = GlazedPatternState.PATTERN_FACING;

        protected AxisPatternLayerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
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
            Direction pattern = existing.is(this)
                    ? existing.getValue(PATTERN_FACING)
                    : context.getHorizontalDirection().getOpposite();
            return placed.setValue(PATTERN_FACING, pattern);
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return super.rotate(state, rotation).setValue(PATTERN_FACING,
                    rotation.rotate(state.getValue(PATTERN_FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return super.mirror(state, mirror).setValue(PATTERN_FACING,
                    mirror.mirror(state.getValue(PATTERN_FACING)));
        }
    }
}
