package dev.resivore.amc.mixin;

import dev.resivore.amc.MossyOakBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.starfish_studios.bbb.BuildingButBetter", remap = false)
abstract class BbbInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 1)
    private void amc$registerMossyOakBlocks(CallbackInfo ci) {
        MossyOakBlocks.afterBbb();
    }
}
