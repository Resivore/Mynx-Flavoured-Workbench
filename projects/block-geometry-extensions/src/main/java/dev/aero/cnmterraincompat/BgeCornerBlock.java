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
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/** BGE Corner: Extended Block Shapes' full-height Vertical Stairs L geometry. */
public class BgeCornerBlock extends BgeProfiledGeometryBlock {
    /**
     * Cardinal carrier retained so C55 palettes continue to decode; each value maps to one
     * corner anchor and HALF is deliberately absent from the successor state definition.
     */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

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
                .setValue(FACING, Orientation.SOUTH_WEST.stateFacing())
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) {
        if (has(BehaviorCapability.PATH_CONVERSION) || materialProfile().surfaceSamplingPolicy()
                == NibaruMaterialProfile.SurfaceSamplingPolicy.PATH_LOWERED_SURFACE) return pathShape(state);
        return regularShape(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        double localX = context.getClickLocation().x - pos.getX();
        double localZ = context.getClickLocation().z - pos.getZ();
        Orientation facing = Orientation.fromHit(localX, localZ);
        return applyProfilePlacement(defaultBlockState()
                .setValue(FACING, facing.stateFacing())
                .setValue(WATERLOGGED,
                        context.getLevel().getFluidState(pos).getType() == Fluids.WATER), context);
    }

    @Override
    protected VoxelShape materialCollisionShape(BlockState state, boolean honey) {
        return lShape(orientation(state), honey ? 15 : 14, honey ? 1 : 0);
    }

    @Override
    protected boolean equalStateFaceCanCull(BlockState state, Direction direction) {
        return orientation(state).equalStateFaceCanCull(direction);
    }

    private static VoxelShape regularShape(BlockState state) {
        return lShape(orientation(state), 16, 0);
    }

    private static VoxelShape pathShape(BlockState state) {
        return lShape(orientation(state), 15, 0);
    }

    /** Full cube/collision shell minus the quarter diagonally opposite the clicked anchor. */
    private static VoxelShape lShape(Orientation facing, double maxY, double inset) {
        if (inset == 1) return honeyInsetShape(facing, maxY);
        if (inset != 0) throw new IllegalArgumentException("Corner inset must be zero or one pixel");
        return switch (facing) {
            case SOUTH_WEST -> Shapes.or(
                    Block.box(0, 0, 0, 8, maxY, 8),
                    Block.box(0, 0, 8, 16, maxY, 16)).optimize();
            case NORTH_WEST -> Shapes.or(
                    Block.box(0, 0, 0, 16, maxY, 8),
                    Block.box(0, 0, 8, 8, maxY, 16)).optimize();
            case NORTH_EAST -> Shapes.or(
                    Block.box(0, 0, 0, 16, maxY, 8),
                    Block.box(8, 0, 8, 16, maxY, 16)).optimize();
            case SOUTH_EAST -> Shapes.or(
                    Block.box(8, 0, 0, 16, maxY, 8),
                    Block.box(0, 0, 8, 16, maxY, 16)).optimize();
        };
    }

    /** One-pixel erosion of every exposed Honey boundary, including both notch faces. */
    private static VoxelShape honeyInsetShape(Orientation facing, double maxY) {
        return switch (facing) {
            case SOUTH_WEST -> Shapes.or(
                    Block.box(1, 0, 1, 7, maxY, 15),
                    Block.box(7, 0, 9, 15, maxY, 15)).optimize();
            case NORTH_WEST -> Shapes.or(
                    Block.box(1, 0, 1, 7, maxY, 15),
                    Block.box(7, 0, 1, 15, maxY, 7)).optimize();
            case NORTH_EAST -> Shapes.or(
                    Block.box(9, 0, 1, 15, maxY, 15),
                    Block.box(1, 0, 1, 9, maxY, 7)).optimize();
            case SOUTH_EAST -> Shapes.or(
                    Block.box(9, 0, 1, 15, maxY, 15),
                    Block.box(1, 0, 9, 9, maxY, 15)).optimize();
        };
    }

    public static Orientation orientation(BlockState state) {
        return Orientation.fromFacing(state.getValue(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, orientation(state).mirror(mirror).stateFacing());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /**
     * Four-corner anchor used by the reference Vertical Stairs form. The clicked/serialized
     * corner remains occupied and the diagonally opposite quarter is absent.
     */
    public enum Orientation implements StringRepresentable {
        SOUTH_WEST("south_west", Direction.WEST, GeometrySurfaceExposure.NORTH_EAST),
        NORTH_WEST("north_west", Direction.NORTH, GeometrySurfaceExposure.SOUTH_EAST),
        NORTH_EAST("north_east", Direction.EAST, GeometrySurfaceExposure.SOUTH_WEST),
        SOUTH_EAST("south_east", Direction.SOUTH, GeometrySurfaceExposure.NORTH_WEST);

        private final String serializedName;
        private final Direction stateFacing;
        private final int removedQuarterMask;

        Orientation(String serializedName, Direction stateFacing, int removedQuarterMask) {
            this.serializedName = serializedName;
            this.stateFacing = stateFacing;
            this.removedQuarterMask = removedQuarterMask;
        }

        public static Orientation fromHit(double localX, double localZ) {
            return localX < 0.5
                    ? localZ < 0.5 ? NORTH_WEST : SOUTH_WEST
                    : localZ < 0.5 ? NORTH_EAST : SOUTH_EAST;
        }

        public int topFootprintMask() {
            return GeometrySurfaceExposure.FULL ^ removedQuarterMask;
        }

        public int removedQuarterMask() {
            return removedQuarterMask;
        }

        public Direction stateFacing() {
            return stateFacing;
        }

        public static Orientation fromFacing(Direction facing) {
            return switch (facing) {
                case WEST -> SOUTH_WEST;
                case NORTH -> NORTH_WEST;
                case EAST -> NORTH_EAST;
                case SOUTH -> SOUTH_EAST;
                default -> throw new IllegalArgumentException("Corner facing must be horizontal: " + facing);
            };
        }

        public Orientation rotate(Rotation rotation) {
            return switch (rotation) {
                case NONE -> this;
                case CLOCKWISE_90 -> clockwise();
                case CLOCKWISE_180 -> clockwise().clockwise();
                case COUNTERCLOCKWISE_90 -> clockwise().clockwise().clockwise();
            };
        }

        private Orientation clockwise() {
            return switch (this) {
                case SOUTH_WEST -> NORTH_WEST;
                case NORTH_WEST -> NORTH_EAST;
                case NORTH_EAST -> SOUTH_EAST;
                case SOUTH_EAST -> SOUTH_WEST;
            };
        }

        public Orientation mirror(Mirror mirror) {
            return switch (mirror) {
                case NONE -> this;
                case LEFT_RIGHT -> switch (this) {
                    case SOUTH_WEST -> NORTH_WEST;
                    case NORTH_WEST -> SOUTH_WEST;
                    case SOUTH_EAST -> NORTH_EAST;
                    case NORTH_EAST -> SOUTH_EAST;
                };
                case FRONT_BACK -> switch (this) {
                    case SOUTH_WEST -> SOUTH_EAST;
                    case SOUTH_EAST -> SOUTH_WEST;
                    case NORTH_WEST -> NORTH_EAST;
                    case NORTH_EAST -> NORTH_WEST;
                };
            };
        }

        /** Equal-state translucent culling only where the adjacent L covers this whole boundary. */
        private boolean equalStateFaceCanCull(Direction direction) {
            if (direction.getAxis() == Direction.Axis.Y) return true;
            return switch (this) {
                case SOUTH_WEST -> direction == Direction.NORTH || direction == Direction.EAST;
                case NORTH_WEST -> direction == Direction.SOUTH || direction == Direction.EAST;
                case NORTH_EAST -> direction == Direction.SOUTH || direction == Direction.WEST;
                case SOUTH_EAST -> direction == Direction.NORTH || direction == Direction.WEST;
            };
        }

        /** Orientation in a glazed pattern's unrotated model frame. */
        public Orientation relativeTo(Direction patternFacing) {
            int turns = GlazedPatternState.patternYaw(patternFacing) / 90;
            Orientation result = this;
            for (int i = 0; i < turns; i++) {
                result = result.rotate(Rotation.COUNTERCLOCKWISE_90);
            }
            return result;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
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

    private static final class SpreadableCornerBlock extends BgeCornerBlock
            implements GeometryAwareSpreadable {
        private SpreadableCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public Exposure spreadableExposure(BlockState state, LevelReader level, BlockPos pos) {
            return GeometrySurfaceExposure.topExposure(level, pos,
                    orientation(state).topFootprintMask());
        }
    }

    private static final class PathCornerBlock extends BgeCornerBlock implements PathGeometry {
        private PathCornerBlock(NibaruMaterialProfile profile, BlockBehaviour.Properties properties) {
            super(profile, properties);
        }
        @Override public boolean pathSurfaceRequiresClearAbove(BlockState state) { return true; }
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
