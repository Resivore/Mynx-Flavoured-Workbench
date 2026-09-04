package dev.resivore.carryonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class ProductionArtifactContractTest {
    private static final Path PATCH = propertyPath("patchJar");

    @Test
    void packagedMetadataIsClientOnlyAndRequiresTheExactAuditedFloor() throws Exception {
        assertEquals("carry-on-patch-0.1.0-canary2.jar", PATCH.getFileName().toString());
        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            String metadata = readUtf8(zip, "fabric.mod.json");
            assertJsonString(metadata, "id", "carry_on_patch");
            assertJsonString(metadata, "version", "0.1.0-canary2");
            assertJsonString(metadata, "environment", "client");
            assertJsonString(metadata, "fabricloader", ">=0.19.3");
            assertJsonString(metadata, "minecraft", "=26.2");
            assertJsonString(metadata, "java", ">=25");
            assertJsonString(metadata, "grapandgo", ">=1.0.1");
            assertFalse(metadata.matches("(?s).*\\\"fabric-api\\\"\\s*:.*"));
            assertTrue(metadata.matches("(?s).*\\\"config\\\"\\s*:\\s*"
                    + "\\\"carry_on_patch\\.client\\.mixins\\.json\\\".*"));

            String mixins = readUtf8(zip, "carry_on_patch.client.mixins.json");
            assertJsonBoolean(mixins, "required", true);
            assertJsonString(mixins, "package", "dev.resivore.carryonpatch.mixin");
            assertJsonInt(mixins, "defaultRequire", 1);
            assertTrue(mixins.matches("(?s).*\\\"client\\\"\\s*:\\s*\\[.*"
                    + "\\\"EntityIdAccessor\\\".*"
                    + "\\\"CarriedObjectFeatureRendererMixin\\\".*\\].*"));
        }
    }

    @Test
    void packagedPayloadContainsOnlyPatchClassesAndNoForeignPayload() throws Exception {
        Set<String> expectedClasses = Set.of(
                "dev/resivore/carryonpatch/RenderOnlyEntityIds.class",
                "dev/resivore/carryonpatch/RibbitCarryPlacement.class",
                "dev/resivore/carryonpatch/mixin/FirstPersonCarryPlacementMixin.class",
                "dev/resivore/carryonpatch/RenderIdAssigningEntityCache.class",
                "dev/resivore/carryonpatch/mixin/EntityIdAccessor.class",
                "dev/resivore/carryonpatch/mixin/CarriedObjectFeatureRendererMixin.class");

        try (ZipFile zip = new ZipFile(PATCH.toFile())) {
            List<String> names = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName().replace('\\', '/'))
                    .toList();
            Set<String> classes = new TreeSet<>();
            List<String> forbidden = new ArrayList<>();

            for (String name : names) {
                String folded = name.toLowerCase(java.util.Locale.ROOT);
                if (name.endsWith(".class")) {
                    classes.add(name);
                    if (!name.startsWith("dev/resivore/carryonpatch/")) {
                        forbidden.add(name);
                    }
                }
                if (name.startsWith("org/chermew/")
                        || name.startsWith("net/minecraft/")
                        || name.startsWith("assets/")
                        || name.startsWith("data/")
                        || name.startsWith("META-INF/jars/")
                        || folded.endsWith(".jar")
                        || folded.endsWith(".refmap.json")) {
                    forbidden.add(name);
                }
            }

            assertEquals(new TreeSet<>(expectedClasses), classes);
            assertTrue(forbidden.isEmpty(), () -> "forbidden packaged payload: " + forbidden);
            assertFalse(names.stream().anyMatch(name -> name.contains("GrabAndGo")));
        }
    }

    private static String readUtf8(ZipFile zip, String entryName) throws IOException {
        var entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertJsonString(String json, String key, String value) {
        assertTrue(json.matches("(?s).*\\\"" + java.util.regex.Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"" + java.util.regex.Pattern.quote(value)
                + "\\\".*"), () -> key + " must be " + value);
    }

    private static void assertJsonBoolean(String json, String key, boolean value) {
        assertTrue(json.matches("(?s).*\\\"" + java.util.regex.Pattern.quote(key)
                + "\\\"\\s*:\\s*" + value + ".*"), () -> key + " must be " + value);
    }

    private static void assertJsonInt(String json, String key, int value) {
        assertTrue(json.matches("(?s).*\\\"" + java.util.regex.Pattern.quote(key)
                + "\\\"\\s*:\\s*" + value + "(?:\\s|,|}).*"),
                () -> key + " must be " + value);
    }

    private static Path propertyPath(String property) {
        return Path.of(Objects.requireNonNull(System.getProperty(property), property));
    }
}
