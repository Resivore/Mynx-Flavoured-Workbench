package dev.resivore.rooteddirtqol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectScopeContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path MAIN_RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");

    @Test
    void packagedResourcesContainNoAdvancementOrUserFacingRegistryContent() throws Exception {
        Set<String> resources;
        try (var paths = Files.walk(MAIN_RESOURCES)) {
            resources = paths
                .filter(Files::isRegularFile)
                .map(MAIN_RESOURCES::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        }

        assertEquals(Set.of(
            "fabric.mod.json",
            "rooted_dirt_qol.mixins.json",
            "data/rooted_dirt_qol/recipe/rooted_dirt.json"
        ), resources);
        assertTrue(resources.stream().noneMatch(path -> path.contains("advancement")));
        assertTrue(resources.stream().noneMatch(path -> path.contains("assets/")));
        assertTrue(resources.stream().noneMatch(path -> path.contains("item/")));
        assertTrue(resources.stream().noneMatch(path -> path.contains("block/")));
    }

    @Test
    void metadataHasNoEntrypointOrRegistrationSurface() throws Exception {
        JsonObject metadata = JsonParser.parseString(Files.readString(
            MAIN_RESOURCES.resolve("fabric.mod.json")
        )).getAsJsonObject();

        assertEquals("rooted_dirt_qol", metadata.get("id").getAsString());
        assertEquals("*", metadata.get("environment").getAsString());
        assertEquals(1, metadata.getAsJsonArray("mixins").size());
        assertEquals("rooted_dirt_qol.mixins.json",
            metadata.getAsJsonArray("mixins").get(0).getAsString());
        assertFalse(metadata.has("entrypoints"));
        assertFalse(metadata.has("accessWidener"));
    }

    @Test
    void productionJavaContainsOnlyTheTargetedMixinAndNoRegistrations() throws Exception {
        Set<String> javaFiles;
        try (var paths = Files.walk(MAIN_JAVA)) {
            javaFiles = paths
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                .map(MAIN_JAVA::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .collect(Collectors.toSet());
        }
        assertEquals(Set.of(
            "dev/resivore/rooteddirtqol/mixin/HangingRootsBlockMixin.java"
        ), javaFiles);

        String productionSource = Files.readString(MAIN_JAVA.resolve(
            "dev/resivore/rooteddirtqol/mixin/HangingRootsBlockMixin.java"
        ));
        assertFalse(productionSource.contains("Registry.register"));
        assertFalse(productionSource.contains("BuiltInRegistries"));
        assertFalse(productionSource.contains("new Item("));
        assertFalse(productionSource.contains("new Block("));
        assertFalse(productionSource.contains("RootedDirtBlock"));
    }
}
