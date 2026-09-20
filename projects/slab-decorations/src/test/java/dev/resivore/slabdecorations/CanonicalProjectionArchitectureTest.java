package dev.resivore.slabdecorations;

import net.minecraft.world.level.LevelReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalProjectionArchitectureTest {
    @Test
    void eligibilityIsStructuralAndContainsNoSpeciesPermissionTable() throws IOException {
        String bytecode = classFile(PlantFamilyEligibility.class);

        assertFalse(bytecode.contains("net/minecraft/world/level/block/Blocks"),
                "eligibility must not name individual vanilla block IDs");
        assertFalse(bytecode.contains("net/minecraft/core/registries/BuiltInRegistries"),
                "eligibility must not build a registry-ID permission table");
        assertFalse(Arrays.stream(PlantFamilyEligibility.class.getDeclaredFields())
                        .anyMatch(field -> Collection.class.isAssignableFrom(field.getType())
                                || field.getType().isArray()),
                "eligibility must not retain a collection/array species allowlist");

        for (String structuralContract : new String[]{
                "VegetationBlock",
                "DoublePlantBlock",
                "BigDripleafBlock",
                "BigDripleafStemBlock",
                "MossyCarpetBlock",
                "CarpetBlock",
                "HangingRootsBlock",
                "SporeBlossomBlock",
                "HangingMossBlock",
                "GrowingPlantBlock",
                "GrowingPlantBlockAccessor",
                "SugarCaneBlock",
                "BambooSaplingBlock",
                "BambooStalkBlock",
                "CactusBlock",
                "CactusFlowerBlock",
                "BaseCoralPlantTypeBlock"
        }) {
            assertTrue(bytecode.contains(structuralContract),
                    () -> "missing structural family contract " + structuralContract);
        }

        for (String excludedContract : new String[]{
                "SaplingBlock",
                "StemBlock",
                "AttachedStemBlock",
                "ChorusPlantBlock",
                "ChorusFlowerBlock",
                "LilyPadBlock",
                "VineBlock",
                "MultifaceBlock",
                "CocoaBlock",
                "BaseCoralWallFanBlock"
        }) {
            assertTrue(bytecode.contains(excludedContract),
                    () -> "missing explicit structural blacklist contract " + excludedContract);
        }
    }

    @Test
    void directionalAttachmentUsesBidirectionalAndRootColumnContractsWithoutIdCoupling()
            throws IOException {
        String eligibility = classFile(PlantFamilyEligibility.class);
        String surface = classFile(NibaruHorizontalSurface.class);
        String commonMixins = resource("/slab_decorations.mixins.json");
        String metadata = resource("/fabric.mod.json");

        assertTrue(eligibility.contains("UPWARD_GROWING_COLUMN")
                        && eligibility.contains("DOWNWARD_GROWING_COLUMN")
                        && eligibility.contains("SUGAR_CANE_COLUMN")
                        && eligibility.contains("BAMBOO_COLUMN")
                        && eligibility.contains("CACTUS_COLUMN")
                        && eligibility.contains("CEILING_FOLIAGE"),
                "directional structural families are absent");
        assertTrue(surface.contains("AttachmentOrientation")
                        && surface.contains("CEILING_TOP_OFFSET")
                        && surface.contains("slabDecorations$invokeGetHeadBlock")
                        && surface.contains("slabDecorations$invokeGetBodyBlock")
                        && surface.contains("upwardColumnAttachment")
                        && surface.contains("growthDirection"),
                "surface resolution does not share generic directional/root attachment contracts");
        assertTrue(commonMixins.contains("GrowingPlantBlockAccessor"),
                "generic growing-plant accessor is not registered");
        assertFalse(eligibility.contains("mynx_regions_unexplored")
                        || surface.contains("mynx_regions_unexplored")
                        || metadata.contains("mynx_regions_unexplored"),
                "RU compatibility must not become an ID exception or production dependency");
    }

    @Test
    void survivalPermissionDelegatesThroughAReadOnlyGuardedProjection() throws Exception {
        Class<?> projection = CanonicalSurvivalProjection.class;
        Class<?> projectedReader = Class.forName(
                "dev.resivore.slabdecorations.CanonicalSupportLevelReader", false,
                projection.getClassLoader());

        assertTrue(LevelReader.class.isAssignableFrom(projectedReader),
                "canonical support view must remain a read-only LevelReader");
        assertTrue(Modifier.isFinal(projectedReader.getModifiers()),
                "canonical support view must not be extensible into a mutating world adapter");
        assertTrue(Arrays.stream(projectedReader.getDeclaredFields())
                        .allMatch(field -> Modifier.isFinal(field.getModifiers())),
                "projected support state and delegate must be immutable references");

        String projectionBytecode = classFile(projection);
        String readerBytecode = classFile(projectedReader);
        assertTrue(projectionBytecode.contains("CanonicalSupportLevelReader")
                        && projectionBytecode.contains("canSurvive")
                        && projectionBytecode.contains("java/lang/ThreadLocal"),
                "permission must call ordinary canSurvive through a thread-scoped projection");
        for (String mutation : new String[]{"setBlock", "destroyBlock", "removeBlock"}) {
            assertFalse(projectionBytecode.contains(mutation),
                    () -> "survival projection must not call " + mutation);
            assertFalse(readerBytecode.contains(mutation),
                    () -> "projected reader must not call " + mutation);
        }
    }

    @Test
    void interactionAndRenderingSeamsRemainRegistered() throws Exception {
        String commonMixins = resource("/slab_decorations.mixins.json");
        String clientMixins = resource("/slab_decorations.client.mixins.json");
        String metadata = resource("/fabric.mod.json");

        assertTrue(commonMixins.contains("BlockStateBaseSurvivalMixin"),
                "global canSurvive projection seam is not registered");
        assertTrue(commonMixins.contains("BlockStateBaseShapeMixin"),
                "outline/collision alignment seam is not registered");
        assertTrue(commonMixins.contains("CropBlockFertilityMixin"),
                "exact BGE Farmland crop-fertility projection seam is not registered");
        assertTrue(commonMixins.contains("EnderscapeVeiledSaplingGrowthMixin")
                        && commonMixins.contains("EnderscapeChanterelleGrowthMixin")
                        && commonMixins.contains("EnderscapeVoidTorchParticleMixin")
                        && commonMixins.contains("EnderscapeBulbLanternParticleMixin"),
                "Enderscape's optional growth and particle seams are not registered");
        assertTrue(commonMixins.contains("GrowingPlantBlockAccessor"),
                "generic growing-column contract seam is not registered");
        assertTrue(commonMixins.contains("RibbitsToadstoolGrowthMixin"),
                "optional Ribbits huge-toadstool transaction seam is not registered");
        assertTrue(commonMixins.contains("BambooPlacementMixin")
                        && commonMixins.contains("NetherFungusBonemealMixin"),
                "bamboo lifecycle or deferred huge-fungus boundary seam is not registered");
        assertTrue(clientMixins.contains("EntityPickMixin"),
                "shifted targeting seam is not registered");
        assertTrue(clientMixins.contains("LevelRendererDestroyOverlayMixin"),
                "shifted breaking-overlay seam is not registered");
        assertTrue(clientMixins.contains("RenderSectionRegionAccessor")
                        && clientMixins.contains("SodiumLevelSliceAccessor"),
                "vanilla and Sodium terrain snapshot bridges must both remain registered");

        String model = classFile(Class.forName(
                "dev.resivore.slabdecorations.client.SurfaceOffsetModel", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        String client = classFile(Class.forName(
                "dev.resivore.slabdecorations.client.SlabDecorationsClient", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        assertTrue(model.contains("RenderSectionRegionAccessor")
                        && model.contains("SodiumLevelSliceAccessor")
                        && model.contains("visibleOffset"),
                "model translation must resolve both terrain snapshot families through the shared offset");
        assertTrue(client.contains("WRAP_LAST_PHASE")
                        && client.contains("SurfaceOffsetModel"),
                "state-selected model variants, including cave-vine berry models, must be wrapped after"
                        + " any specialized renderer wrapper is installed");
        assertFalse(client.contains("PlantFamilyEligibility") || client.contains("isEligible"),
                "client model wrapping must not maintain an independent eligibility/species gate");

        String ribbits = classFile(Class.forName(
                "dev.resivore.slabdecorations.mixin.RibbitsToadstoolGrowthMixin", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        String transaction = classFile(StructureGrowthTransaction.class);
        assertTrue(ribbits.contains("ToadstoolBlock")
                        && ribbits.contains("growHugeToadstool")
                        && ribbits.contains("StructureGrowthTransaction")
                        && ribbits.contains("org/spongepowered/asm/mixin/Pseudo"),
                "Ribbits huge growth must be an optional private-feature seam around the shared transaction");
        assertTrue(transaction.contains("ribbits") && transaction.contains("toadstool_stem"),
                "only the exact Ribbits toadstool stem may continue a successful transaction");
        assertFalse(metadata.contains("\"ribbits\""),
                "Ribbits must remain an optional compatibility target, not a production dependency");

        String enderscapeGrowth = classFile(Class.forName(
                "dev.resivore.slabdecorations.mixin.EnderscapeChanterelleGrowthMixin", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        String enderscapeSapling = classFile(Class.forName(
                "dev.resivore.slabdecorations.mixin.EnderscapeVeiledSaplingGrowthMixin", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        String voidTorchParticles = classFile(Class.forName(
                "dev.resivore.slabdecorations.mixin.EnderscapeVoidTorchParticleMixin", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        String bulbLanternParticles = classFile(Class.forName(
                "dev.resivore.slabdecorations.mixin.EnderscapeBulbLanternParticleMixin", false,
                CanonicalProjectionArchitectureTest.class.getClassLoader()));
        assertTrue(enderscapeGrowth.contains("MurublightChanterelleBlock")
                        && enderscapeGrowth.contains("CelestialChanterelleBlock")
                        && enderscapeSapling.contains("VeiledSaplingBlock")
                        && enderscapeGrowth.contains("isValidBonemealTarget")
                        && enderscapeSapling.contains("isValidBonemealTarget")
                        && enderscapeGrowth.contains("evaluateWithCanonicalSupport")
                        && enderscapeSapling.contains("evaluateWithCanonicalSupport")
                        && transaction.contains("RotatedPillarBlock")
                        && transaction.contains("CEILING"),
                "Enderscape vertical growth must use the shared, bidirectional transaction");
        assertTrue(voidTorchParticles.contains("addParticle") && voidTorchParticles.contains("visibleOffset")
                        && bulbLanternParticles.contains("addParticle") && bulbLanternParticles.contains("visibleOffset"),
                "Enderscape decoration particles must use the resolved surface translation");
        assertFalse(metadata.contains("\"enderscape\""),
                "Enderscape remains an optional compatibility target, not a production dependency");
    }

    private static String classFile(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        return resource(resource, StandardCharsets.ISO_8859_1);
    }

    private static String resource(String path) throws IOException {
        return resource(path, StandardCharsets.UTF_8);
    }

    private static String resource(String path, java.nio.charset.Charset charset) throws IOException {
        try (InputStream input = CanonicalProjectionArchitectureTest.class.getResourceAsStream(path)) {
            assertNotNull(input, () -> "missing classpath resource " + path);
            return new String(input.readAllBytes(), charset);
        }
    }
}
