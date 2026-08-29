package dev.resivore.matchajei;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import dev.resivore.matchajei.server.MatchaDataScanner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class MatchaJeiIntegration implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(
                MatchaJeiDataPayload.TYPE,
                MatchaJeiDataPayload.STREAM_CODEC,
                MatchaJeiDataPayload.MAX_PAYLOAD_BYTES
        );
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            List<RecipeHolder<?>> recipes = server.getRecipeManager().getRecipes().stream()
                    .filter(holder -> MatchaNamespaces.contains(holder.id().identifier().getNamespace()))
                    .toList();
            handler.getPlayer().awardRecipes(recipes);
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (ServerPlayNetworking.canSend(player, MatchaJeiDataPayload.TYPE)) {
                ServerPlayNetworking.send(player, MatchaDataScanner.payloadFor(player.level().getServer()));
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(MatchaDataScanner::initialize);
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resourceManager) ->
                MatchaDataScanner.invalidateReloadable(server)
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(MatchaDataScanner::forget);
    }
}
