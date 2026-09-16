package dev.aero.shulkertrowel.mixin;

import dev.aero.shulkertrowel.compat.OffhandShulkerPlacementPolicy;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops the canonical BlockItem placement path before it can mutate the world or stack. */
@Mixin(BlockItem.class)
abstract class OffhandShulkerPlacementMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, require = 1)
    private void shulkerTrowel$reserveOffhandShulker(
            BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (OffhandShulkerPlacementPolicy.blocks((BlockItem) (Object) this, context.getHand())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
