package dev.resivore.carryonpatch.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.carryonpatch.RibbitCarryPlacement;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import dev.resivore.carryonpatch.RenderIdAssigningEntityCache;
import java.util.HashMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Installs the assigning map at GrabAndGo's one shared synthetic-entity cache boundary. */
@Mixin(
        targets = "org.chermew.grabandgo.client.render.CarriedObjectFeatureRenderer",
        remap = false
)
abstract class CarriedObjectFeatureRendererMixin {
    @Redirect(
            method = "<clinit>()V",
            at = @At(
                    value = "NEW",
                    target = "Ljava/util/HashMap;",
                    ordinal = 0,
                    remap = false
            ),
            require = 1,
            remap = false
    )
    private static HashMap<String, Entity> carryOnPatch$installAssigningCache() {
        return new RenderIdAssigningEntityCache();
    }

    @Redirect(method = "renderEntity(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", remap = false), require = 1, remap = false)
    private static void carryOnPatch$placeThirdPerson(EntityRenderer<?, ?> renderer,
            EntityRenderState state, PoseStack poses, SubmitNodeCollector collector,
            CameraRenderState camera) {
        RibbitCarryPlacement.submit(renderer, state, poses, collector, camera, false);
    }
}
