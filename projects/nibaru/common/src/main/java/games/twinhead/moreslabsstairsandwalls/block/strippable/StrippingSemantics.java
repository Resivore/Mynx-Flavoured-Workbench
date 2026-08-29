package games.twinhead.moreslabsstairsandwalls.block.strippable;

import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Geometry-neutral axe stripping with exact target identity supplied by the material profile. */
public final class StrippingSemantics {
    private StrippingSemantics() {}

    public static InteractionResult strip(ItemStack stack, BlockState source, BlockState strippedTarget,
            Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof AxeItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, PathSemantics.copySharedProperties(source, strippedTarget));
            stack.hurtAndBreak(1, player,
                    hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        } else {
            level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}
