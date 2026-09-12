package dev.resivore.offhandqol.client;

import dev.resivore.offhandqol.OffhandPickupSoundFallbackPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class OffhandShiftClickQolClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OffhandPickupSoundFallbackPayload.TYPE, (payload, context) ->
                context.client().execute(() -> playFallbackPickupSound(context.client(), payload))
        );
    }

    private static void playFallbackPickupSound(Minecraft client, OffhandPickupSoundFallbackPayload payload) {
        if (client.level == null) return;
        float pitch = (client.level.getRandom().nextFloat() - client.level.getRandom().nextFloat()) * 1.4F + 2.0F;
        client.level.playLocalSound(
                payload.x(), payload.y(), payload.z(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, pitch, false
        );
    }
}
