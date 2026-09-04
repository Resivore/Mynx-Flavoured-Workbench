package dev.resivore.carryonpatch.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.carryonpatch.RibbitCarryPlacement;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Runs after GrabAndGo's priority-1000 mixin merges its private carried-entity method. */
@Mixin(value = ItemInHandRenderer.class, priority = 1100, remap = false)
abstract class FirstPersonCarryPlacementMixin {

    @Redirect(method = "renderEntityInFirstPerson(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/nbt/CompoundTag;Lnet/minecraft/client/player/LocalPlayer;F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", remap = false), require = 1, remap = false)
    private void carryOnPatch$placeFirstPerson(EntityRenderer<?, ?> renderer,
            EntityRenderState state, PoseStack poses, SubmitNodeCollector collector,
            CameraRenderState camera) {
        RibbitCarryPlacement.submit(renderer, state, poses, collector, camera, true);
    }
}
