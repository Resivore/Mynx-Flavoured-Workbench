package dev.resivore.slotreservations.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;

/** Final blit boundary shared by atlas-backed and oversized reservation ghosts. */
public final class GhostItemRenderPipeline {
    public static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(id("pipeline/ghost_item_alpha"))
                    .withFragmentShader(id("core/ghost_item_alpha"))
                    .withColorTargetState(new ColorTargetState(
                            BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA
                    ))
                    .build()
    );

    private GhostItemRenderPipeline() {
    }

    /** Forces registration during client initialization, before shader pipelines are prepared. */
    public static void initialize() {
    }

    public static BlitRenderState blit(
            GpuTextureView texture,
            Matrix3x2fc pose,
            int x0,
            int y0,
            int x1,
            int y1,
            float u0,
            float u1,
            float v0,
            float v1,
            int alpha,
            ScreenRectangle scissorArea
    ) {
        return new BlitRenderState(
                PIPELINE,
                TextureSetup.singleTexture(
                        texture,
                        RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)
                ),
                pose,
                x0,
                y0,
                x1,
                y1,
                u0,
                u1,
                v0,
                v1,
                GhostItemRenderScope.alphaOnlyWhite(alpha),
                scissorArea,
                null
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, path);
    }
}
