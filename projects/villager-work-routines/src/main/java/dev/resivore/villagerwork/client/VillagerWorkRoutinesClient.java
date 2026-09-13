package dev.resivore.villagerwork.client;

import dev.resivore.villagerwork.VillagerWorkRoutines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class VillagerWorkRoutinesClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(VillagerWorkRoutines.FISHING_FLOAT, FishingFloatRenderer::new);
    }
}
