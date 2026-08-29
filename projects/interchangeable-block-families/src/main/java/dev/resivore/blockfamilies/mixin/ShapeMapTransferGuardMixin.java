package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShapeMap.class, remap = false)
abstract class ShapeMapTransferGuardMixin {
    @Inject(method = "transferStack", at = @At("RETURN"), cancellable = true, require = 1)
    private static void interchangeableBlockFamilies$validateTransfer(
            ItemStack source,
            int index,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!AuditedShapeRuntime.isAudited(source.getItem())) {
            return;
        }

        ItemStack result = cir.getReturnValue();
        if (result == null
                || result.getCount() != source.getCount()
                || !result.getComponentsPatch().equals(source.getComponentsPatch())
                || !AuditedShapeRuntime.inSameFamily(source.getItem(), result.getItem())) {
            cir.setReturnValue(source.copy());
        }
    }
}
