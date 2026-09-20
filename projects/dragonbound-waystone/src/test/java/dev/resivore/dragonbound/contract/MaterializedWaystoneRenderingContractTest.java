package dev.resivore.dragonbound.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contracts for the 26.2 Fabric baked-model seam, compiled against the production client APIs. */
final class MaterializedWaystoneRenderingContractTest {
    private static final Path CLIENT = Path.of("src/main/java/dev/resivore/dragonbound/client");

    @Test
    void placedAndItemRenderingUseTheSameDirectionalMaterialResolver() throws IOException {
        String block = read("MaterializedWaystoneBlockStateModel.java");
        String item = read("MaterializedWaystoneItemModel.java");

        assertTrue(block.contains("waystone.visualMaterialId().flatMap(MaterializedWaystoneModels::resolve)"));
        assertTrue(item.contains("MaterializedWaystoneModels.resolve(stack)"));
        assertTrue(item.contains("MaterializedWaystoneModels.retarget(quad, material.get())"));
        assertTrue(block.contains("MaterializedWaystoneModels.retarget(quad, material)"));
    }

    @Test
    void itemMaterializationStartsWithTheCompleteVanillaLayerInsteadOfRebuildingIt() throws IOException {
        String item = read("MaterializedWaystoneItemModel.java");

        assertTrue(item.contains("wrapped.update(state, stack, resolver, displayContext, level, owner, seed)"));
        assertTrue(item.contains("int firstLayer"));
        assertTrue(item.contains("replaceAll(quad ->"));
        assertFalse(item.contains("state.clear()"));
        assertFalse(item.contains("state.newLayer()"));
        assertFalse(item.contains("CuboidItemModelWrapper"));
    }

    @Test
    void directionalDefaultStateBakedFacesDriveUnchangedWaystoneGeometry() throws IOException {
        String models = read("MaterializedWaystoneModels.java");
        String block = read("MaterializedWaystoneBlockStateModel.java");

        assertTrue(models.contains("block.defaultBlockState()"));
        assertTrue(models.contains("getBlockStateModelSet()"));
        assertTrue(models.contains("for (Direction direction : Direction.values())"));
        assertTrue(models.contains("part.getQuads(direction)"));
        assertTrue(models.contains("DirectionalMaterialResolver.resolve"));
        assertTrue(models.contains("material.face(waystoneQuad.direction())"));
        assertFalse(models.contains("getQuads(null)"));
        assertTrue(block.contains("wrapped.collectParts(random, parts)"));
        assertFalse(block.contains("new BakedQuad("));
    }

    @Test
    void spriteRetargetingUsesFabricNormalizedSpriteBakingInsteadOfAtlasArithmetic() throws IOException {
        String models = read("MaterializedWaystoneModels.java");

        assertTrue(models.contains("Renderer.get().quadEmitter"));
        assertTrue(models.contains("SpriteLocalUvs.unbake"));
        assertTrue(models.contains("materialBake(new Material.Baked"));
        assertTrue(models.contains("MutableQuadView.BAKE_NORMALIZED"));
        assertTrue(models.contains("SpriteLocalUvs.isInside"));
        assertFalse(models.contains("remapUv("));
        assertFalse(models.contains("getU(relative"));
        assertFalse(models.contains("getV(relative"));
    }

    @Test
    void unsafeClientMaterialsFailClosedAndCacheResetsOnModelReload() throws IOException {
        String models = read("MaterializedWaystoneModels.java");
        String initializer = read("DragonboundWaystoneClient.java");

        assertTrue(models.contains("material.tintIndex() < 0"));
        assertTrue(models.contains("material.lightEmission() == 0"));
        assertTrue(models.contains("material.layer() == ChunkSectionLayer.SOLID"));
        assertTrue(models.contains("material.sprite().transparency().isOpaque()"));
        assertTrue(initializer.contains("MaterializedWaystoneModels.clearCache()"));
        assertTrue(initializer.contains("modifyBlockModelAfterBake()"));
        assertTrue(initializer.contains("modifyItemModelAfterBake()"));
    }

    private static String read(String filename) throws IOException {
        return Files.readString(CLIENT.resolve(filename));
    }
}
