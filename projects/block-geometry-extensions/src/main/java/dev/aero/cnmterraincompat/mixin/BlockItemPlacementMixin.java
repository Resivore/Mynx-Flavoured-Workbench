package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.FullOccupancyNormalizer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves BlockItem's legality transaction while canonicalizing supported full occupancy. */
@Mixin(BlockItem.class)
abstract class BlockItemPlacementMixin {
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true, require = 1)
    private void bge$normalizeCompletedGeometry(BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir) {
        BlockState placed = cir.getReturnValue();
        if (placed != null) cir.setReturnValue(FullOccupancyNormalizer.normalize(placed));
    }
}
