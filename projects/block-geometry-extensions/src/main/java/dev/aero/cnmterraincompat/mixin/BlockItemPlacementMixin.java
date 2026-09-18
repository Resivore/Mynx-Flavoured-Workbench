package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.FullOccupancyNormalizer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves BlockItem's legality/economy transaction while changing only a completed slab result. */
@Mixin(BlockItem.class)
abstract class BlockItemPlacementMixin {
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true, require = 1)
    private void bge$normalizeCompletedSlab(BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir) {
        BlockState placed = cir.getReturnValue();
        if (placed != null) cir.setReturnValue(FullOccupancyNormalizer.normalize(placed));
    }
}
