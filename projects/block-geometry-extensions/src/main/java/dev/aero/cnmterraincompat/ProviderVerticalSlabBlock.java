package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** CNM geometry whose material interactions are supplied by its provider runtime binding. */
class ProviderVerticalSlabBlock extends VerticalSlabBlock {
    ProviderVerticalSlabBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return NibaruProviderAdapter.useComposedCapabilities(this, stack, state, level, pos, player, hand)
                .orElseGet(() -> super.useItemOn(stack, state, level, pos, player, hand, hit));
    }
}
