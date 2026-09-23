package dev.resivore.bgecomplementary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ShaderMaterialInheritanceTest {
    @Test
    void previouslyAllowedGlassParentStillInherits() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("minecraft:glass", 17);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge:glass_stair[facing=east,shape=outer_left]"),
                ignored -> Optional.of("minecraft:glass"));

        assertEquals(17, ids.get("bge:glass_stair[facing=east,shape=outer_left]"));
        assertEquals(1, result.inherited());
    }

    @Test
    void magmaBlockInheritsThroughTheUniversalRoute() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("minecraft:magma_block", 87);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge:magma_corner[facing=south]"),
                ignored -> Optional.of("minecraft:magma_block"));

        assertEquals(87, ids.get("bge:magma_corner[facing=south]"));
        assertEquals(1, result.inherited());
    }

    @Test
    void parentOutsideCanaryOneClassesInheritsThroughTheSameRoute() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("minecraft:oak_planks", 63);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge:oak_planks_quarter_column[axis=x]"),
                ignored -> Optional.of("minecraft:oak_planks"));

        assertEquals(63, ids.get("bge:oak_planks_quarter_column[axis=x]"));
        assertEquals(1, result.inherited());
    }

    @Test
    void preservesExplicitPhysicalAssignmentIncludingZero() {
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("minecraft:magma_block", 87);
        ids.put("bge:magma_slab[type=bottom]", 0);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge:magma_slab[type=bottom]"),
                ignored -> Optional.of("minecraft:magma_block"));

        assertEquals(0, ids.get("bge:magma_slab[type=bottom]"));
        assertEquals(1, result.explicitPhysical());
    }

    @Test
    void leavesUnmappedCanonicalParentUnmapped() {
        Map<String, Integer> ids = new LinkedHashMap<>();

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of("bge:deepslate_wall[up=true]"),
                ignored -> Optional.of("minecraft:deepslate"));

        assertFalse(ids.containsKey("bge:deepslate_wall[up=true]"));
        assertEquals(1, result.missingParent());
    }

    @Test
    void retainsBgeStateSpecificCanonicalProjection() {
        String nearLeaf = "minecraft:oak_leaves[distance=1,persistent=false,waterlogged=false]";
        String farLeaf = "minecraft:oak_leaves[distance=7,persistent=true,waterlogged=false]";
        String nearPhysical = "bge:oak_leaves_layer[layers=1,facing=up,distance=1,persistent=false]";
        String farPhysical = "bge:oak_leaves_layer[layers=4,facing=north,distance=7,persistent=true]";
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put(nearLeaf, 21);
        ids.put(farLeaf, 22);
        Map<String, String> projections = Map.of(nearPhysical, nearLeaf, farPhysical, farLeaf);

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(ids,
                List.of(nearPhysical, farPhysical), physical -> Optional.of(projections.get(physical)));

        assertEquals(21, ids.get(nearPhysical));
        assertEquals(22, ids.get(farPhysical));
        assertEquals(2, result.inherited());
    }

    @Test
    void layerInheritanceIsUniversalAndPreservesExplicitPhysicalEntries() {
        Map<String, String> layers = new LinkedHashMap<>();
        layers.put("minecraft:magma_block", "cutout");
        layers.put("bge:oak_planks_wall", "solid");
        Map<String, String> parents = Map.of(
                "bge:magma_layer", "minecraft:magma_block",
                "bge:oak_planks_wall", "minecraft:oak_planks");

        ShaderMaterialInheritance.Result result = ShaderMaterialInheritance.inheritMissing(layers,
                List.of("bge:magma_layer", "bge:oak_planks_wall"),
                physical -> Optional.of(parents.get(physical)));

        assertEquals("cutout", layers.get("bge:magma_layer"));
        assertEquals("solid", layers.get("bge:oak_planks_wall"));
        assertEquals(1, result.inherited());
        assertEquals(1, result.explicitPhysical());
    }

    @Test
    void bridgeHasNoParentCategoryPolicySeam() throws Exception {
        Method inheritance = ShaderMaterialInheritance.class.getDeclaredMethod("inheritMissing",
                Map.class, Iterable.class, java.util.function.Function.class);
        assertEquals(3, inheritance.getParameterCount());
        assertEquals(1, Arrays.stream(ShaderMaterialInheritance.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("inheritMissing")).count());
        assertFalse(Arrays.asList(inheritance.getParameterTypes()).contains(Predicate.class));

        String bridgeSource = Files.readString(Path.of("src/client/java/dev/resivore/bgecomplementary/"
                + "BgeShaderMaterialBridge.java"), StandardCharsets.UTF_8);
        for (String forbidden : List.of("CANARY_ONE_PARENTS", "Set<", "Predicate", "DyeColor",
                "Blocks.", "eligible", "ineligible", "magma")) {
            assertFalse(bridgeSource.contains(forbidden),
                    () -> "BgeShaderMaterialBridge must not contain a parent-category policy: " + forbidden);
        }
        assertTrue(bridgeSource.contains("BgeMaterialBindings.all()"));
        assertTrue(bridgeSource.contains("binding::canonicalState"));
    }

    @Test
    void fabricMetadataRequiresBgeWithoutReleaseNumberGating() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8);

        assertTrue(metadata.contains("\"cnm_terrain_slabs_compat\": \"*\""));
        assertFalse(metadata.contains("canary79"));
        assertFalse(metadata.contains("canary80"));
        assertFalse(metadata.contains("4.2.23"));
        assertFalse(metadata.contains("4.2.24"));
    }

    @Test
    void currentC80ExposesEveryConsumedCanonicalBindingMethod() {
        assertTrue(BgeCanonicalBindingApi.supports(BgeMaterialBindings.class.getClassLoader()),
                "The C80 test baseline must expose BgeMaterialBindings.all(), Binding"
                        + "#physicalBlock(), #canonicalMaterial(), and #canonicalState(BlockState)");
    }

    @Test
    void mixinBootstrapGatesOnlyTheExactIrisHookWithoutBgeOrMinecraftResolution() throws Exception {
        assertTrue(IrisBgeMixinPlugin.activationAllowed(true));
        assertFalse(IrisBgeMixinPlugin.activationAllowed(false));

        String pluginSource = Files.readString(Path.of("src/client/java/dev/resivore/bgecomplementary/"
                + "IrisBgeMixinPlugin.java"), StandardCharsets.UTF_8);
        assertTrue(pluginSource.contains("hasExactVersion(\"iris\", IRIS_VERSION)"));
        assertFalse(pluginSource.contains("BgeCanonicalBindingApi"));
        assertFalse(pluginSource.contains("BgeShaderMaterialBridge"));
        assertFalse(pluginSource.contains("net.minecraft"));
        assertFalse(pluginSource.contains("dev.aero"));
        for (String forbidden : List.of("BGE_VERSION", "canary79", "canary80", "4.2.23", "4.2.24",
                "cnm_terrain_slabs_compat\", BGE")) {
            assertFalse(pluginSource.contains(forbidden),
                    () -> "IrisBgeMixinPlugin must not gate BGE by release string: " + forbidden);
        }

        try (var stream = IrisBgeMixinPlugin.class.getResourceAsStream("IrisBgeMixinPlugin.class")) {
            assertTrue(stream != null);
            String classConstants = new String(stream.readAllBytes(), StandardCharsets.ISO_8859_1);
            for (String forbidden : List.of("BgeCanonicalBindingApi", "BgeShaderMaterialBridge",
                    "dev/aero/cnmterraincompat", "net/minecraft/world/level/block/Block",
                    "net/minecraft/world/level/block/state/BlockState")) {
                assertFalse(classConstants.contains(forbidden),
                        () -> "Mixin-bootstrap plugin must not resolve " + forbidden);
            }
        }
    }

    @Test
    void lateCapabilityFailureIsAControlledNoOp() {
        Map<String, Integer> untouched = new LinkedHashMap<>();

        assertFalse(BgeLateRuntimeBridge.runIfSupported(() -> false,
                () -> untouched.put("must-not-run", 1)));
        assertFalse(untouched.containsKey("must-not-run"));

        assertFalse(BgeLateRuntimeBridge.runIfSupported(() -> true, () -> {
            throw new NoSuchMethodError("future BGE contract changed");
        }));
        assertFalse(untouched.containsKey("must-not-run"));

        assertTrue(BgeLateRuntimeBridge.runIfSupported(() -> true,
                () -> untouched.put("late-bridge-ran", 1)));
        assertEquals(1, untouched.get("late-bridge-ran"));

        try {
            String runtimeSource = Files.readString(Path.of("src/client/java/dev/resivore/"
                    + "bgecomplementary/BgeLateRuntimeBridge.java"), StandardCharsets.UTF_8);
            assertTrue(runtimeSource.contains("BgeCanonicalBindingApi::isAvailable"));
            assertTrue(runtimeSource.contains("BgeShaderMaterialBridge.inheritMaterialIds"));
            assertTrue(runtimeSource.contains("catch (LinkageError ignored)"));
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void spruceTraceUsesAuthoritativeBindingsAndOnlyObservesCompletedIrisMaps() throws Exception {
        String bridge = Files.readString(Path.of("src/client/java/dev/resivore/bgecomplementary/"
                + "BgeShaderMaterialBridge.java"), StandardCharsets.UTF_8);
        String trace = Files.readString(Path.of("src/client/java/dev/resivore/bgecomplementary/"
                + "SpruceIrisMaterialTrace.java"), StandardCharsets.UTF_8);

        assertTrue(bridge.contains("beforeMaterialInheritance(materialIds)"));
        assertTrue(bridge.contains("afterMaterialInheritance(materialIds, spruceBefore)"));
        assertTrue(bridge.contains("beforeLayerInheritance(layerTypes)"));
        assertTrue(bridge.contains("afterLayerInheritance(layerTypes, spruceBefore)"));
        assertTrue(trace.contains("BgeMaterialBindings.all()"));
        assertTrue(trace.contains("binding.canonicalMaterial() == Blocks.SPRUCE_LEAVES"));
        assertTrue(trace.contains("BGE_SPRUCE_IRIS_MATERIAL_TRACE"));
        assertTrue(trace.contains("BGE_SPRUCE_IRIS_LAYER_TRACE"));
        assertTrue(trace.contains("explicit_physical"));
        assertTrue(trace.contains("inherited_canonical"));
        assertFalse(trace.contains("getPath("));
        assertFalse(trace.contains("put("));
        assertFalse(trace.contains("remove("));
    }
}
