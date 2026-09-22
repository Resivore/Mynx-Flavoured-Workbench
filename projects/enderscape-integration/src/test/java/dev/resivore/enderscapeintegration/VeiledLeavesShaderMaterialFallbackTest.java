package dev.resivore.enderscapeintegration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.enderscapeintegration.client.VeiledLeavesShaderMaterialFallback;
import dev.resivore.enderscapeintegration.mixin.IrisVeiledLeavesMaterialMappingMixin;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;

class VeiledLeavesShaderMaterialFallbackTest {
    private static final String OAK_LEAVES = "minecraft:oak_leaves[distance=7,persistent=false,waterlogged=false]";
    private static final List<String> VEILED_STATES = List.of(
            "enderscape:veiled_leaves[distance=1,persistent=false,waterlogged=false]",
            "enderscape:veiled_leaves[distance=14,persistent=true,waterlogged=true]");

    @Test
    void liveVanillaLeafClassificationFillsEveryUnmappedCanonicalVeiledLeavesState() {
        Map<String, String> classifications = new LinkedHashMap<>();
        classifications.put(OAK_LEAVES, "active-pack-leaf-classification");

        VeiledLeavesShaderMaterialFallback.Result result = VeiledLeavesShaderMaterialFallback.inheritMissing(
                classifications, VEILED_STATES, OAK_LEAVES);

        VEILED_STATES.forEach(state -> assertEquals("active-pack-leaf-classification", classifications.get(state)));
        assertEquals(2, result.inherited());
        assertEquals(0, result.explicit());
        assertEquals(0, result.missingReference());
    }

    @Test
    void explicitVeiledLeavesShaderMappingWinsOverFallbackEvenWhenItsValueIsZero() {
        Map<String, Integer> classifications = new LinkedHashMap<>();
        classifications.put(OAK_LEAVES, 31);
        classifications.put(VEILED_STATES.getFirst(), 0);

        VeiledLeavesShaderMaterialFallback.Result result = VeiledLeavesShaderMaterialFallback.inheritMissing(
                classifications, VEILED_STATES, OAK_LEAVES);

        assertEquals(0, classifications.get(VEILED_STATES.getFirst()));
        assertEquals(31, classifications.get(VEILED_STATES.getLast()));
        assertEquals(1, result.inherited());
        assertEquals(1, result.explicit());
    }

    @Test
    void missingVanillaReferenceLeavesTheCompatibilityPathInert() {
        Map<String, Integer> classifications = new LinkedHashMap<>();

        VeiledLeavesShaderMaterialFallback.Result result = VeiledLeavesShaderMaterialFallback.inheritMissing(
                classifications, VEILED_STATES, OAK_LEAVES);

        VEILED_STATES.forEach(state -> assertFalse(classifications.containsKey(state)));
        assertEquals(0, result.inherited());
        assertEquals(2, result.missingReference());
    }

