package dev.resivore.pickupaudiodiag.mixin;

import dev.resivore.pickupaudiodiag.PickupAudioDiag;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the exact 26.2 take-item handler without changing its packet or local-sound arguments. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    private static final String TAKE_ITEM_HANDLER =
            "handleTakeItemEntity(Lnet/minecraft/network/protocol/game/ClientboundTakeItemEntityPacket;)V";
    private static final String LOCAL_SOUND =
            "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V";

    @Inject(method = TAKE_ITEM_HANDLER, at = @At("HEAD"))
    private void pickupAudioDiag$packetReceived(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        PickupAudioDiag.packetReceived(packet);
    }

    @Inject(method = TAKE_ITEM_HANDLER, at = @At(value = "INVOKE", target = LOCAL_SOUND))
    private void pickupAudioDiag$beginVanillaLocalSound(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        PickupAudioDiag.beginVanillaLocalSound(packet);
    }

    @Inject(method = TAKE_ITEM_HANDLER, at = @At(value = "INVOKE", target = LOCAL_SOUND, shift = At.Shift.AFTER))
    private void pickupAudioDiag$finishVanillaLocalSound(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        PickupAudioDiag.finishVanillaLocalSound();
    }

    @Inject(method = TAKE_ITEM_HANDLER, at = @At("RETURN"))
    private void pickupAudioDiag$packetCompleted(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        PickupAudioDiag.packetCompleted(packet);
    }
}
