package dev.aero.cnmterraincompat;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla 26.2 farmland lifecycle carried by horizontal slab geometry only. */
@SuppressWarnings("deprecation")
public final class FarmlandSlabBlock extends Block {
    public static final IntegerProperty MOISTURE = BlockStateProperties.MOISTURE;
    public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
    public static final int MAX_MOISTURE = 7;

    public static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 7, 16);
    public static final VoxelShape TOP_SHAPE = Block.box(0, 7, 0, 16, 15, 16);
    public static final VoxelShape DOUBLE_SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public FarmlandSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MOISTURE, 0)
                .setValue(TYPE, SlabType.BOTTOM));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (direction == Direction.UP && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !above.isSolid() || shouldMaintainFarmland(level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            case BOTTOM -> BOTTOM_SHAPE;
            case TOP -> TOP_SHAPE;
            case DOUBLE -> DOUBLE_SHAPE;
        };
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) turnToDirt(null, state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int moisture = state.getValue(MOISTURE);
        if (isNearWater(level, pos) || level.isRainingAt(pos.above())) {
            if (moisture < MAX_MOISTURE) {
                level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), Block.UPDATE_CLIENTS);
            }
        } else if (moisture > 0) {
            level.setBlock(pos, state.setValue(MOISTURE, moisture - 1), Block.UPDATE_CLIENTS);
        } else if (!shouldMaintainFarmland(level, pos)) {
            turnToDirt(null, state, level, pos);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level instanceof ServerLevel server
                && level.getRandom().nextFloat() < fallDistance - 0.5
                && entity instanceof LivingEntity
                && (entity instanceof Player || server.getGameRules().get(GameRules.MOB_GRIEFING))
                && entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > 0.512F) {
            turnToDirt(entity, state, level, pos);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    /** Converts to the canonical native Dirt slab while preserving horizontal geometry. */
    public static void turnToDirt(Entity source, BlockState farmland, Level level, BlockPos pos) {
        BlockState dirt = ModBlocks.DIRT.getBlock(ModBlocks.BlockType.SLAB).defaultBlockState()
                .setValue(SlabBlock.TYPE, farmland.getValue(TYPE))
                .setValue(SlabBlock.WATERLOGGED, false);
        BlockState target = Block.pushEntitiesUp(farmland, dirt, level, pos);
        level.setBlockAndUpdate(pos, target);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(source, target));
    }

    private static boolean shouldMaintainFarmland(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.MAINTAINS_FARMLAND);
    }

    private static boolean isNearWater(LevelReader level, BlockPos pos) {
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(nearby).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOISTURE, TYPE);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
