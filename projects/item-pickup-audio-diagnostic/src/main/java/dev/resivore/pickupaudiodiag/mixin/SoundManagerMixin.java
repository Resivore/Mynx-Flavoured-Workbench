package dev.resivore.pickupaudiodiag.mixin;

import dev.resivore.pickupaudiodiag.PickupAudioDiag;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports only pickup SoundInstances arriving at the 26.2 manager facade. */
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
    private static final String PLAY =
            "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;";

    @Inject(method = PLAY, at = @At("HEAD"))
    private void pickupAudioDiag$soundManager(
            SoundInstance soundInstance,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PickupAudioDiag.soundManager(soundInstance);
    }
}
