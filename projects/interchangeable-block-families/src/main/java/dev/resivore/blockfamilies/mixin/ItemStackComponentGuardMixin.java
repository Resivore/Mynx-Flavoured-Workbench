package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops CNM's later RETURN injection from widening unequal component patches for
 * IBF-owned families. The lower mixin priority is intentional: this HEAD guard
 * is applied after CNM's default-priority RETURN hook, so its cancelled return
 * cannot subsequently be widened by CNM.
 */
@Mixin(value = ItemStack.class, priority = 900)
abstract class ItemStackComponentGuardMixin {
    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true, require = 1)
    private static void interchangeableBlockFamilies$requireEqualPatches(
            ItemStack first,
            ItemStack second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (AuditedShapeRuntime.inSameFamily(first.getItem(), second.getItem())
                && !first.getComponentsPatch().equals(second.getComponentsPatch())) {
            cir.setReturnValue(false);
        }
    }
}
