package dev.resivore.polytoneleadrenderingfix.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.polytoneleadrenderingfix.LeashRenderTypes;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * C4 keeps Minecraft 26.2's native leash stream: 25 forward and 25 reverse vertex pairs
 * in one triangle strip. Only the format is expanded to the entity contract Iris exposes
 * to Complementary. C2 used entitySolid (QUADS) for that same strip and was topologically
 * invalid.
 */
@Mixin(LeashFeatureRenderer.class)
abstract class LeashFeatureRendererMixin {
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.05F;
    // Polytone's bundled minecraft:textures/entity/lead.png carries the rope's brown material.
    // Keep the entity route neutral so core/entity does not multiply that material by the old
    // untextured vanilla leash tint a second time.
    private static final float TEXTURE_COLOR = 1.0F;
    private static final String PREPARE =
        "prepare(Lnet/minecraft/client/renderer/feature/LeashFeatureRenderer$Submit;)V";
    private static final String ADD_LEASH_VERTEX_PAIR =
        "addVertexPair(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4fc;FFFFFFIZLnet/minecraft/client/renderer/entity/state/EntityRenderState$LeashState;)V";

    @ModifyArg(
        method = PREPARE,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/LeashFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        ),
        index = 0,
        require = 1
    )
    private RenderType polytoneLeadRenderingFix$useEntityCompatibleTriangleStrip(RenderType vanillaLeashType) {
        return LeashRenderTypes.entityCompatibleTriangleStrip();
    }

    @Inject(method = ADD_LEASH_VERTEX_PAIR, at = @At("HEAD"), cancellable = true, require = 1)
    private static void polytoneLeadRenderingFix$writeEntityFormatLeashVertices(
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
        CallbackInfo callbackInfo
    ) {
        float progress = (float) segment / LEASH_RENDER_STEPS;
        int blockLight = (int) Mth.lerp(progress, leashState.startBlockLight, leashState.endBlockLight);
        int skyLight = (int) Mth.lerp(progress, leashState.startSkyLight, leashState.endSkyLight);
        int packedLight = LightCoordsUtil.pack(blockLight, skyLight);

        // C4: texture owns the brown base color; vertex color is an intentional neutral multiplier.
        // The old vanilla 0.5/0.4/0.3 tint and alternating 0.7 shade applied to an untextured
        // POSITION_COLOR_LIGHTMAP leash, but duplicate Polytone's textured material here.
        float xProgress = x * progress;
        float yProgress = leashY(y, progress, leashState.slack);
        float zProgress = z * progress;
        float[] normal = segmentNormal(x, y, z, xOffset, yOffset, zOffset, progress, leashState.slack);

        writeVertex(
            vertexConsumer, matrix, xProgress - xOffset, yProgress + yOffset, zProgress + zOffset,
            0.0F, progress, packedLight, normal
        );
        writeVertex(
            vertexConsumer, matrix, xProgress + xOffset, yProgress + LEASH_WIDTH - yOffset, zProgress - zOffset,
            1.0F, progress, packedLight, normal
        );
        callbackInfo.cancel();
    }

    private static float leashY(float y, float progress, boolean slack) {
        if (!slack) {
            return y * progress;
        }
        return y > 0.0F
            ? y * progress * progress
            : y - y * (1.0F - progress) * (1.0F - progress);
    }

    /**
     * Derives a surface normal from the local sag tangent and exact pair cross section,
     * rather than passing a universal world-up placeholder to the entity shader contract.
     */
    private static float[] segmentNormal(
        float x, float y, float z, float xOffset, float yOffset, float zOffset,
        float progress, boolean slack
    ) {
        float tangentY;
        if (!slack) {
            tangentY = y;
        } else if (y > 0.0F) {
            tangentY = 2.0F * y * progress;
        } else {
            tangentY = 2.0F * y * (1.0F - progress);
        }

        float acrossX = 2.0F * xOffset;
        float acrossY = LEASH_WIDTH - 2.0F * yOffset;
        float acrossZ = -2.0F * zOffset;
        float normalX = tangentY * acrossZ - z * acrossY;
        float normalY = z * acrossX - x * acrossZ;
        float normalZ = x * acrossY - tangentY * acrossX;
        float lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;
        if (lengthSquared < 1.0E-8F) {
            // This only handles a zero-area segment, which has no visible rope surface.
            return new float[] {0.0F, 0.0F, 1.0F};
        }
        float inverseLength = Mth.invSqrt(lengthSquared);
        return new float[] {normalX * inverseLength, normalY * inverseLength, normalZ * inverseLength};
    }

    private static void writeVertex(
        VertexConsumer vertexConsumer,
        Matrix4fc matrix,
        float x,
        float y,
        float z,
        float u,
        float v,
        int packedLight,
        float[] normal
    ) {
        vertexConsumer.addVertex(matrix, x, y, z)
            .setColor(TEXTURE_COLOR, TEXTURE_COLOR, TEXTURE_COLOR, 1.0F)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(normal[0], normal[1], normal[2]);
    }
}
