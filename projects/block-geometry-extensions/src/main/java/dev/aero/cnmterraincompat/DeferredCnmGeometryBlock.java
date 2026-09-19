package dev.aero.cnmterraincompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Registry-time BGE tail geometry for a real CNM admission whose canonical material has not yet
 * been selected.  It deliberately contains only geometry state: the resolved material, drops,
 * resources, and ShapeMap membership are installed by {@link CnmShapeMapCandidateBridge}.
 *
 * <p>This is intentionally a generic normal-material carrier. Special material behavior remains
 * owned by the existing typed Nibaru catalog, which is selected whenever a resolved parent has a
 * profile. An untyped candidate is inaccessible unless CNM later resolves its exact component.</p>
 */
final class DeferredCnmGeometryBlock extends Block implements SimpleWaterloggedBlock,
        BlockspaceFundedGeometry {
    private static final ThreadLocal<BgeGeometryRole> CONSTRUCTING_ROLE = new ThreadLocal<>();
    private final BgeGeometryRole role;

    static DeferredCnmGeometryBlock create(BgeGeometryRole role, BlockBehaviour.Properties properties) {
        CONSTRUCTING_ROLE.set(Objects.requireNonNull(role, "role"));
        try {
            return new DeferredCnmGeometryBlock(properties);
        } finally {
            CONSTRUCTING_ROLE.remove();
        }
    }

    private DeferredCnmGeometryBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BgeGeometryRole role = constructionRole();
        if (role != BgeGeometryRole.LAYER && role != BgeGeometryRole.CORNER
                && role != BgeGeometryRole.QUARTER_COLUMN) {
            throw new IllegalArgumentException("Deferred CNM geometry must be a BGE tail role: " + role);
        }
        this.role = role;
        BlockState state = stateDefinition.any().setValue(BgeProfiledGeometryBlock.WATERLOGGED, false);
        state = switch (role) {
            case LAYER -> state.setValue(BgeLayerBlock.FACING, Direction.UP)
                    .setValue(BgeLayerBlock.LAYERS, 1).setValue(BgeLayerBlock.DOUBLE, false);
            case CORNER -> state.setValue(BgeCornerBlock.FACING, Direction.SOUTH);
            case QUARTER_COLUMN -> state.setValue(BgeColumnBlock.OCCUPANCY,
                    BgeColumnBlock.Occupancy.SW);
            default -> throw new IllegalStateException("Unexpected deferred role " + role);
        };
        registerDefaultState(state);
    }

    BgeGeometryRole role() {
        return role;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        switch (constructionRole()) {
            case LAYER -> builder.add(BgeLayerBlock.FACING, BgeLayerBlock.LAYERS,
                    BgeLayerBlock.DOUBLE, BgeProfiledGeometryBlock.WATERLOGGED);
            case CORNER -> builder.add(BgeCornerBlock.FACING, BgeProfiledGeometryBlock.WATERLOGGED);
            case QUARTER_COLUMN -> builder.add(BgeColumnBlock.OCCUPANCY,
                    BgeProfiledGeometryBlock.WATERLOGGED);
            default -> throw new IllegalStateException("Unexpected deferred role " + role);
        }
    }

    private static BgeGeometryRole constructionRole() {
        return Objects.requireNonNull(CONSTRUCTING_ROLE.get(), "deferred geometry construction role");
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) return expandedState(existing, context);
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return switch (role) {
            case LAYER -> defaultBlockState().setValue(BgeLayerBlock.FACING, context.getClickedFace())
                    .setValue(BgeLayerBlock.WATERLOGGED, waterlogged);
            case CORNER -> defaultBlockState().setValue(BgeCornerBlock.FACING,
                    context.getHorizontalDirection().getOpposite()).setValue(
                    BgeProfiledGeometryBlock.WATERLOGGED, waterlogged);
            case QUARTER_COLUMN -> defaultBlockState().setValue(BgeColumnBlock.OCCUPANCY,
                    occupancy(context)).setValue(BgeProfiledGeometryBlock.WATERLOGGED, waterlogged);
            default -> null;
        };
    }

    @Override
    @Nullable
    public BlockState expandedState(BlockState existing, BlockPlaceContext context) {
        if (!existing.is(this) || !context.getItemInHand().is(asItem())) return null;
        if (role == BgeGeometryRole.LAYER && context.getClickedFace() == existing.getValue(BgeLayerBlock.FACING)
                && existing.getValue(BgeLayerBlock.LAYERS) < 4) {
            return existing.setValue(BgeLayerBlock.LAYERS, existing.getValue(BgeLayerBlock.LAYERS) + 1);
        }
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return switch (role) {
            case LAYER -> layerShape(state);
            case CORNER -> cornerShape(state.getValue(BgeCornerBlock.FACING));
            case QUARTER_COLUMN -> columnShape(state.getValue(BgeColumnBlock.OCCUPANCY));
            default -> Shapes.empty();
        };
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BgeProfiledGeometryBlock.WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (state.getValue(BgeProfiledGeometryBlock.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    private static BgeColumnBlock.Occupancy occupancy(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        double x = context.getClickLocation().x - pos.getX();
        double z = context.getClickLocation().z - pos.getZ();
        return x < 0.5 ? (z < 0.5 ? BgeColumnBlock.Occupancy.NW : BgeColumnBlock.Occupancy.SW)
                : (z < 0.5 ? BgeColumnBlock.Occupancy.NE : BgeColumnBlock.Occupancy.SE);
    }

    private static VoxelShape layerShape(BlockState state) {
        int depth = state.getValue(BgeLayerBlock.LAYERS) * 4;
        return switch (state.getValue(BgeLayerBlock.FACING)) {
            case UP -> Block.box(0, 0, 0, 16, depth, 16);
            case DOWN -> Block.box(0, 16 - depth, 0, 16, 16, 16);
            case NORTH -> Block.box(0, 0, 0, 16, 16, depth);
            case EAST -> Block.box(16 - depth, 0, 0, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 16 - depth, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, depth, 16, 16);
        };
    }

    private static VoxelShape cornerShape(Direction facing) {
        VoxelShape northwest = Block.box(0, 0, 0, 8, 16, 8);
        VoxelShape northeast = Block.box(8, 0, 0, 16, 16, 8);
        VoxelShape southwest = Block.box(0, 0, 8, 8, 16, 16);
        VoxelShape southeast = Block.box(8, 0, 8, 16, 16, 16);
        return switch (facing) {
            case NORTH -> Shapes.or(northwest, northeast, southwest);
            case EAST -> Shapes.or(northeast, southeast, northwest);
            case SOUTH -> Shapes.or(southwest, southeast, northeast);
            case WEST -> Shapes.or(northwest, southwest, southeast);
            default -> throw new IllegalStateException("Deferred Corner facing must be horizontal");
        };
    }

    private static VoxelShape columnShape(BgeColumnBlock.Occupancy occupancy) {
        VoxelShape northwest = Block.box(0, 0, 0, 8, 16, 8);
        VoxelShape northeast = Block.box(8, 0, 0, 16, 16, 8);
        VoxelShape southwest = Block.box(0, 0, 8, 8, 16, 16);
        VoxelShape southeast = Block.box(8, 0, 8, 16, 16, 16);
        return switch (occupancy) {
            case NW -> northwest;
            case NE -> northeast;
            case SW -> southwest;
            case SE -> southeast;
            case NW_SE -> Shapes.or(northwest, southeast);
            case NE_SW -> Shapes.or(northeast, southwest);
        };
    }
}
