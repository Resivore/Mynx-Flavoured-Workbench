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
                "GrowingPlantBlockAccessor"
        }) {
            assertTrue(bytecode.contains(structuralContract),
                    () -> "missing structural family contract " + structuralContract);
        }
    }

    @Test
    void directionalAttachmentUsesGenericDownwardContractsWithoutRuProductionCoupling()
            throws IOException {
        String eligibility = classFile(PlantFamilyEligibility.class);
        String surface = classFile(NibaruHorizontalSurface.class);
        String commonMixins = resource("/slab_decorations.mixins.json");
        String metadata = resource("/fabric.mod.json");

        assertTrue(eligibility.contains("DOWNWARD_GROWING_COLUMN")
                        && eligibility.contains("CEILING_FOLIAGE"),
                "directional structural families are absent");
        assertTrue(surface.contains("AttachmentOrientation")
                        && surface.contains("CEILING_TOP_OFFSET")
                        && surface.contains("slabDecorations$invokeGetHeadBlock")
                        && surface.contains("slabDecorations$invokeGetBodyBlock"),
                "surface resolution does not use the generic exact head/body attachment contract");
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
    void interactionAndRenderingSeamsRemainRegistered() throws IOException {
        String commonMixins = resource("/slab_decorations.mixins.json");
        String clientMixins = resource("/slab_decorations.client.mixins.json");

        assertTrue(commonMixins.contains("BlockStateBaseSurvivalMixin"),
                "global canSurvive projection seam is not registered");
        assertTrue(commonMixins.contains("BlockStateBaseShapeMixin"),
                "outline/collision alignment seam is not registered");
        assertTrue(commonMixins.contains("GrowingPlantBlockAccessor"),
                "generic growing-column contract seam is not registered");
        assertTrue(clientMixins.contains("EntityPickMixin"),
                "shifted targeting seam is not registered");
        assertTrue(clientMixins.contains("LevelRendererDestroyOverlayMixin"),
                "shifted breaking-overlay seam is not registered");
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
