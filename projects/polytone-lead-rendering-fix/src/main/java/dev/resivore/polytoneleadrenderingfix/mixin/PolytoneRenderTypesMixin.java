package dev.resivore.polytoneleadrenderingfix.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.polytone.PolytoneRenderTypes;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces Polytone's own vanilla leash fallback without changing its LeashMixin or any other
 * Polytone render feature. These descriptors are audited against Polytone 26.2-6.3.1-fabric.
 */
@Mixin(value = PolytoneRenderTypes.class, remap = false)
abstract class PolytoneRenderTypesMixin {
    private static final String GET_LEASH_RENDER_TYPE =
        "getLeashRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;";
    private static final String ADD_LEASH_VERTEX_PAIR =
        "addLeashVertexPair(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4fc;FFFFFFIZLnet/minecraft/client/renderer/entity/state/EntityRenderState$LeashState;)Z";

    @Inject(method = GET_LEASH_RENDER_TYPE, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void polytoneLeadRenderingFix$useVanillaLeashRenderType(
        CallbackInfoReturnable<RenderType> callbackInfo
    ) {
        callbackInfo.setReturnValue(null);
    }

    @Inject(method = ADD_LEASH_VERTEX_PAIR, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void polytoneLeadRenderingFix$useVanillaLeashVertices(
        VertexConsumer vertexConsumer,
        Matrix4fc matrix,
        float x,
        float y,
        float z,
        float xOffset,
        float yOffset,
        float zOffset,
        int segment,
        boolean reverse,
        EntityRenderState.LeashState leashState,
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        callbackInfo.setReturnValue(false);
    }
}
