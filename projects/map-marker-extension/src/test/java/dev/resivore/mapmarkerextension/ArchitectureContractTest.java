package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class ArchitectureContractTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));

    @Test
    void metadataDeclaresCommonRegistrationAndOptionalClientOnlyXaeroIntegration()
        throws IOException {
        String metadata = Files.readString(PROJECT.resolve("src/main/resources/fabric.mod.json"));

        assertTrue(metadata.contains("\"id\": \"map_marker_extension\""));
        assertTrue(metadata.contains("\"name\": \"Map Marker Extension\""));
        assertTrue(metadata.contains("\"environment\": \"*\""));
        assertTrue(metadata.contains("\"main\""));
        assertTrue(metadata.contains("dev.resivore.mapmarkerextension.MapMarkerExtension"));
        assertTrue(metadata.contains("\"client\""));
        assertTrue(metadata.contains("dev.resivore.mapmarkerextension.MapMarkerExtensionClient"));
        assertTrue(metadata.contains("\"suggests\""));
        String depends = metadata.substring(metadata.indexOf("\"depends\""),
            metadata.indexOf("\"suggests\""));
        assertFalse(depends.contains("xaerominimap"));
        assertFalse(depends.contains("xaeroworldmap"));
    }

    @Test
    void commonPathRegistersTypesAndNormalizesOnlyTheNativeExplorationEntry()
        throws IOException {
        String initializer = source("MapMarkerExtension.java");
        String decorationTypes = source("core/MapMarkerDecorationTypes.java");
        String normalizer = source("core/MapMarkerNormalizer.java");
        String mixin = source("mixin/MapItemSavedDataMixin.java");
        String mixinConfig = Files.readString(PROJECT.resolve(
            "src/main/resources/map_marker_extension.mixins.json"
        ));

        assertTrue(initializer.contains("MapMarkerDecorationTypes.register()"));
        assertTrue(decorationTypes.contains("Registry.registerForHolder("));
        assertTrue(decorationTypes.contains("source.showOnItemFrame()"));
        assertTrue(decorationTypes.contains("source.mapColor()"));
        assertTrue(decorationTypes.contains("source.explorationMapElement()"));
        assertTrue(decorationTypes.contains("source.trackCount()"));
        assertTrue(normalizer.contains("EXPLORATION_TARGET_KEY = \"+\""));
        assertTrue(normalizer.contains("DataComponents.MAP_DECORATIONS"));
        assertTrue(normalizer.contains("decorations.withDecoration("));
        assertTrue(normalizer.contains("current.x(), current.z(), current.rotation()"));
        assertTrue(mixin.contains("@Inject(method = \"tickCarriedBy\", at = @At(\"HEAD\"))"));
        assertTrue(mixin.contains("MapMarkerNormalizer.normalize(stack)"));
        assertTrue(mixin.contains("addDecoration("));
        assertTrue(mixinConfig.contains("MapItemSavedDataMixin"));
        assertFalse(productionJava().contains("MapRendererMixin"));
        assertFalse(productionJava().contains("atlasSprite ="));
    }

    @Test
    void commonCodeHasNoClientXaeroCompassOrIndependentDiscoveryDependency()
        throws IOException {
        StringBuilder common = new StringBuilder(source("MapMarkerExtension.java"));
        for (String directory : new String[] {"core", "mixin"}) {
            try (Stream<Path> files = Files.walk(PROJECT.resolve(
                "src/main/java/dev/resivore/mapmarkerextension/" + directory
            ))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    common.append(Files.readString(file));
                }
            }
        }
        String text = common.toString();

        assertFalse(text.contains("net.minecraft.client"));
        assertFalse(text.contains("xaero."));
        assertFalse(text.toLowerCase().contains("compassribbon"));
        assertFalse(text.contains("StructureManager"));
        assertFalse(text.contains("findNearestMapStructure"));
        assertFalse(text.contains("ChunkGenerator"));
        assertFalse(text.contains("getSeed("));
        assertFalse(text.contains("ServerPlayNetworking"));
        assertFalse(text.contains("ClientPlayNetworking"));
    }

    @Test
    void scannerRemainsPossessionGatedEphemeralAndDimensionExplicit() throws IOException {
        String scanner = source("client/CarriedMapScanner.java");
        String repository = source("client/MapMarkerTargetRepository.java");

        assertTrue(scanner.contains("client.player.getInventory()"));
        assertTrue(scanner.contains("inventory.getContainerSize()"));
        assertTrue(scanner.contains("inventory.getItem(slot)"));
        assertTrue(scanner.contains("Items.FILLED_MAP"));
        assertTrue(scanner.contains("DataComponents.MAP_ID"));
        assertTrue(scanner.contains("DataComponents.MAP_DECORATIONS"));
        assertTrue(scanner.contains("Level.OVERWORLD.identifier().toString()"));
        assertTrue(scanner.contains("client.player == null || client.level == null"));
        assertTrue(scanner.contains("targets.clear()"));
        assertFalse(repository.contains("java.nio.file"));
        assertFalse(productionJava().toLowerCase().contains("waypoint"));
        assertFalse(productionJava().toLowerCase().contains("terrain discovery"));
    }

    @Test
    void noCompassRibbonCodeResourceOrBuildLinkExists() throws IOException {
        String production = productionJava().toLowerCase();
        String build = Files.readString(PROJECT.resolve("build.gradle")).toLowerCase();
        assertFalse(production.contains("compassribbon"));
        assertFalse(production.contains("compass_ribbon"));
        assertFalse(build.contains("compass"));
        try (Stream<Path> files = Files.walk(PROJECT.resolve("src/main/resources"))) {
            assertTrue(files.noneMatch(path -> path.toString().toLowerCase().contains("compass")));
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(PROJECT.resolve(
            "src/main/java/dev/resivore/mapmarkerextension/" + relativePath
        ));
    }

    private static String productionJava() throws IOException {
        StringBuilder result = new StringBuilder();
        try (Stream<Path> files = Files.walk(PROJECT.resolve("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                result.append(Files.readString(file)).append('\n');
            }
        }
        return result.toString();
    }
}
