package dev.resivore.pickupaudiodiag.mixin;

import dev.resivore.pickupaudiodiag.PickupAudioDiag;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distinguishes reaching SoundEngine.play from reaching its first actual vanilla instruction.
 * None of these observers is cancellable or changes a value.
 */
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    private static final String PLAY =
            "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;";
    private static final String RESOLVE =
            "Lnet/minecraft/client/resources/sounds/SoundInstance;resolve(Lnet/minecraft/client/sounds/SoundManager;)Lnet/minecraft/client/sounds/WeighedSoundEvents;";
    private static final String LOADED_FIELD = "Lnet/minecraft/client/sounds/SoundEngine;loaded:Z";

    @Inject(method = PLAY, at = @At("HEAD"))
    private void pickupAudioDiag$engineEnter(
            SoundInstance soundInstance,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PickupAudioDiag.engineEnter(soundInstance);
    }

    @Inject(method = PLAY, at = @At(value = "FIELD", target = LOADED_FIELD, opcode = Opcodes.GETFIELD, ordinal = 0))
    private void pickupAudioDiag$engineSurvivedHead(
            SoundInstance soundInstance,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PickupAudioDiag.engineSurvivedHead(soundInstance);
    }

    @Inject(method = PLAY, at = @At(value = "INVOKE", target = RESOLVE, shift = At.Shift.AFTER))
    private void pickupAudioDiag$resolvedSound(
            SoundInstance soundInstance,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PickupAudioDiag.resolvedSound(soundInstance);
    }

    @Inject(method = PLAY, at = @At("RETURN"))
    private void pickupAudioDiag$engineReturned(
            SoundInstance soundInstance,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        PickupAudioDiag.engineReturned(soundInstance, cir.getReturnValue());
    }
}
