package dev.resivore.inventorycrafting.mixin;

import net.minecraft.world.inventory.SlotRanges;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SlotRanges.class)
abstract class SlotRangesMixin {
    @Dynamic("Targets javac's lambda body for SlotRanges' static range registration")
    @ModifyArg(
            method = "lambda$static$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/SlotRanges;addSlotRange(Ljava/util/List;Ljava/lang/String;II)V",
                    ordinal = 6
            ),
            index = 3,
            require = 1
    )
    private static int inventory3x3$registerNinePlayerCraftingRanges(int vanillaCount) {
        return 9;
    }
}
