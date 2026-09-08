package dev.resivore.polytoneleadrenderingfix;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.resivore.polytoneleadrenderingfix.mixin.RenderTypeAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** C4's leash-only entity-format pipeline, deliberately retaining vanilla triangle-strip topology. */
public final class LeashRenderTypes {
    // Minecraft 26.2 has no entity lead texture. Polytone 26.2-6.3.1 supplies this required
    // minecraft-namespace material; C4 deliberately keeps that established textured route.
    private static final Identifier POLYTONE_LEAD_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/lead.png");
    private static final RenderPipeline ENTITY_COMPATIBLE_LEASH_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("polytone_lead_rendering_fix", "leash_entity_triangle_strip"))
        .withVertexShader("core/entity")
        .withFragmentShader("core/entity")
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withBindGroupLayout(BindGroupLayouts.LIGHTING)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
        .withDepthStencilState(DepthStencilState.DEFAULT)
        .withCull(false)
        .build();
    private static final RenderType ENTITY_COMPATIBLE_LEASH = RenderTypeAccessor.polytoneLeadRenderingFix$create(
        "polytone_lead_entity_triangle_strip",
        RenderSetup.builder(ENTITY_COMPATIBLE_LEASH_PIPELINE)
            .withTexture("Sampler0", POLYTONE_LEAD_TEXTURE)
            .useLightmap()
            .useOverlay()
            .createRenderSetup()
    );

    static {
        // Iris maps core pipelines by object identity. This is optional: Iris classes are
        // initialized only when the loader reports Iris, while vanilla still uses core/entity.
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            IrisPipelines.assignPipeline(ENTITY_COMPATIBLE_LEASH_PIPELINE, ShaderKey.ENTITIES_SOLID);
        }
    }

    private LeashRenderTypes() {
    }

    public static RenderType entityCompatibleTriangleStrip() {
        return ENTITY_COMPATIBLE_LEASH;
    }
}
