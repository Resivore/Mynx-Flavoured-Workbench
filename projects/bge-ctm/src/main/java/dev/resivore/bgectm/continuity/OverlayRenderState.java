package dev.resivore.bgectm.continuity;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Objects;

/** The complete non-geometric state set by Continuity 3.0.1's exact overlay helper. */
record OverlayRenderState(int tint, QuadAtlas atlas, boolean animated,
        ChunkSectionLayer chunkLayer, RenderType itemRenderType, TriState ambientOcclusion) {
    OverlayRenderState {
        Objects.requireNonNull(atlas, "atlas");
        Objects.requireNonNull(chunkLayer, "chunkLayer");
        Objects.requireNonNull(itemRenderType, "itemRenderType");
        Objects.requireNonNull(ambientOcclusion, "ambientOcclusion");
    }

    static OverlayRenderState continuity(TextureAtlasSprite sprite, int tint,
            ChunkSectionLayer layer, TriState ao) {
        return new OverlayRenderState(tint, QuadAtlas.BLOCK, sprite.contents().isAnimated(), layer,
                itemRenderType(layer), ao);
    }

    static RenderType itemRenderType(ChunkSectionLayer layer) {
        return layer == ChunkSectionLayer.TRANSLUCENT
                ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
    }

    void apply(QuadEmitter emitter) {
        emitter.color(tint, tint, tint, tint);
        emitter.atlas(atlas);
        emitter.animated(animated);
        emitter.chunkLayer(chunkLayer);
        emitter.itemRenderType(itemRenderType);
        emitter.ambientOcclusion(ambientOcclusion);
    }
}
