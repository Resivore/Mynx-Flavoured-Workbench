package com.fizzware.dramaticdoors.blocks;

import com.fizzware.dramaticdoors.blockentities.TallNetheriteDoorBlockEntity;
import com.fizzware.dramaticdoors.state.properties.TripleBlockPart;

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
import net.minecraft.world.phys.BlockHitResult;

public class TallNetheriteDoorBlock extends TallDoorBlock implements EntityBlock
{
	public TallNetheriteDoorBlock(BlockSetType blockset, Block from) {
		super(blockset, from);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (state.getValue(THIRD) == TripleBlockPart.LOWER) {
			return new TallNetheriteDoorBlockEntity(pos, state);
		}
		return null;
	}
	
	@Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos delegatedPos = state.getValue(THIRD) == TripleBlockPart.LOWER ? pos : (state.getValue(THIRD) == TripleBlockPart.UPPER ? pos.below(2) : pos.below(1));
		BlockEntity be = level.getBlockEntity(delegatedPos);
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
	    return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
	}
	
}
