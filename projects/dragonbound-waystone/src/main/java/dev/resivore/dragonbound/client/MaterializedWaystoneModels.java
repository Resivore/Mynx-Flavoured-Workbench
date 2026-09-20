package dev.resivore.dragonbound.client;

import dev.resivore.dragonbound.material.WaystoneMaterial;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
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
        return rebakeSpriteLocal(waystoneQuad, donor).orElse(waystoneQuad);
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

        EnumMap<Direction, List<BakedQuad.MaterialInfo>> exteriorCandidates = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            exteriorCandidates.put(direction, new ArrayList<>());
        }
        for (BlockStateModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : part.getQuads(direction)) {
                    // getQuads(direction) is the model's cull-specific exterior data. An entry
                    // whose geometric direction disagrees is not a safe face-material source.
                    if (quad.direction() != direction) {
                        return Optional.empty();
                    }
                    exteriorCandidates.get(direction).add(quad.materialInfo());
                }
            }
        }

        return DirectionalMaterialResolver.resolve(exteriorCandidates, MaterializedWaystoneModels::safeOpaqueMaterial)
                .map(faces -> new DirectionalMaterial(blockId, faces));
    }

    private static boolean safeOpaqueMaterial(BakedQuad.MaterialInfo material) {
        return material.tintIndex() < 0
                && material.lightEmission() == 0
                && material.layer() == ChunkSectionLayer.SOLID
                && material.sprite().transparency().isOpaque();
    }

    /**
     * Rebuilds a baked Waystone quad through Fabric's 26.2 sprite baker. Minecraft stores
     * BakedQuad UVs as raw atlas coordinates; Fabric's materialBake expects sprite-local
     * normalized coordinates, then performs the target-sprite interpolation itself.
     */
    private static Optional<BakedQuad> rebakeSpriteLocal(
            BakedQuad waystoneQuad,
            BakedQuad.MaterialInfo donor) {
        TextureAtlasSprite sourceSprite = waystoneQuad.materialInfo().sprite();
        TextureAtlasSprite donorSprite = donor.sprite();
        BakedQuad[] output = new BakedQuad[1];
        QuadEmitter emitter = Renderer.get().quadEmitter(view -> output[0] = view.toBakedQuad(donorSprite));
        emitter.fromBakedQuad(waystoneQuad);

        if (!SpriteLocalUvs.unbake(emitter, sourceSprite)) {
            return Optional.empty();
        }

        // BAKE_NORMALIZED keeps our [0, 1] source-local coordinates intact. Fabric's sprite
        // baker maps each one into precisely this donor sprite's atlas rectangle.
        emitter.materialBake(new Material.Baked(donorSprite, false), MutableQuadView.BAKE_NORMALIZED);
        emitter.chunkLayer(donor.layer());
        emitter.itemRenderType(donor.itemRenderType());
        emitter.tintIndex(-1);
        emitter.diffuseShade(donor.shade());
        emitter.emissive(false);
        emitter.emit();

        return Optional.ofNullable(output[0])
                .filter(rebaked -> rebaked.direction() == waystoneQuad.direction())
                .filter(rebaked -> SpriteLocalUvs.isInside(rebaked, donorSprite));
    }

    record DirectionalMaterial(Identifier blockId, EnumMap<Direction, BakedQuad.MaterialInfo> faces) {
        BakedQuad.MaterialInfo face(Direction direction) {
            return faces.get(direction);
        }
    }
}