    @Test
    void exactReferencesEstablishTheCanonicalLeafSeamWithoutBundlingExternalContent() throws Exception {
        try (ZipFile enderscape = new ZipFile(enderscapeJar().toFile());
             ZipFile complementary = new ZipFile(complementaryZip().toFile())) {
            String model = read(enderscape, "assets/enderscape/models/block/veiled_leaves.json");
            String state = read(enderscape, "assets/enderscape/blockstates/veiled_leaves.json");
            String leafProperties = read(complementary, "shaders/block.properties");

            assertTrue(model.contains("minecraft:block/leaves"));
            assertTrue(model.contains("enderscape:block/veiled_leaves"));
            assertTrue(state.contains("enderscape:block/veiled_leaves"));
            String vanillaLeafEntry = leafProperties.lines()
                    .filter(line -> line.startsWith("block.") && line.contains("oak_leaves"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(vanillaLeafEntry.contains("leaves"));
            assertFalse(vanillaLeafEntry.contains("veiled_leaves"));
        }
    }

    @Test
    void currentProjectIdentityRetainsThePriorIdentityAndAllHistoricalReleaseMetadata() throws Exception {
        JsonObject status = JsonParser.parseString(Files.readString(projectRoot().resolve("WORKBENCH_STATUS.json")))
                .getAsJsonObject();
        JsonObject identity = status.getAsJsonObject("identity");
        assertEquals("4029bb0e-1167-4eb2-a658-44733d415b3b", identity.get("uuid").getAsString());
        assertEquals("Enderscape Integration", identity.get("name").getAsString());
        assertEquals("enderscape-integration", identity.get("project_id").getAsString());
        assertTrue(strings(identity.getAsJsonArray("legacy_names")).contains("Enderscape Pruning"));
        assertTrue(strings(identity.getAsJsonArray("legacy_ids")).contains("enderscape-pruning"));

        String log = Files.readString(projectRoot().resolve("CODEX_LOG.md"));
        List<String> historicalMetadata = List.of(
                "Source checkpoint: `a3bcd02bda69cf6d2413f1ec59c0a9acf62d8558`",
                "`enderscape-pruning-0.1.0-canary1.jar`, version `0.1.0-canary1`, SHA-256 `93e8b9ae4f4bc010f6ca18b74a0a8ac1744b298e96f3ddfce2c3f876589a2fd5`, built `2026-09-19T07:29:22.9774388Z`",
                "Source checkpoint: `cb0707e9af08f230280dd7f73994153443233924`",
                "`enderscape-pruning-0.1.0-canary2.jar`, version `0.1.0-canary2`, SHA-256 `1da6a13cbd5a0027159a45d66cff89261aa5dd53cd9c438a5c3a1b17f4a0039b`, built `2026-09-20T01:52:05.0944502Z`",
                "Source checkpoint: `95f85bbe309ccd4c146414e121880ca4c21968c9`",
                "`enderscape-pruning-0.1.0-canary3.jar`, version `0.1.0-canary3`, SHA-256 `3fd52a2204be416558609706a88f289c2ec1cd50d2897942be57d6946e522e3b`, built `2026-09-21T19:37:06.3977108Z`",
                "Source checkpoint: `7fd96db9504606ca65ee8b3ed21583987646b175`",
                "`enderscape-integration-0.1.0-canary4.jar`, version `0.1.0-canary4`, SHA-256 `20ac7360850f6b62335d333e9328acc2475b248f74c0b4826ed8aadf283bab30`, built `2026-09-22T01:23:24.3593451Z`");
        historicalMetadata.forEach(value -> assertTrue(log.contains(value), value));
    }

    @Test
    void canonicalSeedRunsBeforeTheGenericBgeCompletedMapPass() throws Exception {
        Method callback = java.util.Arrays.stream(IrisVeiledLeavesMaterialMappingMixin.class.getDeclaredMethods())
                .filter(method -> method.getName().contains("inheritVeiledLeavesMaterialId"))
                .findFirst()
                .orElseThrow();
        Inject injection = callback.getAnnotation(Inject.class);
        int defaultInjectOrder = (Integer) Inject.class.getMethod("order").getDefaultValue();

        assertEquals(900, injection.order());
        assertEquals(1000, defaultInjectOrder);
        assertTrue(injection.order() < defaultInjectOrder);

        String canonical = VEILED_STATES.getFirst();
        String physical = "bge:veiled_leaves_layer[layers=1,facing=up,distance=1,persistent=false]";
        Map<String, Integer> classifications = new LinkedHashMap<>();
        classifications.put(OAK_LEAVES, 31);

        VeiledLeavesShaderMaterialFallback.inheritMissing(classifications, List.of(canonical), OAK_LEAVES);
        VeiledLeavesShaderMaterialFallback.inheritMissing(classifications, List.of(physical), canonical);

        assertEquals(31, classifications.get(canonical));
        assertEquals(31, classifications.get(physical));

        String bgeMixin = Files.readString(workbenchRoot().resolve(
                "projects/bge-complementary/src/client/java/dev/resivore/bgecomplementary/mixin/"
                        + "IrisBgeMaterialMappingMixin.java"));
        assertFalse(Pattern.compile("\\border\\s*=").matcher(bgeMixin).find());
    }

    @Test
    void compatibilityIsCanonicalOnlyAndLeavesBgeComplementaryGeneric() throws Exception {
        String bridge = mainSource("client/VeiledLeavesShaderMaterialBridge.java");
        String mixin = mainSource("mixin/IrisVeiledLeavesMaterialMappingMixin.java");
        String allIntegrationSource = javaSources(projectRoot().resolve("src/main/java"));
        Path bgeSource = workbenchRoot().resolve("projects/bge-complementary/src");
        String allBgeProductionSource = javaSources(bgeSource.resolve("main/java"), bgeSource.resolve("client/java"));

        assertTrue(bridge.contains("enderscape\", \"veiled_leaves"));
        assertTrue(bridge.contains("Blocks.OAK_LEAVES.defaultBlockState()"));
        assertFalse(bridge.contains("10009"));
        assertFalse(bridge.contains("bge:"));
        assertTrue(mixin.contains("createBlockStateIdMap(Lit/unimi/dsi/fastutil/ints/Int2ObjectLinkedOpenHashMap;"));
        assertTrue(mixin.contains("order = 900"));
        assertFalse(mixin.contains("priority = 1100"));
        assertFalse(allIntegrationSource.contains("block.10009"));
        assertFalse(allBgeProductionSource.toLowerCase(java.util.Locale.ROOT).contains("enderscape"));
        assertFalse(allBgeProductionSource.toLowerCase(java.util.Locale.ROOT).contains("veiled"));
    }

    private static List<String> strings(JsonArray values) {
        return values.asList().stream().map(value -> value.getAsString()).toList();
    }

    private static String mainSource(String relative) throws IOException {
        return Files.readString(projectRoot().resolve("src/main/java/dev/resivore/enderscapeintegration").resolve(relative));
    }

    private static String read(ZipFile zip, String name) throws IOException {
        try (InputStream stream = zip.getInputStream(zip.getEntry(name))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String readPath(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String javaSources(Path... roots) throws IOException {
        StringBuilder sources = new StringBuilder();
        for (Path root : roots) {
            try (var paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .map(VeiledLeavesShaderMaterialFallbackTest::readPath)
                        .forEach(sources::append);
            }
        }
        return sources.toString();
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }

    private static Path workbenchRoot() {
        return Path.of(System.getProperty("workbenchRoot")).toAbsolutePath().normalize();
    }

    private static Path enderscapeJar() {
        return workbenchRoot().resolve("originals/mods/enderscape-fabric-3.0.2+mc26.2.jar");
    }

    private static Path complementaryZip() {
        return workbenchRoot().resolve("originals/shaderpacks/ComplementaryUnbound_r5.8.1.zip");
    }
}
