package dev.resivore.villagerwork.mixin.client;

import dev.resivore.villagerwork.client.VwrFishingRodPresentation;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerRenderer.class)
abstract class VillagerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
            at = @At("TAIL"))
    private void villagerWork$extractFishingRodState(Villager villager, VillagerRenderState state,
                                                      float partialTick, CallbackInfo ci) {
        VwrFishingRodPresentation presentation = (VwrFishingRodPresentation)(Object)state;
        presentation.villagerWork$setEntityId(villager.getId());
        presentation.villagerWork$setPartialTick(partialTick);
    }
}
