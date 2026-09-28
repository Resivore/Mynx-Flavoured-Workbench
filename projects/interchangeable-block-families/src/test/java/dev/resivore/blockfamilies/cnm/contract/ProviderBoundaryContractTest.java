package dev.resivore.blockfamilies.cnm.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProviderBoundaryContractTest {
    private static final String PATHS_JAR_PROPERTY = "macawsPathsReferenceJar";
    private static final String WINDOWS_JAR_PROPERTY = "macawsWindowsReferenceJar";

    private static final List<String> PATH_WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped");
    private static final List<String> PATH_MATERIALS = List.of(
            "andesite", "diorite", "granite", "sandstone", "red_sandstone", "brick",
            "stone", "mossy_stone", "cobbled_deepslate", "deepslate", "mud_brick",
            "blackstone", "dark_prismarine");
    private static final List<String> PAVING_MATERIALS = List.of(
            "andesite", "diorite", "granite", "sandstone", "red_sandstone", "brick",
            "cobblestone", "mossy_cobblestone", "cobbled_deepslate", "deepslate", "mud_brick",
            "blackstone", "dark_prismarine");
    private static final List<String> THIN_PATH_DESIGNS = List.of(
            "running_bond_path", "strewn_rocky_path", "windmill_weave_path",
            "flagstone_path", "crystal_floor_path");
    private static final List<String> PATTERNED_BLOCK_DESIGNS = List.of(
            "running_bond", "windmill_weave", "flagstone", "crystal_floor");
    private static final List<String> PAVING_DESIGNS = List.of(
            "diamond_paving", "basket_weave_paving", "square_paving",
            "honeycomb_paving", "clover_paving", "dumble_paving");

    private static final List<String> WINDOW_WOODS = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "crimson", "warped");

    @Test
    void catalogContainsExactlyTheAuditedPathsAndSeventyEightPavings() throws Exception {
        Set<String> expectedPaths = expectedPathIds();
        Set<String> expectedPavings = product(PAVING_MATERIALS, PAVING_DESIGNS, "_");
        assertEquals(77, expectedPaths.size());
        assertEquals(78, expectedPavings.size());

        Set<String> catalogPaths = catalogMembers(
                AuditedShapeFamily.Category.BUILDING_ACCESSORY, "mcwpaths");
        Set<String> expectedCatalogPaths = new LinkedHashSet<>(expectedPaths);
        expectedCatalogPaths.addAll(expectedPavings);
        assertEquals(155, expectedCatalogPaths.size());
        assertEquals(namespaced("mcwpaths", expectedCatalogPaths), catalogPaths);

        try (JarFile jar = providerJar(PATHS_JAR_PROPERTY)) {
            Set<String> blockstates = blockstateIds(jar, "mcwpaths");
            Set<String> providerPaths = matching(blockstates, id -> id.endsWith("_path"));
            assertEquals(expectedPaths, providerPaths);
            assertEquals(expectedPavings, matching(blockstates, id -> id.endsWith("_paving")));

            for (String id : expectedPaths) {
                assertJarEntry(jar, blockstateEntry("mcwpaths", id));
                assertEquals("mcwpaths:" + id, recipeResult(jar, "mcwpaths", id), id);
            }
        }
    }

    @Test
    void everyOtherMacawsPathsFormRemainsOutsideTheCatalog() throws Exception {
        Set<String> expectedPavings = product(PAVING_MATERIALS, PAVING_DESIGNS, "_");
        Set<String> expectedPathBlocks = Set.of(
                "dirt_path_block", "gravel_path_block", "podzol_path_block",
                "red_sand_path_block", "sand_path_block");
        Set<String> expectedSlabs = suffixedPatternedBlocks("_slab");
        Set<String> expectedStairs = suffixedPatternedBlocks("_stairs");
        Set<String> expectedFullBlocks = product(PATH_MATERIALS, PATTERNED_BLOCK_DESIGNS, "_");

        try (JarFile jar = providerJar(PATHS_JAR_PROPERTY)) {
            Set<String> all = blockstateIds(jar, "mcwpaths");
            Set<String> paths = matching(all, id -> id.endsWith("_path"));
            Set<String> pavings = matching(all, id -> id.endsWith("_paving"));
            Set<String> pathBlocks = matching(all, id -> id.endsWith("_path_block"));
            Set<String> slabs = matching(all, id -> id.endsWith("_slab"));
            Set<String> stairs = matching(all, id -> id.endsWith("_stairs"));
            Set<String> fullBlocks = new LinkedHashSet<>(all);
            fullBlocks.removeAll(paths);
            fullBlocks.removeAll(pavings);
            fullBlocks.removeAll(pathBlocks);
            fullBlocks.removeAll(slabs);
            fullBlocks.removeAll(stairs);

            assertEquals(316, all.size());
            assertEquals(77, paths.size());
            assertEquals(expectedPavings, pavings);
            assertEquals(78, pavings.size());
            assertEquals(expectedPathBlocks, pathBlocks);
            assertEquals(5, pathBlocks.size());
            assertEquals(expectedSlabs, slabs);
            assertEquals(52, slabs.size());
            assertEquals(expectedStairs, stairs);
            assertEquals(52, stairs.size());
            assertEquals(expectedFullBlocks, fullBlocks);
            assertEquals(52, fullBlocks.size());

            Set<String> excluded = new LinkedHashSet<>();
            excluded.addAll(pathBlocks);
            excluded.addAll(slabs);
            excluded.addAll(stairs);
            excluded.addAll(fullBlocks);
            assertEquals(161, excluded.size());
            assertTrue(disjoint(namespaced("mcwpaths", excluded), allCatalogMembers()),
                    "An explicitly excluded Macaw Paths form entered an IBF family");
        }
    }

    @Test
    void exactPathProviderBytecodePinsTheThinGeometryAndRegistrationTypes() throws Exception {
        try (JarFile jar = providerJar(PATHS_JAR_PROPERTY)) {
            ClassNode pathBlock = classNode(jar, "com/mcwpaths/kikoz/objects/PathBlock.class");
            MethodNode pathClinit = method(pathBlock, "<clinit>");
            Set<Double> doubleConstants = new HashSet<>();
            boolean callsSixCoordinateBox = false;
            for (AbstractInsnNode instruction : pathClinit.instructions) {
                if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Double value) {
                    doubleConstants.add(value);
                }
                if (instruction instanceof MethodInsnNode call
                        && call.getOpcode() == Opcodes.INVOKESTATIC
                        && call.owner.equals("net/minecraft/world/level/block/Block")
                        && call.name.equals("box")
                        && call.desc.equals("(DDDDDD)Lnet/minecraft/world/phys/shapes/VoxelShape;")) {
                    callsSixCoordinateBox = true;
                }
            }
            assertTrue(doubleConstants.contains(0.01D), "PathBlock lost the audited 0.01 lower Y");
            assertTrue(doubleConstants.contains(0.99D), "PathBlock lost the audited 0.99 upper Y");
            assertTrue(callsSixCoordinateBox, "PathBlock no longer constructs the audited thin box");

            ClassNode facingPathBlock = classNode(
                    jar, "com/mcwpaths/kikoz/objects/FacingPathBlock.class");
            assertEquals("com/mcwpaths/kikoz/objects/PathBlock", facingPathBlock.superName);
            assertTrue(facingPathBlock.methods.stream().noneMatch(candidate -> candidate.name.equals("getShape")),
                    "FacingPathBlock unexpectedly overrides the audited PathBlock geometry");

            ClassNode blockInit = classNode(jar, "com/mcwpaths/kikoz/init/BlockInit.class");
            Map<String, String> thinRegistrations = thinRegistrations(method(blockInit, "<clinit>"));
            Set<String> expectedPathAndPavingRegistrations = new LinkedHashSet<>(expectedPathIds());
            expectedPathAndPavingRegistrations.addAll(product(PAVING_MATERIALS, PAVING_DESIGNS, "_"));
            assertEquals(expectedPathAndPavingRegistrations, thinRegistrations.keySet());
            assertEquals(Set.of(
                            "com/mcwpaths/kikoz/objects/PathBlock",
                            "com/mcwpaths/kikoz/objects/FacingPathBlock"),
                    Set.copyOf(thinRegistrations.values()));
        }
    }

    @Test
    void windowsAccessoryBoundaryContainsOnlyParapetsBlindsAndCurtainRods() throws Exception {
        Set<String> expectedParapets = expectedParapets();
        Set<String> expectedBlinds = suffixed(WINDOW_WOODS, "_blinds");
        Set<String> expectedRods = suffixed(WINDOW_WOODS, "_curtain_rod");
        expectedRods = new LinkedHashSet<>(expectedRods);
        expectedRods.add("golden_curtain_rod");
        expectedRods.add("metal_curtain_rod");

        assertEquals(28, expectedParapets.size());
        assertEquals(11, expectedBlinds.size());
        assertEquals(13, expectedRods.size());

        Set<String> expectedProviderAccessories = new LinkedHashSet<>();
        expectedProviderAccessories.addAll(expectedParapets);
        expectedProviderAccessories.addAll(expectedBlinds);
        expectedProviderAccessories.addAll(expectedRods);
        assertEquals(52, expectedProviderAccessories.size());

        try (JarFile jar = providerJar(WINDOWS_JAR_PROPERTY)) {
            Set<String> blockstates = blockstateIds(jar, "mcwwindows");
            Set<String> providerAccessories = matching(blockstates,
                    id -> id.endsWith("_parapet")
                            || id.endsWith("_blinds")
                            || id.endsWith("_curtain_rod"));
            assertEquals(expectedProviderAccessories, providerAccessories);

            for (String id : expectedProviderAccessories) {
                assertJarEntry(jar, blockstateEntry("mcwwindows", id));
                assertEquals("mcwwindows:" + id, recipeResult(jar, "mcwwindows", id), id);
            }

            Set<String> excludedWindows = matching(blockstates, id -> id.contains("window"));
            Set<String> excludedCurtains = matching(blockstates, id -> id.endsWith("_curtain"));
            Set<String> excludedShutters = matching(blockstates, id -> id.contains("shutter"));
            Set<String> excludedMosaics = matching(blockstates, id -> id.contains("mosaic"));
            Set<String> excludedArrowSlits = matching(blockstates, id -> id.contains("arrow_slit"));
            for (Set<String> excludedPartition : List.of(
                    excludedWindows, excludedCurtains, excludedShutters,
                    excludedMosaics, excludedArrowSlits)) {
                assertFalse(excludedPartition.isEmpty(), "The exact provider lost an audited exclusion partition");
                assertTrue(disjoint(providerAccessories, excludedPartition));
            }

            Set<String> expectedCatalogAccessories = new LinkedHashSet<>(expectedProviderAccessories);
            expectedCatalogAccessories.remove("prismarine_parapet");
            assertEquals(namespaced("mcwwindows", expectedCatalogAccessories),
                    catalogMembers(AuditedShapeFamily.Category.BUILDING_ACCESSORY, "mcwwindows"));
        }
    }

    @Test
    void windowsRecipesPinMaterialAnomaliesAndTheirLiteralCatalogDecisions() throws Exception {
        try (JarFile jar = providerJar(WINDOWS_JAR_PROPERTY)) {
            assertEquals(Set.of("minecraft:stick", "minecraft:polished_blackstone"),
                    recipeIngredients(jar, "mcwwindows", "blackstone_parapet"));
            assertEquals(Set.of("minecraft:stick", "minecraft:prismarine_bricks"),
                    recipeIngredients(jar, "mcwwindows", "prismarine_parapet"));
            assertEquals(Set.of("minecraft:gold_nugget", "minecraft:gold_ingot"),
                    recipeIngredients(jar, "mcwwindows", "golden_curtain_rod"));
            assertEquals(Set.of("minecraft:iron_nugget", "minecraft:iron_ingot"),
                    recipeIngredients(jar, "mcwwindows", "metal_curtain_rod"));
        }

        Map<String, String> familyByMember = familyKeyByMember();
        assertEquals("interchangeable_block_families:cnm/building_accessory/polished_blackstone",
                familyByMember.get("mcwwindows:blackstone_parapet"));
        assertFalse(familyByMember.containsKey("mcwwindows:prismarine_parapet"),
                "The prismarine-bricks parapet must remain excluded as a singleton material family");
        assertEquals("interchangeable_block_families:cnm/building_accessory/gold",
                familyByMember.get("mcwwindows:golden_curtain_rod"));
        assertEquals("interchangeable_block_families:cnm/building_accessory/iron",
                familyByMember.get("mcwwindows:metal_curtain_rod"));
    }

    private static Set<String> expectedPathIds() {
        Set<String> expected = new LinkedHashSet<>(suffixed(PATH_WOODS, "_planks_path"));
        expected.addAll(product(PATH_MATERIALS, THIN_PATH_DESIGNS, "_"));
        return Set.copyOf(expected);
    }

    private static Set<String> expectedParapets() {
        Set<String> expected = new LinkedHashSet<>();
        for (String wood : WINDOW_WOODS) {
            String trunk = wood.equals("crimson") || wood.equals("warped") ? "_stem_parapet" : "_log_parapet";
            expected.add(wood + trunk);
            expected.add(wood + "_plank_parapet");
        }
        expected.addAll(Set.of(
                "andesite_parapet", "diorite_parapet", "granite_parapet",
                "blackstone_parapet", "prismarine_parapet", "dark_prismarine_parapet"));
        return Set.copyOf(expected);
    }

    private static Set<String> suffixedPatternedBlocks(String suffix) {
        Set<String> result = new LinkedHashSet<>();
        for (String material : PATH_MATERIALS) {
            for (String design : PATTERNED_BLOCK_DESIGNS) {
                result.add(material + "_" + design + suffix);
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> product(List<String> prefixes, List<String> suffixes, String separator) {
        Set<String> result = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            for (String suffix : suffixes) {
                result.add(prefix + separator + suffix);
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> suffixed(List<String> prefixes, String suffix) {
        Set<String> result = new LinkedHashSet<>();
        for (String prefix : prefixes) result.add(prefix + suffix);
        return Set.copyOf(result);
    }

    private static Set<String> catalogMembers(AuditedShapeFamily.Category category, String namespace) {
        Set<String> result = new LinkedHashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families(category)) {
            for (var member : family.members()) {
                if (member.toString().startsWith(namespace + ":")) result.add(member.toString());
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> allCatalogMembers() {
        Set<String> result = new LinkedHashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            for (var member : family.members()) result.add(member.toString());
        }
        return Set.copyOf(result);
    }

    private static Map<String, String> familyKeyByMember() {
        Map<String, String> result = new LinkedHashMap<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            for (var member : family.members()) {
                assertTrue(result.put(member.toString(), family.key().toString()) == null, member.toString());
            }
        }
        return Map.copyOf(result);
    }

    private static Set<String> blockstateIds(JarFile jar, String namespace) {
        String prefix = "assets/" + namespace + "/blockstates/";
        Set<String> result = new LinkedHashSet<>();
        jar.stream()
                .filter(entry -> !entry.isDirectory()
                        && entry.getName().startsWith(prefix)
                        && entry.getName().endsWith(".json"))
                .map(entry -> entry.getName().substring(prefix.length(), entry.getName().length() - 5))
                .sorted()
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private static Set<String> matching(Set<String> values, Predicate<String> predicate) {
        Set<String> result = new LinkedHashSet<>();
        values.stream().filter(predicate).sorted().forEach(result::add);
        return Set.copyOf(result);
    }

    private static Set<String> namespaced(String namespace, Set<String> paths) {
        Set<String> result = new LinkedHashSet<>();
        for (String path : paths) result.add(namespace + ":" + path);
        return Set.copyOf(result);
    }

    private static boolean disjoint(Set<String> left, Set<String> right) {
        return left.stream().noneMatch(right::contains);
    }

    private static Map<String, String> thinRegistrations(MethodNode clinit) {
        Map<String, String> result = new LinkedHashMap<>();
        for (AbstractInsnNode instruction : clinit.instructions) {
            if (!(instruction instanceof LdcInsnNode ldc) || !(ldc.cst instanceof String id)) continue;
            if (!id.endsWith("_path") && !id.endsWith("_paving")) continue;
            AbstractInsnNode next = nextExecutable(instruction);
            if (!(next instanceof TypeInsnNode type) || next.getOpcode() != Opcodes.NEW) continue;
            if (!type.desc.equals("com/mcwpaths/kikoz/objects/PathBlock")
                    && !type.desc.equals("com/mcwpaths/kikoz/objects/FacingPathBlock")) continue;
            assertTrue(result.put(id, type.desc) == null, "Duplicate thin registration for " + id);
        }
        return Map.copyOf(result);
    }

    private static AbstractInsnNode nextExecutable(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next instanceof LabelNode || next instanceof LineNumberNode || next instanceof FrameNode) {
            next = next.getNext();
        }
        return next;
    }

    private static ClassNode classNode(JarFile jar, String entryName) throws Exception {
        JarEntry entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited class " + entryName);
        try (InputStream input = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name) {
        return owner.methods.stream()
                .filter(candidate -> candidate.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(owner.name + " is missing " + name));
    }

    private static String recipeResult(JarFile jar, String namespace, String recipePath) throws Exception {
        JsonObject recipe = recipe(jar, namespace, recipePath);
        JsonElement result = recipe.get("result");
        assertNotNull(result, recipePath + " has no result");
        return result.isJsonObject()
                ? result.getAsJsonObject().get("id").getAsString()
                : result.getAsString();
    }

    private static Set<String> recipeIngredients(
            JarFile jar, String namespace, String recipePath) throws Exception {
        JsonObject recipe = recipe(jar, namespace, recipePath);
        Set<String> ingredients = new LinkedHashSet<>();
        if (recipe.has("key")) {
            for (Map.Entry<String, JsonElement> entry : recipe.getAsJsonObject("key").entrySet()) {
                ingredients.add(entry.getValue().getAsString());
            }
        }
        if (recipe.has("ingredient")) ingredients.add(recipe.get("ingredient").getAsString());
        if (recipe.has("ingredients")) {
            for (JsonElement ingredient : recipe.getAsJsonArray("ingredients")) {
                ingredients.add(ingredient.getAsString());
            }
        }
        return Set.copyOf(ingredients);
    }

    private static JsonObject recipe(JarFile jar, String namespace, String recipePath) throws Exception {
        String entryName = "data/" + namespace + "/recipe/" + recipePath + ".json";
        JarEntry entry = jar.getJarEntry(entryName);
        assertNotNull(entry, "Missing audited recipe " + entryName);
        try (InputStream input = jar.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String blockstateEntry(String namespace, String path) {
        return "assets/" + namespace + "/blockstates/" + path + ".json";
    }

    private static void assertJarEntry(JarFile jar, String entryName) {
        assertNotNull(jar.getJarEntry(entryName), "Missing audited provider resource " + entryName);
    }

    private static JarFile providerJar(String property) throws Exception {
        String configured = System.getProperty(property);
        assertTrue(configured != null && !configured.isBlank(),
                "Missing Gradle-wired property " + property);
        Path path = Path.of(configured);
        assertTrue(Files.isRegularFile(path), "Missing exact provider artifact " + path);
        return new JarFile(path.toFile());
    }
}
