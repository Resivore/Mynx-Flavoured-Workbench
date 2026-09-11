package dev.resivore.pickupaudiodiag.mixin;

import dev.resivore.pickupaudiodiag.PickupAudioDiag;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the concrete vanilla local-sound API rather than competing at CCAR's argument-modifying call site. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    private static final String PLAY_LOCAL_SOUND =
            "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V";

    @Inject(method = PLAY_LOCAL_SOUND, at = @At("HEAD"))
    private void pickupAudioDiag$localSound(
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            boolean distanceDelay,
            CallbackInfo ci) {
        PickupAudioDiag.localSound(x, y, z, sound, source, volume, pitch, distanceDelay);
    }
}
