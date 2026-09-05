package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.item.NaturalistCreativeIdentity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shape-family inventory equality must not merge distinct creative entries. */
@Mixin(targets = "net.minecraft.world.item.ItemStackLinkedSet$1")
public abstract class CreativeItemStackIdentityMixin {
    @Inject(method = "equals(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void naturalist$exactCreativeIdentity(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (NaturalistCreativeIdentity.isActive()) {
            cir.setReturnValue(NaturalistCreativeIdentity.sameStack(first, second));
        }
    }
}
