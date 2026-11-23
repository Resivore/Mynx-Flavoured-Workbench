package dev.resivore.amc.mixin;

import dev.resivore.amc.DoorIntegration;
import dev.resivore.amc.TrapdoorIntegration;
import dev.resivore.amc.MossyOakBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.resivore.mynxtrees.MynxTrees", remap = false)
abstract class MynxTreesInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 1)
    private void amc$registerMossyOakBlocks(CallbackInfo ci) {
        MossyOakBlocks.afterTrees();
    }
}
