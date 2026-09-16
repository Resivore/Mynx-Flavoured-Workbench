package dev.resivore.villagerwork.client;

import dev.resivore.villagerwork.VillagerWorkRoutines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.EntityTypes;

public final class VillagerWorkRoutinesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(VillagerWorkRoutines.FISHING_FLOAT, FishingFloatRenderer::new);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityType != EntityTypes.VILLAGER || !(entityRenderer instanceof VillagerRenderer villagerRenderer)) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    RenderLayerParent<VillagerRenderState, VillagerModel> parent =
                            (RenderLayerParent<VillagerRenderState, VillagerModel>) villagerRenderer;
                    registrationHelper.register(new VwrFishingRodLayer(parent,
                            context.getEntityRenderDispatcher().getItemInHandRenderer()));
                });
    }
}
