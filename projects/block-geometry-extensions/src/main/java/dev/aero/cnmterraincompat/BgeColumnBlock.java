package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.api.material.BehaviorCapability;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.block.concretepowder.ConcretePowderSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafDistanceCarrier;
import games.twinhead.moreslabsstairsandwalls.block.leaves.LeafSemantics;
import games.twinhead.moreslabsstairsandwalls.block.spreadable.SpreadableGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * BGE quarter Column: Step occupancy rotated into the vertical axis.
 *
 * <p>One blockspace holds either one 8x16x8 corner member or one terminal
 * opposite-diagonal pair. The six-state property is the entire occupancy truth;
 * no CNM combined marker participates.</p>
 */
public class BgeColumnBlock extends BgeProfiledGeometryBlock
        implements BlockspaceFundedGeometry {
    public static final EnumProperty<Occupancy> OCCUPANCY =
            EnumProperty.create("occupancy", Occupancy.class);

    private static final VoxelShape NW = Block.box(0, 0, 0, 8, 16, 8);
    private static final VoxelShape NE = Block.box(8, 0, 0, 16, 16, 8);
    private static final VoxelShape SW = Block.box(0, 0, 8, 8, 16, 16);
    private static final VoxelShape SE = Block.box(8, 0, 8, 16, 16, 16);
    private static final VoxelShape NW_SE = Shapes.or(NW, SE);
    private static final VoxelShape NE_SW = Shapes.or(NE, SW);

    public static BgeColumnBlock create(NibaruMaterialProfile profile,
            BlockBehaviour.Properties properties) {
        var capabilities = profile.capabilities();
        if (capabilities.contains(BehaviorCapability.LEAF_LIFECYCLE))
            return new LeavesColumnBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.PATH_CONVERSION))
            return new PathColumnBlock(profile, properties);
        if (isSpreadableSurface(profile))
            return new SpreadableColumnBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.OXIDIZABLE))
            return new CopperColumnBlock(profile, properties.randomTicks());
        if (capabilities.contains(BehaviorCapability.CONCRETE_HARDENING))
            return new ConcreteColumnBlock(profile, properties);
        if (capabilities.contains(BehaviorCapability.FALLING))
            return new FallingColumnBlock(profile, properties);
        if (capabilities.contains(BehaviorCapability.ICE_MELTING)) properties = properties.randomTicks();
        boolean axis = MaterialAxisState.applies(profile);
        boolean pattern = capabilities.contains(BehaviorCapability.GLAZED_ORIENTATION);
        if (axis && pattern) return new AxisPatternColumnBlock(profile, properties);
        if (axis) return new AxisColumnBlock(profile, properties);
        if (pattern) return new PatternColumnBlock(profile, properties);
        return new BgeColumnBlock(profile, properties);
    }

    @Override
    protected BgeGeometryRole geometryRole() {
        return BgeGeometryRole.QUARTER_COLUMN;
    }

    public BgeColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
        super(profile, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(OCCUPANCY, Occupancy.SW)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return has(BehaviorCapability.PATH_CONVERSION) || materialProfile().surfaceSamplingPolicy()
                == NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE
                ? occupancyShape(state.getValue(OCCUPANCY), 0, 15, 0)
                : switch (state.getValue(OCCUPANCY)) {
            case NW -> NW;
            case NE -> NE;
            case SW -> SW;
            case SE -> SE;
            case NW_SE -> NW_SE;
            case NE_SW -> NE_SW;
        };
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return expandedState(state, context) != null || super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existing = context.getLevel().getBlockState(pos);
        if (existing.is(this)) {
            BlockState expansion = expandedState(existing, context);
            return expansion == null ? null : applyProfilePlacement(expansion, context);
        }

        double localX = context.getClickLocation().x - pos.getX();
        double localZ = context.getClickLocation().z - pos.getZ();
        Occupancy occupancy = localX < 0.5
                ? localZ < 0.5 ? Occupancy.NW : Occupancy.SW
                : localZ < 0.5 ? Occupancy.NE : Occupancy.SE;
        return applyProfilePlacement(defaultBlockState()
                .setValue(OCCUPANCY, occupancy)
                .setValue(WATERLOGGED,
                        context.getLevel().getFluidState(pos).getType() == Fluids.WATER), context);
    }

    @Override
    protected VoxelShape materialCollisionShape(BlockState state, boolean honey) {
        return occupancyShape(state.getValue(OCCUPANCY), 0, honey ? 15 : 14, honey ? 1 : 0);
    }

    @Override
    protected boolean equalStateFaceCanCull(BlockState state, Direction direction) {
        return direction.getAxis() == Direction.Axis.Y;
    }

    private static VoxelShape occupancyShape(Occupancy occupancy, double minY,
            double maxY, double inset) {
        return switch (occupancy) {
            case NW -> quadrantShape(Occupancy.NW, minY, maxY, inset);
            case NE -> quadrantShape(Occupancy.NE, minY, maxY, inset);
            case SW -> quadrantShape(Occupancy.SW, minY, maxY, inset);
            case SE -> quadrantShape(Occupancy.SE, minY, maxY, inset);
            case NW_SE -> Shapes.or(quadrantShape(Occupancy.NW, minY, maxY, inset),
                    quadrantShape(Occupancy.SE, minY, maxY, inset));
            case NE_SW -> Shapes.or(quadrantShape(Occupancy.NE, minY, maxY, inset),
                    quadrantShape(Occupancy.SW, minY, maxY, inset));
        };
    }

    private static VoxelShape quadrantShape(Occupancy occupancy, double minY,
            double maxY, double inset) {
        return switch (occupancy) {
            case NW -> Block.box(inset, minY, inset, 8 - inset, maxY, 8 - inset);
            case NE -> Block.box(8 + inset, minY, inset, 16 - inset, maxY, 8 - inset);
            case SW -> Block.box(inset, minY, 8 + inset, 8 - inset, maxY, 16 - inset);
            case SE -> Block.box(8 + inset, minY, 8 + inset, 16 - inset, maxY, 16 - inset);
            case NW_SE, NE_SW -> throw new IllegalArgumentException("Expected singleton Column occupancy");
        };
    }

    @Override
    @Nullable
    public BlockState expandedState(BlockState existing, BlockPlaceContext context) {
        if (!existing.is(this) || !context.getItemInHand().is(asItem())) return null;
        Occupancy expanded = expandedOccupancy(existing.getValue(OCCUPANCY), context.getClickedFace());
        // A continuation target reached through the empty portion of this cell has the same
        // contextual form as CNM Step's !replacingClickedOnBlock path. The ray first hits the
        // backing block, then BlockPlaceContext resolves this occupied cell as its relative
        // target. There is no physical Column surface from which to derive a quadrant face, so
        // retain the only compatible terminal counterpart of the existing singleton instead.
        if (expanded == null && !context.replacingClickedOnBlock()) {
            expanded = oppositeDiagonal(existing.getValue(OCCUPANCY));
        }
        return expanded == null ? null : existing.setValue(OCCUPANCY, expanded);
    }

    @Nullable
    public static Occupancy expandedOccupancy(Occupancy occupancy, Direction clickedFace) {
        return switch (occupancy) {
            case NW -> clickedFace == Direction.EAST || clickedFace == Direction.SOUTH
                    ? Occupancy.NW_SE : null;
            case NE -> clickedFace == Direction.WEST || clickedFace == Direction.SOUTH
                    ? Occupancy.NE_SW : null;
            case SW -> clickedFace == Direction.EAST || clickedFace == Direction.NORTH
                    ? Occupancy.NE_SW : null;
            case SE -> clickedFace == Direction.WEST || clickedFace == Direction.NORTH
                    ? Occupancy.NW_SE : null;
            case NW_SE, NE_SW -> null;
        };
    }

    @Nullable
    private static Occupancy oppositeDiagonal(Occupancy occupancy) {
        return switch (occupancy) {
            case NW, SE -> Occupancy.NW_SE;
            case NE, SW -> Occupancy.NE_SW;
            case NW_SE, NE_SW -> null;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(OCCUPANCY, state.getValue(OCCUPANCY).rotate(rotation));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(OCCUPANCY, state.getValue(OCCUPANCY).mirror(mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OCCUPANCY);
    }

    @SuppressWarnings("deprecation")
    private static final class LeavesColumnBlock extends BgeColumnBlock implements LeafDistanceCarrier {
        private static final IntegerProperty DISTANCE = BlockStateProperties.DISTANCE;
        private static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

        private LeavesColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
            registerDefaultState(LeafSemantics.applyDefaultState(defaultBlockState()));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DISTANCE, PERSISTENT);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
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

    private static final class SpreadableColumnBlock extends BgeColumnBlock
            implements GeometryAwareSpreadable {
        private SpreadableColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
            return GeometrySurfaceExposure.topExposure(level, pos,
                    state.getValue(OCCUPANCY).topFootprintMask());
        }
    }

    private static final class PathColumnBlock extends BgeColumnBlock implements PathGeometry {
        private PathColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public boolean pathSurfaceRequiresClearAbove(BlockState state) { return true; }
    }

    private static final class CopperColumnBlock extends BgeColumnBlock
            implements ChangeOverTimeBlock<WeatheringCopper.WeatherState> {
        private CopperColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
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

    private static class FallingColumnBlock extends BgeColumnBlock implements Fallable {
        private FallingColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
    }

    private static final class ConcreteColumnBlock extends FallingColumnBlock {
        private ConcreteColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
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

    public enum Occupancy implements StringRepresentable {
        NW("north_west"),
        NE("north_east"),
        SW("south_west"),
        SE("south_east"),
        NW_SE("north_west_south_east"),
        NE_SW("north_east_south_west");

        private final String serializedName;

        Occupancy(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public boolean isDouble() {
            return this == NW_SE || this == NE_SW;
        }

        /** Exact upward material footprint on BGE's shared 2-by-2 quarter grid. */
        public int topFootprintMask() {
            return switch (this) {
                case NW -> GeometrySurfaceExposure.NORTH_WEST;
                case NE -> GeometrySurfaceExposure.NORTH_EAST;
                case SW -> GeometrySurfaceExposure.SOUTH_WEST;
                case SE -> GeometrySurfaceExposure.SOUTH_EAST;
                case NW_SE -> GeometrySurfaceExposure.NORTH_WEST
                        | GeometrySurfaceExposure.SOUTH_EAST;
                case NE_SW -> GeometrySurfaceExposure.NORTH_EAST
                        | GeometrySurfaceExposure.SOUTH_WEST;
            };
        }

        public Occupancy rotate(Rotation rotation) {
            return switch (rotation) {
                case NONE -> this;
                case CLOCKWISE_90 -> switch (this) {
                    case NW -> NE;
                    case NE -> SE;
                    case SE -> SW;
                    case SW -> NW;
                    case NW_SE -> NE_SW;
                    case NE_SW -> NW_SE;
                };
                case CLOCKWISE_180 -> switch (this) {
                    case NW -> SE;
                    case NE -> SW;
                    case SW -> NE;
                    case SE -> NW;
                    case NW_SE -> NW_SE;
                    case NE_SW -> NE_SW;
                };
                case COUNTERCLOCKWISE_90 -> switch (this) {
                    case NW -> SW;
                    case SW -> SE;
                    case SE -> NE;
                    case NE -> NW;
                    case NW_SE -> NE_SW;
                    case NE_SW -> NW_SE;
                };
            };
        }

        public Occupancy mirror(Mirror mirror) {
            return switch (mirror) {
                case NONE -> this;
                case LEFT_RIGHT -> switch (this) {
                    case NW -> SW;
                    case NE -> SE;
                    case SW -> NW;
                    case SE -> NE;
                    case NW_SE -> NE_SW;
                    case NE_SW -> NW_SE;
                };
                case FRONT_BACK -> switch (this) {
                    case NW -> NE;
                    case NE -> NW;
                    case SW -> SE;
                    case SE -> SW;
                    case NW_SE -> NE_SW;
                    case NE_SW -> NW_SE;
                };
            };
        }
    }

    public static class AxisColumnBlock extends BgeColumnBlock {
        protected AxisColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
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
            BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
            BlockState placed = super.getStateForPlacement(context);
            return placed == null ? null : placed.setValue(MaterialAxisState.AXIS,
                    MaterialAxisState.placementAxis(existing, this, context.getClickedFace()));
        }

        @Override
        public BlockState rotate(BlockState state, Rotation rotation) {
            return RotatedPillarBlock.rotatePillar(super.rotate(state, rotation), rotation);
        }
    }

    public static class PatternColumnBlock extends BgeColumnBlock {
        protected PatternColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
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
            BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
            BlockState placed = super.getStateForPlacement(context);
            if (placed == null) return null;
            Direction pattern = existing.is(this)
                    ? existing.getValue(GlazedPatternState.PATTERN_FACING)
                    : context.getHorizontalDirection().getOpposite();
            return placed.setValue(GlazedPatternState.PATTERN_FACING, pattern);
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

    public static final class AxisPatternColumnBlock extends AxisColumnBlock {
        private AxisPatternColumnBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
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
            BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
            BlockState placed = super.getStateForPlacement(context);
            if (placed == null) return null;
            Direction pattern = existing.is(this)
                    ? existing.getValue(GlazedPatternState.PATTERN_FACING)
                    : context.getHorizontalDirection().getOpposite();
            return placed.setValue(GlazedPatternState.PATTERN_FACING, pattern);
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
