package dev.aero.shulkertrowel.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In the exact audited QRC 1.9 Fabric binary, returning PASS here leaves the
 * interaction to normal Minecraft processing and bypasses only its shulker feature.
 */
@Pseudo
@Mixin(targets = "com.natamus.quickrightclick_common_fabric.events.QuickEvent", remap = false)
abstract class QuickRightClickShulkerCompatibilityMixin {
    @Inject(method = "onItemClick", at = @At("HEAD"), cancellable = true, require = 1)
    private static void shulkerTrowel$delegateShulkersToNormalInteraction(
            Player player,
            Level level,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
