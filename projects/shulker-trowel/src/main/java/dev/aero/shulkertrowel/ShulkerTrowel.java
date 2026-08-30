package dev.aero.shulkertrowel;

import dev.aero.shulkertrowel.item.ModItems;
import dev.aero.shulkertrowel.network.ChangeTrowelGeometryPayload;
import dev.aero.shulkertrowel.network.TrowelGeometryAuthority;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShulkerTrowel implements ModInitializer {
    public static final String MOD_ID = "shulker_trowel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        PayloadTypeRegistry.serverboundPlay().register(
                ChangeTrowelGeometryPayload.TYPE,
                ChangeTrowelGeometryPayload.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(ChangeTrowelGeometryPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        TrowelGeometryAuthority.apply(context.player(), payload.geometryId())));
        LOGGER.info("Shulker Trowel Canary 5 initialized");
    }
}
