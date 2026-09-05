package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.client.NaturalistRenderEntityLookup;
import com.crispytwig.naturalist.server.entity.base.IKMount;
import com.crispytwig.naturalist.server.entity.base.MultipartMob;
import com.crispytwig.naturalist.server.entity.util.MobPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderDispatcher.class, EntityHitboxDebugRenderer.class})
public class EntityRenderDispatcherMixin {
    @Inject(
            method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN"),
            require = 0)
    private void naturalist$rememberSource(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        NaturalistRenderEntityLookup.remember(cir.getReturnValue(), entity, partialTick);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void naturalist$skipBakedRider(EntityRenderState state, CameraRenderState cameraState,
                                           double x, double y, double z, PoseStack poseStack,
                                           SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        Entity entity = NaturalistRenderEntityLookup.source(state);
        if (entity instanceof Player && entity.getVehicle() instanceof IKMount) {
            ci.cancel();
        }
    }

    @Inject(
            method = "showHitboxes(Lnet/minecraft/world/entity/Entity;FZ)V",
            at = @At("TAIL"),
            require = 0)
    private void naturalist$renderMobPartHitboxes(Entity entity, float partialTick, boolean serverEntity, CallbackInfo ci) {
        if (serverEntity) {
            return;
        }
        if (entity instanceof MultipartMob multipart) {
            for (MobPart part : multipart.getParts()) {
                Vec3 offset = part.getPosition(partialTick).subtract(part.position());
                Gizmos.cuboid(part.getBoundingBox().move(offset),
                        GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.25F, 1.0F, 0.0F)));
            }
        }
    }
}
