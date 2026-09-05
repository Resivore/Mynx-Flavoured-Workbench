package com.crispytwig.naturalist.lifecycle.mixin;
import com.crispytwig.naturalist.lifecycle.ShapeEqualityFixture;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Reproduces CNM 2.0.7's verified RETURN hook: widen a false comparison to shape-family equality.
@Mixin(ItemStack.class)
public abstract class ShapeEqualityFixtureMixin {
    @Inject(method = "isSameItemSameComponents", at = @At("RETURN"), cancellable = true, require = 1)
    private static void fixture$shapeFamily(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && ShapeEqualityFixture.sameFamily(first.getItem(), second.getItem())) cir.setReturnValue(true);
    }
}
