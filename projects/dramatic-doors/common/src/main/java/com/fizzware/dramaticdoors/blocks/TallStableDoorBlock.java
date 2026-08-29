package com.fizzware.dramaticdoors.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallStableDoorBlock extends TallDoorBlock
{
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_OPEN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D).move(-0.75D, 0.0D, 0.0D);
    protected static final VoxelShape NORTH_OPEN_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D).move(0.75D, 0.0D, 0.0D);
    protected static final VoxelShape WEST_OPEN_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D).move(0.0D, 0.0D, -0.75D);
    protected static final VoxelShape EAST_OPEN_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D).move(0.0D, 0.0D, 0.75D);
    protected static final VoxelShape SOUTH_OPEN_LEFT_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D).move(0.75D, 0.0D, 0.0D);
    protected static final VoxelShape NORTH_OPEN_LEFT_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D).move(-0.75D, 0.0D, 0.0D);
    protected static final VoxelShape WEST_OPEN_LEFT_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D).move(0.0D, 0.0D, 0.75D);
    protected static final VoxelShape EAST_OPEN_LEFT_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D).move(0.0D, 0.0D, -0.75D);

	public TallStableDoorBlock(BlockSetType blockset, Block from) {
		super(blockset, from);
	}

	public TallStableDoorBlock(BlockSetType blockset, Properties properties) {
		super(blockset, properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean flag = !state.getValue(OPEN);
        boolean flag1 = state.getValue(HINGE) == DoorHingeSide.LEFT;
        switch(direction) {
            case EAST:
            default:
                return flag ? EAST_AABB : (flag1 ? EAST_OPEN_LEFT_AABB : EAST_OPEN_AABB);
            case SOUTH:
                return flag ? SOUTH_AABB : (flag1 ? SOUTH_OPEN_LEFT_AABB : SOUTH_OPEN_AABB);
            case WEST:
                return flag ? WEST_AABB : (flag1 ? WEST_OPEN_LEFT_AABB : WEST_OPEN_AABB);
            case NORTH:
                return flag ? NORTH_AABB : (flag1 ? NORTH_OPEN_LEFT_AABB : NORTH_OPEN_AABB);
        }
    }
}
