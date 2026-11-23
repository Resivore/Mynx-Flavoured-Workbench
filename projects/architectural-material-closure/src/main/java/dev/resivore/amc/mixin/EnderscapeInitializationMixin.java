package dev.resivore.amc.mixin;

import dev.resivore.amc.DoorIntegration;
import dev.resivore.amc.TrapdoorIntegration;
import dev.resivore.amc.EnderscapeFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.penumbra.enderscape.Enderscape", remap = false)
abstract class EnderscapeInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 1)
    private void amc$registerEnderscapeFamilies(CallbackInfo ci) {
        EnderscapeFamilies.registerAfterProvider();
        DoorIntegration.woodReady("enderscape");
        TrapdoorIntegration.woodReady("enderscape");
    }
}
