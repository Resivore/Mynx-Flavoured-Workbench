package dev.resivore.amc.mixin;

import dev.resivore.amc.TrapdoorIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.kikoz.mcwtrpdoors.MacawsTrapdoors", remap = false)
abstract class MacawTrapdoorsInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 1)
    private void amc$trapdoorProviderReady(CallbackInfo ci) {
        TrapdoorIntegration.trapdoorsReady();
    }
}
