package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafSemantics;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/** BGE Corner: the reference 16x8x8, hit-oriented, non-combining quarter piece. */
public class BgeCornerBlock extends BgeProfiledGeometryBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    private static final VoxelShape NORTH_BOTTOM = Block.box(0, 0, 0, 16, 8, 8);
    private static final VoxelShape SOUTH_BOTTOM = Block.box(0, 0, 8, 16, 8, 16);
    private static final VoxelShape EAST_BOTTOM = Block.box(8, 0, 0, 16, 8, 16);
    private static final VoxelShape WEST_BOTTOM = Block.box(0, 0, 0, 8, 8, 16);
    private static final VoxelShape NORTH_TOP = Block.box(0, 8, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_TOP = Block.box(0, 8, 8, 16, 16, 16);
    private static final VoxelShape EAST_TOP = Block.box(8, 8, 0, 16, 16, 16);
    private static final VoxelShape WEST_TOP = Block.box(0, 8, 0, 8, 16, 16);

    public static BgeCornerBlock create(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        var capabilities = profile.capabilities();
        if (capabilities.contains(BehaviorCapability.LEAF_LIFECYCLE))
            return new LeavesCornerBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.PATH_CONVERSION))
            return new PathCornerBlock(profile, properties);
        if (isSpreadableSurface(profile))
            return new SpreadableCornerBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.OXIDIZABLE))
            return new CopperCornerBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.CONCRETE_HARDENING))
            return new ConcreteCornerBlock(profile, properties);
        if (capabilities.contains(BehaviorCapability.FALLING))
            return new FallingCornerBlock(profile, properties);
        if (capabilities.contains(BehaviorCapability.ICE_MELTING)) properties = properties.randomTicks();
        boolean axis = MaterialAxisState.applies(profile);
        boolean pattern = capabilities.contains(BehaviorCapability.GLAZED_ORIENTATION);
        if (axis && pattern) return new AxisPatternCornerBlock(profile, properties);
        if (axis) return new AxisCornerBlock(profile, properties);
        if (pattern) return new PatternCornerBlock(profile, properties);
        return new BgeCornerBlock(profile, properties);
    }

    @Override
    protected BgeGeometryRole geometryRole() {
        return BgeGeometryRole.CORNER;
    }

    public BgeCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
        super(profile, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        if (has(BehaviorCapability.PATH_CONVERSION)) return pathShape(state);
        return regularShape(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction.Axis placementAxis = context.getHorizontalDirection().getAxis();
        double coordinate = context.getClickLocation().get(placementAxis) - pos.get(placementAxis);
        Direction facing = switch (placementAxis) {
            case X -> coordinate < 0.5 ? Direction.WEST : Direction.EAST;
            case Z -> coordinate < 0.5 ? Direction.NORTH : Direction.SOUTH;
            case Y -> throw new IllegalStateException("Horizontal placement axis cannot be Y");
        };
        Direction clickedFace = context.getClickedFace();
        double localY = context.getClickLocation().y - pos.getY();
        Half half = clickedFace == Direction.DOWN
                || clickedFace != Direction.UP && localY > 0.5
                ? Half.TOP : Half.BOTTOM;
        return applyProfilePlacement(defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, half)
                .setValue(WATERLOGGED,
                        context.getLevel().getFluidState(pos).getType() == Fluids.WATER), context);
    }

    @Override
    protected VoxelShape materialCollisionShape(BlockState state, boolean honey) {
        Direction facing = state.getValue(FACING);
        boolean top = state.getValue(HALF) == Half.TOP;
        double minY = top ? 8 : 0;
        double maxY = top ? honey ? 15 : 14 : honey ? 7 : 6;
        if (!honey) return halfShape(facing, minY, maxY, 0);
        return halfShape(facing, minY, maxY, 1);
    }

    @Override
    protected boolean equalStateFaceCanCull(BlockState state, Direction direction) {
        return direction.getAxis().isHorizontal()
                && direction.getAxis() != state.getValue(FACING).getAxis();
    }

    private static VoxelShape regularShape(BlockState state) {
        boolean top = state.getValue(HALF) == Half.TOP;
        return switch (state.getValue(FACING)) {
            case NORTH -> top ? NORTH_TOP : NORTH_BOTTOM;
            case SOUTH -> top ? SOUTH_TOP : SOUTH_BOTTOM;
            case EAST -> top ? EAST_TOP : EAST_BOTTOM;
            case WEST -> top ? WEST_TOP : WEST_BOTTOM;
            default -> throw new IllegalStateException("Corner facing must be horizontal");
        };
    }

    private static VoxelShape pathShape(BlockState state) {
        boolean top = state.getValue(HALF) == Half.TOP;
        return halfShape(state.getValue(FACING), top ? 7 : 0, top ? 15 : 7, 0);
    }

    private static VoxelShape halfShape(Direction facing, double minY, double maxY, double inset) {
        return switch (facing) {
            case NORTH -> Block.box(inset, minY, inset, 16 - inset, maxY, 8 - inset);
            case SOUTH -> Block.box(inset, minY, 8 + inset, 16 - inset, maxY, 16 - inset);
            case EAST -> Block.box(8 + inset, minY, inset, 16 - inset, maxY, 16 - inset);
            case WEST -> Block.box(inset, minY, inset, 8 - inset, maxY, 16 - inset);
            default -> throw new IllegalStateException("Corner facing must be horizontal");
        };
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF);
    }

    @SuppressWarnings("deprecation")
    private static final class LeavesCornerBlock extends BgeCornerBlock implements LeafDistanceCarrier {
        private static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
        private static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

        private LeavesCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DISTANCE, PERSISTENT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state == null ? null : LeafSemantics.applyPlayerPlacementState(state);
        }

        @Override public boolean isRandomlyTicking(BlockState state) {
            return LeafSemantics.isRandomlyTicking(state);
        }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) { LeafSemantics.decayIfNeeded(state, level, pos); }
        @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            level.setBlock(pos, LeafSemantics.updateDistanceFromLogs(state, level, pos), 3);
        }
        @Override protected BlockState updateShape(BlockState state, LevelReader level,
                ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
                BlockState neighbor, RandomSource random) {
            LeafSemantics.scheduleDistanceUpdate(state, level, ticks, pos, this, neighbor);
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }
        public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 1; }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            LeafSemantics.animateRainDrip(state, level, pos, random);
        }
    }

    private static final class SpreadableCornerBlock extends BgeCornerBlock implements SpreadableGeometry {
        private SpreadableCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
            if (state.getValue(WATERLOGGED)) return Exposure.BLOCKED;
            return state.getValue(HALF) == Half.BOTTOM ? Exposure.EXPOSED : Exposure.DEFAULT;
        }
    }

    private static final class PathCornerBlock extends BgeCornerBlock implements PathGeometry {
        private PathCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public boolean pathSurfaceRequiresClearAbove(BlockState state) {
            return state.getValue(HALF) == Half.TOP;
        }
    }

    private static final class CopperCornerBlock extends BgeCornerBlock
            implements ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
        private CopperCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public WeatheringCopper.WeatherState getAge() {
            return NibaruProviderAdapter.copperAge(this);
        }
        @Override public Optional<BlockState> getNext(BlockState state) {
            return NibaruProviderAdapter.nextOxidation(this, state);
        }
        @Override public float getChanceModifier() {
            return getAge() == WeatheringCopper.WeatherState.UNAFFECTED ? 0.75F : 1.0F;
        }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) { changeOverTime(state, level, pos, random); }
        @Override public boolean isRandomlyTicking(BlockState state) {
            return getAge() != WeatheringCopper.WeatherState.OXIDIZED;
        }
    }

    private static class FallingCornerBlock extends BgeCornerBlock implements Fallable {
        private FallingCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
    }

    private static final class ConcreteCornerBlock extends FallingCornerBlock {
        private ConcreteCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public void onLand(Level level, BlockPos pos, BlockState falling, BlockState current,
                FallingBlockEntity entity) {
            if (ConcretePowderSemantics.shouldHarden(level, pos, current)) {
                level.setBlock(pos, ConcretePowderSemantics.hardenedState(falling,
                        NibaruProviderAdapter.concreteHardening(this)), Block.UPDATE_ALL);
            }
        }
    }

    public static class AxisCornerBlock extends BgeCornerBlock {
        protected AxisCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
            registerDefaultState(defaultBlockState().setValue(MaterialAxisState.AXIS, Direction.Axis.Y));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(MaterialAxisState.AXIS);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState placed = super.getStateForPlacement(context);
            return placed.setValue(MaterialAxisState.AXIS, context.getClickedFace().getAxis());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
        }
    }

    public static class PatternCornerBlock extends BgeCornerBlock {
        protected PatternCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
            registerDefaultState(defaultBlockState().setValue(
                    GlazedPatternState.PATTERN_FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(GlazedPatternState.PATTERN_FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context).setValue(
                    GlazedPatternState.PATTERN_FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return super.rotate(state, rotation).setValue(GlazedPatternState.PATTERN_FACING,
                    rotation.rotate(state.getValue(GlazedPatternState.PATTERN_FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return super.mirror(state, mirror).setValue(GlazedPatternState.PATTERN_FACING,
                    mirror.mirror(state.getValue(GlazedPatternState.PATTERN_FACING)));
        }
    }

    public static final class AxisPatternCornerBlock extends AxisCornerBlock {
        private AxisPatternCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
            registerDefaultState(defaultBlockState().setValue(
                    GlazedPatternState.PATTERN_FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(GlazedPatternState.PATTERN_FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return super.getStateForPlacement(context).setValue(
                    GlazedPatternState.PATTERN_FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return super.rotate(state, rotation).setValue(GlazedPatternState.PATTERN_FACING,
                    rotation.rotate(state.getValue(GlazedPatternState.PATTERN_FACING)));
        }

        @Override
        public BlockState mirror(BlockState state, Mirror mirror) {
            return super.mirror(state, mirror).setValue(GlazedPatternState.PATTERN_FACING,
                    mirror.mirror(state.getValue(GlazedPatternState.PATTERN_FACING)));
        }
    }
}
