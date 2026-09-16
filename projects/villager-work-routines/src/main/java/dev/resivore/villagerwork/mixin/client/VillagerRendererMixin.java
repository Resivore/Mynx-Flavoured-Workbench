package dev.resivore.villagerwork.mixin.client;

import dev.resivore.villagerwork.VillagerWorkRoutines;
import dev.resivore.villagerwork.client.VwrFishingRodLayer;
import dev.resivore.villagerwork.client.VwrFishingRodPresentation;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerRenderer.class)
abstract class VillagerRendererMixin {
    @Shadow protected abstract boolean addLayer(RenderLayer<VillagerRenderState, VillagerModel> layer);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void villagerWork$addFishingRodLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        RenderLayerParent<VillagerRenderState, VillagerModel> parent =
                (RenderLayerParent<VillagerRenderState, VillagerModel>)(Object)this;
        addLayer(new VwrFishingRodLayer(parent,
                context.getEntityRenderDispatcher().getItemInHandRenderer()));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
            at = @At("TAIL"))
    private void villagerWork$extractFishingRodState(Villager villager, VillagerRenderState state,
                                                      float partialTick, CallbackInfo ci) {
        VwrFishingRodPresentation presentation = (VwrFishingRodPresentation)(Object)state;
        presentation.villagerWork$setEntityId(villager.getId());
        presentation.villagerWork$setRenderFishingRod(VillagerWorkRoutines.isFishingRodPresentation(
                villager.getMainHandItem()));
    }
}
