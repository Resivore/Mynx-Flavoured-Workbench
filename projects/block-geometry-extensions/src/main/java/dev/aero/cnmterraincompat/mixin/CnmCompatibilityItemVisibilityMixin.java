package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.RetainedCompatibilityAliases;
import dev.tazer.clutternomore.common.CHooks;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends CNM's shared creative/search presentation predicate without changing ShapeMap. */
@Mixin(value = CHooks.class, remap = false)
abstract class CnmCompatibilityItemVisibilityMixin {
    @Inject(method = "denyItem", at = @At("RETURN"), cancellable = true, require = 1)
    private static void cnmTerrainCompat$hideRetainedCompatibilityAliases(Item item,
            CallbackInfoReturnable<Boolean> cir) {
        if (RetainedCompatibilityAliases.isRetainedButHiddenCompatibilityAlias(item)) {
            cir.setReturnValue(true);
        }
    }
}
