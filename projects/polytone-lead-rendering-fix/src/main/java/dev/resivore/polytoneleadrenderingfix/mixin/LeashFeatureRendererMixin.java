package dev.resivore.polytoneleadrenderingfix.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapts only Minecraft 26.2's leash feature to the entity-format route Iris maps to its
 * entity program. Iris maps the native POSITION_COLOR_LIGHTMAP leash pipeline to its Basic
 * program; Complementary's Basic program consumes a normal that the native leash format
 * cannot carry. The entity route carries the complete attribute contract instead.
 *
 * <p>The geometry, alternating vanilla brown RGB factors, slack curve, and packed-light
 * interpolation below are transcribed from Minecraft 26.2's LeashFeatureRenderer. The
 * additional UV/overlay/normal attributes exist solely because the selected vanilla entity
 * RenderType requires them; this class does not alter leash state or gameplay.</p>
 */
@Mixin(LeashFeatureRenderer.class)
abstract class LeashFeatureRendererMixin {
    private static final Identifier VANILLA_LEAD_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/lead.png");
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.05F;
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
    private RenderType polytoneLeadRenderingFix$useEntityCompatibleLeashRoute(RenderType vanillaLeashType) {
        // This is Minecraft's own entity RenderType and its own lead texture, not Polytone's path.
        return RenderTypes.entitySolid(VANILLA_LEAD_TEXTURE);
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

        // Exact Minecraft 26.2 rope tint and alternating directional-shading factor.
        float shade = segment % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
        float red = 0.5F * shade;
        float green = 0.4F * shade;
        float blue = 0.3F * shade;
        float xProgress = x * progress;
        float yProgress = leashY(y, progress, leashState.slack);
        float zProgress = z * progress;

        writeVertex(
            vertexConsumer, matrix, xProgress - xOffset, yProgress + yOffset, zProgress + zOffset,
            0.0F, progress, red, green, blue, packedLight
        );
        writeVertex(
            vertexConsumer, matrix, xProgress + xOffset, yProgress + LEASH_WIDTH - yOffset, zProgress - zOffset,
            1.0F, progress, red, green, blue, packedLight
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

    private static void writeVertex(
        VertexConsumer vertexConsumer,
        Matrix4fc matrix,
        float x,
        float y,
        float z,
        float u,
        float v,
        float red,
        float green,
        float blue,
        int packedLight
    ) {
        vertexConsumer.addVertex(matrix, x, y, z)
            .setColor(red, green, blue, 1.0F)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            // Entity-format shaders require a defined normal; this preserves the lightmap as
            // the leash's primary lighting input while avoiding an invented per-face pattern.
            .setNormal(0.0F, 1.0F, 0.0F);
    }
}
