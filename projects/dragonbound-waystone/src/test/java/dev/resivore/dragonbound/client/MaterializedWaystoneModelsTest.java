package dev.resivore.dragonbound.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaterializedWaystoneModelsTest {
    private static final RenderType BLOCK_ITEM_RENDER_TYPE = Sheets.cutoutBlockItemSheet();

    @BeforeAll
    static void installRendererForSpriteBake() {
        try {
            Renderer.get();
        } catch (UnsupportedOperationException ignored) {
            Renderer.register(IndigoRenderer.INSTANCE);
        }
    }

    @Test
    void retargetingPreservesWaystoneTopologyPositionsAndDirectionsWhileConfiningUvsToOneDonorSprite() {
        try (TextureAtlasSprite source = sprite("waystone_source", 16, 16);
             TextureAtlasSprite donor = sprite("warped_hyphae", 176, 96)) {
            MaterializedWaystoneModels.DirectionalMaterial material = material(donor);
            List<BakedQuad> sourceQuads = new ArrayList<>();
            List<BakedQuad> retargeted = new ArrayList<>();

            for (Direction direction : Direction.values()) {
                BakedQuad sourceQuad = quad(direction, source);
                sourceQuads.add(sourceQuad);
                retargeted.add(MaterializedWaystoneModels.retarget(sourceQuad, material));
            }

            assertEquals(sourceQuads.size(), retargeted.size());
            for (int index = 0; index < sourceQuads.size(); index++) {
                BakedQuad sourceQuad = sourceQuads.get(index);
                BakedQuad result = retargeted.get(index);
                assertEquals(sourceQuad.position0(), result.position0());
                assertEquals(sourceQuad.position1(), result.position1());
                assertEquals(sourceQuad.position2(), result.position2());
                assertEquals(sourceQuad.position3(), result.position3());
                assertEquals(sourceQuad.direction(), result.direction());
                assertSame(donor, result.materialInfo().sprite());
                assertTrue(SpriteLocalUvs.isInside(result, donor));
            }
        }
    }

    private static MaterializedWaystoneModels.DirectionalMaterial material(TextureAtlasSprite donor) {
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(
                donor,
                ChunkSectionLayer.SOLID,
                BLOCK_ITEM_RENDER_TYPE,
                -1,
                true,
                0);
        EnumMap<Direction, BakedQuad.MaterialInfo> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, info);
        }
        return new MaterializedWaystoneModels.DirectionalMaterial(
                Identifier.withDefaultNamespace("warped_hyphae"), faces);
    }

    private static BakedQuad quad(Direction direction, TextureAtlasSprite sprite) {
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(
                sprite,
                ChunkSectionLayer.SOLID,
                BLOCK_ITEM_RENDER_TYPE,
                -1,
                true,
                0);
        Vector3f[] positions = positions(direction);
        return new BakedQuad(
                positions[0],
                positions[1],
                positions[2],
                positions[3],
                UVPair.pack(sprite.getU(0.0F), sprite.getV(0.0F)),
                UVPair.pack(sprite.getU(0.0F), sprite.getV(1.0F)),
                UVPair.pack(sprite.getU(1.0F), sprite.getV(1.0F)),
                UVPair.pack(sprite.getU(1.0F), sprite.getV(0.0F)),
                direction,
                info);
    }

    private static Vector3f[] positions(Direction direction) {
        return switch (direction) {
            case DOWN -> new Vector3f[]{
                    new Vector3f(0.0F, 0.25F, 1.0F), new Vector3f(0.0F, 0.25F, 0.0F),
                    new Vector3f(1.0F, 0.25F, 0.0F), new Vector3f(1.0F, 0.25F, 1.0F)};
            case UP -> new Vector3f[]{
                    new Vector3f(0.0F, 0.75F, 0.0F), new Vector3f(0.0F, 0.75F, 1.0F),
                    new Vector3f(1.0F, 0.75F, 1.0F), new Vector3f(1.0F, 0.75F, 0.0F)};
            case NORTH -> new Vector3f[]{
                    new Vector3f(0.0F, 0.25F, 0.0F), new Vector3f(0.0F, 0.75F, 0.0F),
                    new Vector3f(1.0F, 0.75F, 0.0F), new Vector3f(1.0F, 0.25F, 0.0F)};
            case SOUTH -> new Vector3f[]{
                    new Vector3f(1.0F, 0.25F, 1.0F), new Vector3f(1.0F, 0.75F, 1.0F),
                    new Vector3f(0.0F, 0.75F, 1.0F), new Vector3f(0.0F, 0.25F, 1.0F)};
            case WEST -> new Vector3f[]{
                    new Vector3f(0.0F, 0.25F, 1.0F), new Vector3f(0.0F, 0.75F, 1.0F),
                    new Vector3f(0.0F, 0.75F, 0.0F), new Vector3f(0.0F, 0.25F, 0.0F)};
            case EAST -> new Vector3f[]{
                    new Vector3f(1.0F, 0.25F, 0.0F), new Vector3f(1.0F, 0.75F, 0.0F),
                    new Vector3f(1.0F, 0.75F, 1.0F), new Vector3f(1.0F, 0.25F, 1.0F)};
        };
    }

    private static TextureAtlasSprite sprite(String name, int x, int y) {
        NativeImage image = new NativeImage(16, 16, false);
        SpriteContents contents = new SpriteContents(
                Identifier.withDefaultNamespace(name),
                new FrameSize(16, 16),
                image);
        return new TestSprite(contents, x, y);
    }

    private static final class TestSprite extends TextureAtlasSprite {
        private TestSprite(SpriteContents contents, int x, int y) {
            super(TextureAtlas.LOCATION_BLOCKS, contents, 256, 256, x, y, 0);
        }
    }
}
