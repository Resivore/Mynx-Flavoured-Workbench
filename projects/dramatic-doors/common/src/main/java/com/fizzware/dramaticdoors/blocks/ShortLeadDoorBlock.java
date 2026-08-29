package com.fizzware.dramaticdoors.blocks;

import com.fizzware.dramaticdoors.state.properties.DDBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class ShortLeadDoorBlock extends ShortDoorBlock
{
	public static final IntegerProperty OPENING_PROGRESS = DDBlockStateProperties.OPENING_PROGRESS;

	public ShortLeadDoorBlock(BlockSetType blockset, Block from) {
		super(blockset, from);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.FALSE).setValue(HINGE, DoorHingeSide.LEFT).setValue(POWERED, Boolean.FALSE).setValue(WATERLOGGED, false).setValue(OPENING_PROGRESS, 0));
	}
	
    public boolean canBeOpened(BlockState state) {
        return state.getValue(OPENING_PROGRESS) == 2;
    }
	
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HINGE, POWERED, WATERLOGGED, OPENING_PROGRESS);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this.canBeOpened(state)) {
            ShortDoorBlock.tryOpenDoubleDoor(level, state, pos);

            state = state.cycle(OPEN).setValue(OPENING_PROGRESS, 0);
            level.setBlock(pos, state, 10);
            level.levelEvent(player, state.getValue(OPEN) ? 1005 : 1011, pos, 0);
        } else {
            int p = state.getValue(OPENING_PROGRESS) + 1;
            level.setBlock(pos, state.setValue(OPENING_PROGRESS, p), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);

            level.playSound(player, pos, SoundEvents.NETHERITE_BLOCK_STEP, SoundSource.BLOCKS, 1, 1);
            level.scheduleTick(pos, this, 20);
        }
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource pRandom) {
        level.setBlock(pos, state.setValue(OPENING_PROGRESS, 0), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
    }
}
