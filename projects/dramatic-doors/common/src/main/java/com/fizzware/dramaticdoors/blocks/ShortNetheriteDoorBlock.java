package com.fizzware.dramaticdoors.blocks;

import com.fizzware.dramaticdoors.blockentities.TallNetheriteDoorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class ShortNetheriteDoorBlock extends ShortDoorBlock implements EntityBlock
{

	public ShortNetheriteDoorBlock(BlockSetType blockset, Block from) {
		super(blockset, from);
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TallNetheriteDoorBlockEntity(pos, state);
	}
	
	@Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockEntity be = level.getBlockEntity(pos);
		//Execute action.
		if (be != null && be instanceof TallNetheriteDoorBlockEntity) {
			TallNetheriteDoorBlockEntity door = (TallNetheriteDoorBlockEntity) be;
			if (door.handleAction(player, player.getUsedItemHand(), "door")) {
				tryOpenDoubleDoor(level, state, pos);
				BlockState newState = state.cycle(OPEN);
				level.setBlock(pos, newState, 10);
	            this.playSound(player, level, pos, state.getValue(OPEN));
	            level.gameEvent(player, state.getValue(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
			}
		}
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
	    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}
}
