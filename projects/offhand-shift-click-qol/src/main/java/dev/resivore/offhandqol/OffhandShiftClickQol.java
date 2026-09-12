package dev.resivore.offhandqol;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class OffhandShiftClickQol implements ModInitializer {
    public static final String MOD_ID = "offhand_shift_click_qol";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                OffhandPickupSoundFallbackPayload.TYPE,
                OffhandPickupSoundFallbackPayload.CODEC
        );
    }
}
