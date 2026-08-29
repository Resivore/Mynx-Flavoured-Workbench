package com.fizzware.dramaticdoors.blocks;

import org.jetbrains.annotations.Nullable;

import com.fizzware.dramaticdoors.DramaticDoors;
import com.fizzware.dramaticdoors.compat.Compats;
import com.fizzware.dramaticdoors.compat.registries.SupplementariesCompat;
import com.fizzware.dramaticdoors.registry.DDNames;
import com.fizzware.dramaticdoors.tags.DDBlockTags;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class ShortDoorBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<ShortDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308821_ -> p_308821_.group(BlockSetType.CODEC.fieldOf("block_set_type").forGetter(ShortDoorBlock::type), propertiesCodec()).apply(p_308821_, ShortDoorBlock::new)
    );
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
    private final BlockSetType type;
	
    public static final Identifier TOOTH_DOOR_RES = Identifier.fromNamespaceAndPath(DramaticDoors.MOD_ID, DDNames.SHORT_TOOTH);
    
    public ShortDoorBlock(BlockSetType blockset, Block from) {
        this(blockset, from, null);
    }
    
	public ShortDoorBlock(BlockSetType blockset, Block from, @Nullable FeatureFlag flag) {
    	super(flag != null ? Properties.ofFullCopy(from).requiredFeatures(flag) : Properties.ofFullCopy(from));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.FALSE).setValue(HINGE, DoorHingeSide.LEFT).setValue(POWERED, Boolean.FALSE).setValue(WATERLOGGED, Boolean.FALSE));
        this.type = blockset;
	}
	
    public ShortDoorBlock(BlockSetType blockset, Properties properties) {
    	super(properties.sound(blockset.soundType()));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.FALSE).setValue(HINGE, DoorHingeSide.LEFT).setValue(POWERED, Boolean.FALSE).setValue(WATERLOGGED, Boolean.FALSE));
        this.type = blockset;
    }

    @Override
    protected BlockState updateShape(BlockState stateIn, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        return !canSurvive(stateIn, level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(stateIn, level, tickAccess, currentPos, facing, facingPos, facingState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        if (blockpos.getY() <= context.getLevel().getMaxY() && context.getLevel().getBlockState(blockpos).canBeReplaced(context)) {
            Level level = context.getLevel();
            Direction face = context.getHorizontalDirection();
            DoorHingeSide hinge = this.getHinge(context);
            boolean flag = level.hasNeighborSignal(blockpos) || level.hasNeighborSignal(blockpos.above());
            boolean waterfilled = level.getFluidState(blockpos).getType() == Fluids.WATER; 
            if (context.getClickedFace() == context.getHorizontalDirection().getOpposite() && context.getClickedFace().getAxis().isHorizontal()) { // Flip the direction.
            	face = face.getOpposite();
            	hinge = hinge == DoorHingeSide.LEFT ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT; // Flip the hinge.
            }
            return this.defaultBlockState().setValue(FACING, face).setValue(HINGE, hinge).setValue(POWERED, flag).setValue(OPEN, flag).setValue(WATERLOGGED, waterfilled);
        } else {
            return null;
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {}

    protected DoorHingeSide getHinge(BlockPlaceContext context) {
        BlockGetter BlockGetter = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction behindDir = context.getHorizontalDirection(); // Behind
        Direction leftDir = behindDir.getCounterClockWise(); // Left
        BlockPos leftPos = placePos.relative(leftDir); // Left Pos
        BlockState leftBlockstate = BlockGetter.getBlockState(leftPos); // Left BlockState
        Direction rightDir = behindDir.getClockWise(); // Right
        BlockPos rightPos = placePos.relative(rightDir); // Right Pos
        BlockState rightBlockstate = BlockGetter.getBlockState(rightPos); // Right Blockstate
        int i = (leftBlockstate.isCollisionShapeFullBlock(BlockGetter, leftPos) ? -1 : 0) + (rightBlockstate.isCollisionShapeFullBlock(BlockGetter, rightPos) ? 1 : 0);
        boolean leftIsLowerOfSameType = leftBlockstate.getBlock() == this;
        boolean rightIsLowerOfSameType = rightBlockstate.getBlock() == this;
        if ((!leftIsLowerOfSameType || rightIsLowerOfSameType) && i <= 0) {
            if ((!rightIsLowerOfSameType || leftIsLowerOfSameType) && i >= 0) {
                int j = behindDir.getStepX();
                int k = behindDir.getStepZ();
                Vec3 vec3d = context.getClickLocation();
                double d0 = vec3d.x - (double)placePos.getX();
                double d1 = vec3d.z - (double)placePos.getZ();
                return (j >= 0 || !(d1 < 0.5D)) && (j <= 0 || !(d1 > 0.5D)) && (k >= 0 || !(d0 > 0.5D)) && (k <= 0 || !(d0 < 0.5D)) ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            } else {
                return DoorHingeSide.LEFT;
            }
        } else {
            return DoorHingeSide.RIGHT;
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (this.type() == BlockSetType.COPPER && itemstack.getItem() instanceof AxeItem && player.isCrouching()) {
        	if (ShortWeatheringDoorBlock.getUnwaxed(state).isPresent()) {
        		BlockState newstate = ShortWeatheringDoorBlock.getUnwaxed(state).get();
	            if (player instanceof ServerPlayer) {
	                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, pos, itemstack);
	            }
	            level.setBlock(pos, newstate, 11);
	            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newstate));
	            level.levelEvent(player, 3003, pos, 0);
				itemstack.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        		return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        	}
		}
    	if (!this.type.canOpenByHand() && !state.is(DDBlockTags.MOB_INTERACTABLE_SHORT_DOORS)) {
            return InteractionResult.PASS;
        } 
    	else {
        	if (Compats.SUPPLEMENTARIES_INSTALLED && (this == SupplementariesCompat.SHORT_GOLD_DOOR && state.getValue(POWERED))/* || (this == SupplementariesCompat.SHORT_SILVER_DOOR && !state.getValue(POWERED))*/) {
        		return InteractionResult.PASS;
        	}
        	tryOpenDoubleDoor(level, state, pos);
            state = state.cycle(OPEN);
            level.setBlock(pos, state, 10);
	        this.playSound(player, level, pos, state.getValue(OPEN));
            level.gameEvent(player, state.getValue(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
			if (state.getValue(WATERLOGGED)) {
				level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
			}
            if (Compats.BLUEPRINT_INSTALLED && this == BuiltInRegistries.BLOCK.getValue(TOOTH_DOOR_RES)) {
            	level.scheduleTick(pos, this, 20);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
    }

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!Compats.BLUEPRINT_INSTALLED) {
			return;
		}
		if (this != BuiltInRegistries.BLOCK.getValue(TOOTH_DOOR_RES)) {
			return; // Should stop the 'dancing' doors.
		}
		if (!level.isClientSide()) {
			state = state.cycle(OPEN);
			level.setBlock(pos, state, 10);
			this.playSound(null, level, pos, state.getValue(OPEN));
		}
	}
    
    public void toggleDoor(Level level, BlockPos pos, boolean open) {
        BlockState blockstate = level.getBlockState(pos);
        if (blockstate.getBlock() == this && blockstate.getValue(OPEN) != open) {
            level.setBlock(pos, blockstate.setValue(OPEN, open), 10);
            this.playSound(null, level, pos, open);
        }
    }

	public boolean isOpen(BlockState state) {
		return state.getValue(OPEN);
	}

	public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
		if (state.is(this) && state.getValue(OPEN) != open) {
			level.setBlock(pos, state.setValue(OPEN, Boolean.valueOf(open)), 10);
			this.playSound(entity, level, pos, open);
			level.gameEvent(entity, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
			tryOpenDoubleDoor(level, state, pos);
			// If there is a short door above, also try to open that half.
			BlockState stateAbove = level.getBlockState(pos.above(1));
			if (stateAbove.is(DDBlockTags.SHORT_WOODEN_DOORS)) {
				boolean isNight = level.getSkyDarken() >= 4;
				if ((isNight && (open || stateAbove.getValue(OPEN))) || (!isNight && !stateAbove.getValue(OPEN))) {
					level.setBlock(pos.above(), stateAbove.setValue(OPEN, Boolean.valueOf(open)), 10);
				}
			}
		}
 	}
    
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, Orientation orientation, boolean isMoving) {
        boolean flag = level.hasNeighborSignal(pos);
        if (blockIn != this && flag != state.getValue(POWERED)) {
        	if (Compats.SUPPLEMENTARIES_INSTALLED && this == SupplementariesCompat.SHORT_GOLD_DOOR/* || this == SupplementariesCompat.SHORT_SILVER_DOOR*/) {
        		level.setBlock(pos, state.setValue(POWERED, flag), 2);
        	}
        	else {
	            if (flag != state.getValue(OPEN)) {
	                this.playSound(null, level, pos, flag);
	            }
	            tryOpenDoubleDoor(level, state, pos);
	            level.setBlock(pos, state.setValue(POWERED, flag).setValue(OPEN, flag), Block.UPDATE_CLIENTS);
				if (state.getValue(WATERLOGGED)) {
					level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
				}
        	}
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    	return true;
        /*boolean result;
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        BlockPos wallPos = pos.relative(state.getValue(FACING).getOpposite(), 1);
        BlockState wallState = level.getBlockState(wallPos);
        result = belowState.isFaceSturdy(level, below, Direction.UP) || belowState.is(DDItemTags.SHORT_DOORS) || wallState.isFaceSturdy(level, wallPos, state.getValue(FACING).getOpposite());
        return result;*/
    }

    protected void playSound(@Nullable Entity entity, Level level, BlockPos pos, boolean isOpen) {
        level.playSound(entity, pos, isOpen ? this.type.doorOpen() : this.type.doorClose(), SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        return Mth.getSeed(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HINGE, POWERED, WATERLOGGED);
    }

    @Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}
    
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean flag = !state.getValue(OPEN);
        boolean flag1 = state.getValue(HINGE) == DoorHingeSide.RIGHT;
        switch(direction) {
            case EAST:
            default:
                return flag ? EAST_AABB : (flag1 ? NORTH_AABB : SOUTH_AABB);
            case SOUTH:
                return flag ? SOUTH_AABB : (flag1 ? EAST_AABB : WEST_AABB);
            case WEST:
                return flag ? WEST_AABB : (flag1 ? SOUTH_AABB : NORTH_AABB);
            case NORTH:
                return flag ? NORTH_AABB : (flag1 ? WEST_AABB : EAST_AABB);
        }
    }

    public boolean allowsMovement(BlockState state, BlockGetter level, BlockPos pos, PathType type) {
        switch(type) {
            case WALKABLE:
                return state.getValue(OPEN);
            case WATER:
                return false;
            case OPEN:
                return state.getValue(OPEN);
            default:
                return false;
        }
    }

    public PushReaction getPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return mirrorIn == Mirror.NONE ? state : state.rotate(mirrorIn.getRotation(state.getValue(FACING))).cycle(HINGE);
    }

    public static boolean isWoodenDoor(Level level, BlockPos pos) {
    	return isWoodenDoor(level.getBlockState(pos));
    }

	public static boolean isWoodenDoor(BlockState state) {
		return state.getBlock() instanceof ShortDoorBlock && (state.is(DDBlockTags.SHORT_WOODEN_DOORS));
	}
	
	public static boolean isMobInteractable(Level level, BlockPos pos) {
		return isMobInteractable(level.getBlockState(pos));
	}
	
	public static boolean isMobInteractable(BlockState state) {
		return state.getBlock() instanceof ShortDoorBlock && (state.is(DDBlockTags.MOB_INTERACTABLE_SHORT_DOORS));
	}
    
    //Double Doors Compatibility
	public static void tryOpenDoubleDoor(Level world, BlockState state, BlockPos pos) {
        if (Compats.DOUBLE_DOORS_INSTALLED || Compats.COUPLINGS_INSTALLED) {
            Direction direction = state.getValue(ShortDoorBlock.FACING);
            boolean isOpen = state.getValue(ShortDoorBlock.OPEN);
            DoorHingeSide isMirrored = state.getValue(ShortDoorBlock.HINGE);
            BlockPos mirrorPos = pos.relative(isMirrored == DoorHingeSide.RIGHT ? direction.getCounterClockWise() : direction.getClockWise());
            BlockPos doorPos = mirrorPos;
            BlockState other = world.getBlockState(doorPos);
            if (other.getBlock() == state.getBlock() && other.getValue(ShortDoorBlock.FACING) == direction && !other.getValue(ShortDoorBlock.POWERED) &&  other.getValue(ShortDoorBlock.OPEN) == isOpen && other.getValue(ShortDoorBlock.HINGE) != isMirrored) {
                BlockState newState = other.cycle(ShortDoorBlock.OPEN);
                world.setBlock(doorPos, newState, 10);
            }
        }
    }
	
	public BlockSetType type() {
		return this.type;
	}
}
