package dev.resivore.matchabeacon.contract;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RuntimeInterceptionRegistrationTest {
    @Test
    void fabricMetadataRegistersTheEntrypointAndExactRuntimeMixins() throws IOException {
        Path resources = projectRoot().resolve("src/main/resources");
        String fabricMetadata = compactJson(Files.readString(resources.resolve("fabric.mod.json"), UTF_8));
        assertTrue(fabricMetadata.contains(
                "\"entrypoints\":{\"main\":[\"dev.resivore.matchabeacon.MatchaBeaconKindlingCompat\"]}"));
        assertTrue(fabricMetadata.contains(
                "\"mixins\":[\"matcha_beacon_kindling_compat.mixins.json\"]"),
                "Fabric metadata must install the runtime interception mixins");

        String mixinMetadata = compactJson(Files.readString(
                resources.resolve("matcha_beacon_kindling_compat.mixins.json"), UTF_8));
        assertTrue(mixinMetadata.contains("\"required\":true"));
        assertTrue(mixinMetadata.contains("\"package\":\"dev.resivore.matchabeacon.mixin\""));
        assertTrue(mixinMetadata.contains(
                "\"mixins\":[\"FunctionCommandMixin\",\"ServerFunctionManagerMixin\",\"SpawnEggItemMixin\"]"),
                "The exact nested/top-level function-dispatch and spawn-egg seams must remain registered");
        assertTrue(mixinMetadata.contains("\"defaultRequire\":1"),
                "Missing injections must fail rather than silently expose Matcha's legacy scheduler");
    }

    @Test
    void runtimeInterceptionDoesNotDependOnDataPackOverrideOrdering() throws IOException {
        Path resources = projectRoot().resolve("src/main/resources");
        assertTrue(Files.isDirectory(resources), () -> "Missing main resources directory: " + resources);

        List<String> forbiddenOverrides;
        try (Stream<Path> files = Files.walk(resources)) {
            forbiddenOverrides = files
                    .filter(Files::isRegularFile)
                    .map(resources::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> path.toLowerCase(Locale.ROOT).startsWith("data/main/"))
                    .sorted()
                    .toList();
        }

        assertTrue(forbiddenOverrides.isEmpty(),
                () -> "Compatibility must use runtime interception, not data/main resource overrides: "
                        + forbiddenOverrides);
    }

    private static Path projectRoot() {
        String value = System.getProperty("projectRoot");
        assertNotNull(value, "Gradle must provide the projectRoot system property");
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static String compactJson(String json) {
        return json.replaceAll("\\s+", "");
    }
}
