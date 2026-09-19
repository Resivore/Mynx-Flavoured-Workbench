package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.material.WaystoneMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves one deterministic default-state sprite for every world direction, then applies that
 * source material to the unchanged baked Waystone quads. Client model data is intentionally used
 * here rather than in common code so a dedicated server never classloads rendering APIs.
 */
final class MaterializedWaystoneModels {
    private static final Map<Identifier, Optional<DirectionalMaterial>> MATERIALS = new ConcurrentHashMap<>();

    private MaterializedWaystoneModels() {
    }

    static void clearCache() {
        MATERIALS.clear();
    }

    static Optional<DirectionalMaterial> resolve(ItemStack stack) {
        return WaystoneMaterial.selectedBlockId(stack).flatMap(MaterializedWaystoneModels::resolve);
    }

    static Optional<DirectionalMaterial> resolve(Identifier blockId) {
        return MATERIALS.computeIfAbsent(blockId, MaterializedWaystoneModels::load);
    }

    static BakedQuad retarget(BakedQuad waystoneQuad, DirectionalMaterial material) {
        BakedQuad.MaterialInfo donor = material.face(waystoneQuad.direction());
        TextureAtlasSprite sourceSprite = waystoneQuad.materialInfo().sprite();
        TextureAtlasSprite donorSprite = donor.sprite();
        BakedQuad.MaterialInfo replacement = new BakedQuad.MaterialInfo(
                donorSprite,
                donor.layer(),
                donor.itemRenderType(),
                -1,
                donor.shade(),
                0);
        return new BakedQuad(
                waystoneQuad.position0(),
                waystoneQuad.position1(),
                waystoneQuad.position2(),
                waystoneQuad.position3(),
                remapUv(waystoneQuad.packedUV0(), sourceSprite, donorSprite),
                remapUv(waystoneQuad.packedUV1(), sourceSprite, donorSprite),
                remapUv(waystoneQuad.packedUV2(), sourceSprite, donorSprite),
                remapUv(waystoneQuad.packedUV3(), sourceSprite, donorSprite),
                waystoneQuad.direction(),
                replacement);
    }

    private static Optional<DirectionalMaterial> load(Identifier blockId) {
        if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
            return Optional.empty();
        }
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        if (!WaystoneMaterial.isEligibleBlock(block)) {
            return Optional.empty();
        }

        BlockState defaultState = block.defaultBlockState();
        BlockStateModel model = Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .get(defaultState);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0x4457415953544F4EL), parts);

        EnumMap<Direction, BakedQuad.MaterialInfo> faces = new EnumMap<>(Direction.class);
        for (BlockStateModelPart part : parts) {
            collectFaces(part.getQuads(null), faces);
            for (Direction direction : Direction.values()) {
                collectFaces(part.getQuads(direction), faces);
            }
        }

        if (faces.size() != Direction.values().length) {
            return Optional.empty();
        }
        return Optional.of(new DirectionalMaterial(blockId, faces));
    }

    private static void collectFaces(
            List<BakedQuad> quads,
            EnumMap<Direction, BakedQuad.MaterialInfo> faces) {
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            if (faces.containsKey(quad.direction()) || !safeOpaqueMaterial(material)) {
                continue;
            }
            faces.put(quad.direction(), material);
        }
    }

    private static boolean safeOpaqueMaterial(BakedQuad.MaterialInfo material) {
        return material.tintIndex() < 0
                && material.lightEmission() == 0
                && material.layer() == ChunkSectionLayer.SOLID
                && material.sprite().transparency().isOpaque();
    }

    private static long remapUv(long packed, TextureAtlasSprite source, TextureAtlasSprite target) {
        float sourceU = UVPair.unpackU(packed);
        float sourceV = UVPair.unpackV(packed);
        float relativeU = (sourceU - source.getU0()) / (source.getU1() - source.getU0());
        float relativeV = (sourceV - source.getV0()) / (source.getV1() - source.getV0());
        return UVPair.pack(target.getU(relativeU * 16.0F), target.getV(relativeV * 16.0F));
    }

    record DirectionalMaterial(Identifier blockId, EnumMap<Direction, BakedQuad.MaterialInfo> faces) {
        BakedQuad.MaterialInfo face(Direction direction) {
            return faces.get(direction);
        }
    }
}
