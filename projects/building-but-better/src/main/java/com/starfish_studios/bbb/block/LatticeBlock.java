package com.starfish_studios.bbb.block;

import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import com.starfish_studios.bbb.block.properties.LatticePlantType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class LatticeBlock extends Block implements SimpleWaterloggedBlock, BonemealableBlock, CaveVines {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<LatticePlantType> PLANT_TYPE = BBBBlockStateProperties.PLANT_TYPE;
    public static final BooleanProperty LEFT = BBBBlockStateProperties.LEFT;
    public static final BooleanProperty MIDDLE = BBBBlockStateProperties.MIDDLE;
    public static final BooleanProperty RIGHT = BBBBlockStateProperties.RIGHT;

    public static final VoxelShape NORTH_AABB = Block.box(0, 0, 14, 16, 16, 16);
    public static final VoxelShape NORTH_PLANT_AABB = Block.box(0, 0, 13, 16, 16, 14);
    public static final VoxelShape SOUTH_AABB = Block.box(0, 0, 0, 16, 16, 2);
    public static final VoxelShape SOUTH_PLANT_AABB = Block.box(0, 0, 2, 16, 16, 3);
    public static final VoxelShape WEST_AABB = Block.box(14, 0, 0, 16, 16, 16);
    public static final VoxelShape WEST_PLANT_AABB = Block.box(13, 0, 0, 14, 16, 16);
    public static final VoxelShape EAST_AABB = Block.box(0, 0, 0, 2, 16, 16);
    public static final VoxelShape EAST_PLANT_AABB = Block.box(2, 0, 0, 3, 16, 16);

    private static final Map<LatticePlantType, Item> PLANT_TYPE_TO_ITEM = createPlantMap();

    private static Map<LatticePlantType, Item> createPlantMap() {
        EnumMap<LatticePlantType, Item> map = new EnumMap<>(LatticePlantType.class);
        map.put(LatticePlantType.VINES, Items.VINE);
        map.put(LatticePlantType.OAK_LEAVES, Items.OAK_LEAVES);
        map.put(LatticePlantType.SPRUCE_LEAVES, Items.SPRUCE_LEAVES);
        map.put(LatticePlantType.BIRCH_LEAVES, Items.BIRCH_LEAVES);
        map.put(LatticePlantType.JUNGLE_LEAVES, Items.JUNGLE_LEAVES);
        map.put(LatticePlantType.ACACIA_LEAVES, Items.ACACIA_LEAVES);
        map.put(LatticePlantType.DARK_OAK_LEAVES, Items.DARK_OAK_LEAVES);
        map.put(LatticePlantType.AZALEA_LEAVES, Items.AZALEA_LEAVES);
        map.put(LatticePlantType.FLOWERING_AZALEA_LEAVES, Items.FLOWERING_AZALEA_LEAVES);
        map.put(LatticePlantType.MANGROVE_LEAVES, Items.MANGROVE_LEAVES);
        map.put(LatticePlantType.CHERRY_LEAVES, Items.CHERRY_LEAVES);
        map.put(LatticePlantType.GLOW_LICHEN, Items.GLOW_LICHEN);
        map.put(LatticePlantType.CAVE_VINES, Items.GLOW_BERRIES);
        return Map.copyOf(map);
    }

    public LatticeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(LEFT, true)
                .setValue(MIDDLE, true)
                .setValue(RIGHT, true)
                .setValue(PLANT_TYPE, LatticePlantType.NONE)
                .setValue(BERRIES, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape lattice = switch (state.getValue(FACING)) {
            case NORTH -> NORTH_AABB;
            case SOUTH -> SOUTH_AABB;
            case WEST -> WEST_AABB;
            case EAST -> EAST_AABB;
            default -> Shapes.empty();
        };
        if (state.getValue(PLANT_TYPE) == LatticePlantType.NONE) {
            return lattice;
        }
        return Shapes.or(lattice, plantShape(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PLANT_TYPE) == LatticePlantType.NONE
                ? Shapes.empty()
                : plantShape(state.getValue(FACING));
    }

    private static VoxelShape plantShape(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_PLANT_AABB;
            case SOUTH -> SOUTH_PLANT_AABB;
            case WEST -> WEST_PLANT_AABB;
            case EAST -> EAST_PLANT_AABB;
            default -> Shapes.empty();
        };
    }

    private BlockState getConnections(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        Direction leftDirection = facing.getClockWise();
        Direction rightDirection = facing.getCounterClockWise();
        boolean left = !validConnection(level.getBlockState(pos.relative(leftDirection)), level, pos.relative(leftDirection));
        boolean right = !validConnection(level.getBlockState(pos.relative(rightDirection)), level, pos.relative(rightDirection));
        return state.setValue(LEFT, left).setValue(RIGHT, right);
    }

    private boolean validConnection(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (state.isFaceSturdy(level, pos, direction)) {
                return true;
            }
        }
        return state.is(this);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.SHEARS)) {
            removePlant(state, level, pos);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && stack.is(Items.SHEARS)
                && state.getValue(PLANT_TYPE) != LatticePlantType.NONE) {
            if (state.getValue(PLANT_TYPE) == LatticePlantType.CAVE_VINES && state.getValue(BERRIES)) {
                level.setBlock(pos, state.setValue(PLANT_TYPE, LatticePlantType.NONE), Block.UPDATE_ALL);
                return CaveVines.use(null, state, level, pos);
            }
            removePlant(state, level, pos);
            return InteractionResult.SUCCESS_SERVER;
        }

        if (state.getValue(PLANT_TYPE) == LatticePlantType.NONE) {
            LatticePlantType plantType = plantTypeFor(stack.getItem());
            if (plantType != null) {
                level.setBlock(pos, state.setValue(PLANT_TYPE, plantType), Block.UPDATE_ALL);
                level.playSound(null, pos,
                        plantType == LatticePlantType.VINES ? SoundEvents.VINE_PLACE : SoundEvents.GRASS_PLACE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        }

        if (state.getValue(PLANT_TYPE) == LatticePlantType.CAVE_VINES) {
            return CaveVines.use(null, state, level, pos);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static LatticePlantType plantTypeFor(Item item) {
        for (Map.Entry<LatticePlantType, Item> entry : PLANT_TYPE_TO_ITEM.entrySet()) {
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void removePlant(BlockState state, Level level, BlockPos pos) {
        LatticePlantType plantType = state.getValue(PLANT_TYPE);
        Item item = PLANT_TYPE_TO_ITEM.get(plantType);
        if (item == null) return;
        level.setBlock(pos, state.setValue(PLANT_TYPE, LatticePlantType.NONE), Block.UPDATE_ALL);
        popResource(level, pos, new ItemStack(item));
        level.playSound(null, pos, SoundEvents.VINE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !state.getValue(BERRIES) && state.getValue(PLANT_TYPE) == LatticePlantType.CAVE_VINES;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_CLIENTS);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return getConnections(state, level, pos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, LEFT, RIGHT, MIDDLE, PLANT_TYPE, BERRIES);
    }
}
