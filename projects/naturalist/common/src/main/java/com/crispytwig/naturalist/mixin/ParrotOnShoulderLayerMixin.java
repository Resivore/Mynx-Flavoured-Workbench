package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.entity.util.ParrotFlight;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParrotOnShoulderLayer.class)
public abstract class ParrotOnShoulderLayerMixin {
    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"))
    private void naturalist$markShoulderFlap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                             int packedLight, AvatarRenderState state, float yRot, float xRot,
                                             CallbackInfo ci) {
        Entity source = NaturalistRenderEntityLookup.source(state);
        ParrotFlight.shoulderShouldFlap = source instanceof Player player
                && ParrotFlight.hasBirdOnHead(player)
                && !player.onGround();
        ParrotFlight.shoulderPartialTick = NaturalistRenderEntityLookup.partialTick(state);
    }

    @ModifyArg(
            method = "submitOnShoulder(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;FFZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            index = 1)
    private Object naturalist$markFlyingShoulderState(Object state) {
        if (ParrotFlight.shoulderShouldFlap && state instanceof ParrotRenderState parrotState) {
            NaturalistParrotRenderStateLookup.markFlyingShoulder(parrotState);
        }
        return state;
    }
}
