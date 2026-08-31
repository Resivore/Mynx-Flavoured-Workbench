package com.starfish_studios.bbb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A retained BBB use interaction driven by the Fabric hammer callback.
 * Keeping the interaction on the block avoids registry-name parsing and makes
 * the curated interaction surface explicit.
 */
public interface HammerableBlock {
    InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit);
}
