package dev.resivore.carriedrouting.mixin.client;

import dev.resivore.carriedrouting.client.RoutedPickupSoundState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @ModifyArgs(
            method = "handleTakeItemEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
            )
    )
    private void carriedRouting$lowerRoutedPickupPitch(Args args, ClientboundTakeItemEntityPacket packet) {
        if (args.get(3) == SoundEvents.ITEM_PICKUP
                && RoutedPickupSoundState.consume(packet.getItemId())) {
            args.set(6, (float) args.get(6) * RoutedPickupSoundState.PITCH_MULTIPLIER);
        }
    }
}
