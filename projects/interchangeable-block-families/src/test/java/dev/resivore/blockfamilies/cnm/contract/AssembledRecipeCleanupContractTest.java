package dev.resivore.blockfamilies.cnm.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssembledRecipeCleanupContractTest {
    @Test
    void exactAssembledCorpusReplaysCnmCleanupOrder() throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        recipes.addAll(recipesFromJar("macawsDoorsReferenceJar", "mcwdoors", "two_high_doors"));
        recipes.addAll(recipesFromJar("macawsPathsReferenceJar", "mcwpaths", "macaws_paths"));
        recipes.addAll(recipesFromJar("macawsTrapdoorsReferenceJar", "mcwtrpdoors", "trapdoors"));
        recipes.addAll(recipesFromJar("macawsWindowsReferenceJar", "mcwwindows", "windows_and_shutters"));
        recipes.addAll(recipesFromJar(
                "dramaticDoorsReferenceJar", "dramaticdoors", "dramatic_packaged_static"));
        recipes.addAll(dynamicAndModeledRecipes());

        assertEquals(Map.of(
                        "two_high_doors", 278L,
                        "macaws_paths", 316L,
                        "trapdoors", 207L,
                        "windows_and_shutters", 335L,
                        "dramatic_packaged_static", 59L,
                        "three_high_doors", 217L,
                        "fence_gates", 24L,
                        "vanilla_building_accessories", 42L),
                countByCorpus(recipes));
        assertEquals(1_478, recipes.size());
        assertEquals(recipes.size(), recipes.stream().map(RecipeFixture::recipeId).distinct().count(),
                "The assembled fixture must have one exact identity per recipe");

        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        Map<String, List<String>> assembledTags = assembledTags();
        Map<String, Long> removedNonParents = new LinkedHashMap<>();
        List<String> dangerousParentRemovals = new ArrayList<>();
        List<String> survivingLiteralRewrites = new ArrayList<>();

        for (RecipeFixture recipe : recipes) {
            CleanupOutcome outcome = cleanup(recipe, shapes, assembledTags);
            if (outcome.disposition() == Disposition.REMOVE_NON_PARENT_RESULT) {
                removedNonParents.merge(recipe.corpus(), 1L, Long::sum);
            } else if (outcome.disposition() == Disposition.REMOVE_PARENT_RESULT) {
                dangerousParentRemovals.add(recipe.recipeId());
            } else if (outcome.literalIngredientChanged()) {
                survivingLiteralRewrites.add(recipe.recipeId());
            }
        }

        Map<String, Long> expectedRemoved = new LinkedHashMap<>();
        expectedRemoved.put("two_high_doors", 260L);
        expectedRemoved.put("macaws_paths", 143L);
        expectedRemoved.put("trapdoors", 195L);
        expectedRemoved.put("windows_and_shutters", 215L);
        expectedRemoved.put("three_high_doors", 217L);
        expectedRemoved.put("fence_gates", 12L);
        expectedRemoved.put("vanilla_building_accessories", 20L);
        assertEquals(expectedRemoved, removedNonParents);
        assertEquals(260 + 217 + 195 + 215 + 143 + 12 + 20,
                removedNonParents.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(1_062L, removedNonParents.values().stream().mapToLong(Long::longValue).sum());
        assertTrue(dangerousParentRemovals.isEmpty(), dangerousParentRemovals.toString());
        assertTrue(survivingLiteralRewrites.isEmpty(), survivingLiteralRewrites.toString());
    }

    @Test
    void newFamiliesRemainObtainableAndCleanupTouchesOnlyTheirNonParentOutputs() throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        recipes.addAll(recipesFromJar("macawsPathsReferenceJar", "mcwpaths", "macaws_paths"));
        recipes.addAll(recipesFromJar(
                "macawsWindowsReferenceJar", "mcwwindows", "windows_and_shutters"));
        recipes.addAll(dynamicAndModeledRecipes());

        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        Map<String, List<String>> tags = assembledTags();
        Set<String> newMembers = new LinkedHashSet<>();
        List<AuditedShapeFamily> newFamilies = new ArrayList<>();
        newFamilies.addAll(AuditedShapeFamilies.families(AuditedShapeFamily.Category.BAR_CHAIN));
        newFamilies.addAll(AuditedShapeFamilies.families(
                AuditedShapeFamily.Category.BUILDING_ACCESSORY));
        for (AuditedShapeFamily family : newFamilies) {
            family.members().forEach(member -> newMembers.add(member.toString()));
        }

        Set<String> retainedResults = new LinkedHashSet<>();
        List<String> newRemovedRecipeIds = new ArrayList<>();
        for (RecipeFixture recipe : recipes) {
            CleanupOutcome outcome = cleanup(recipe, shapes, tags);
            if (outcome.disposition() == Disposition.KEEP) {
                retainedResults.add(recipe.result());
            } else if (outcome.disposition() == Disposition.REMOVE_NON_PARENT_RESULT
                    && newMembers.contains(recipe.result())) {
                newRemovedRecipeIds.add(recipe.recipeId());
                assertTrue(shapes.nonParents().contains(recipe.result()),
                        "Cleanup removed a new-family parent result: " + recipe.recipeId());
            }
        }

        assertEquals(214, newRemovedRecipeIds.size(), newRemovedRecipeIds.toString());
        assertTrue(newRemovedRecipeIds.containsAll(List.of(
                "minecraft:iron_chain",
                "minecraft:copper_chain",
                "minecraft:waxed_copper_chain_from_honeycomb",
                "minecraft:waxed_exposed_copper_chain_from_honeycomb",
                "minecraft:waxed_weathered_copper_chain_from_honeycomb",
                "minecraft:waxed_oxidized_copper_chain_from_honeycomb",
                "mcwpaths:oak_planks_path",
                "mcwpaths:stone_running_bond_path",
                "mcwwindows:oak_log_parapet",
                "mcwwindows:metal_curtain_rod")),
                "Expected new-family non-parent recipes were not all removed");

        Set<String> environmentalCopperParents = Set.of(
                "minecraft:exposed_copper_bars",
                "minecraft:weathered_copper_bars",
                "minecraft:oxidized_copper_bars");
        for (AuditedShapeFamily family : newFamilies) {
            String parent = family.canonicalParent().toString();
            assertTrue(retainedResults.contains(parent) || environmentalCopperParents.contains(parent),
                    "New family has no retained recipe or audited weathering acquisition: " + family.key());
        }
    }

    @Test
    void exactBroadDoorAndTrapdoorTagsCollapseToDistinctCanonicalParents() throws Exception {
        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        Map<String, List<String>> tags = assembledTags();

        assertCanonicalTag(tags, shapes, "minecraft:wooden_doors",
                expectedParents(AuditedShapeFamily.Category.TWO_HIGH_DOOR, "minecraft:iron_door"));
        assertCanonicalTag(tags, shapes, "minecraft:doors",
                expectedParents(AuditedShapeFamily.Category.TWO_HIGH_DOOR, null));
        assertCanonicalTag(tags, shapes, "minecraft:wooden_trapdoors",
                expectedParents(AuditedShapeFamily.Category.TRAPDOOR, "minecraft:iron_trapdoor"));
        assertCanonicalTag(tags, shapes, "minecraft:trapdoors",
                expectedParents(AuditedShapeFamily.Category.TRAPDOOR, null));
    }

    @Test
    void representativeExpandedTagCasesExerciseStableDeduplication() throws Exception {
        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        for (String[] row : AuditFixtures.tsv("tag-canonicalization.tsv", 4)) {
            List<String> expanded = List.of(row[2].split("\\|", -1));
            List<String> expected = List.of(row[3].split("\\|", -1));
            assertEquals(expected, canonicalize(expanded, shapes.parentByMember()),
                    row[0] + " / " + row[1]);
            assertTrue(expanded.size() > expected.size(), row[0] + " must prove deduplication");
        }
    }

    private static CleanupOutcome cleanup(RecipeFixture recipe, ShapeFamilies shapes,
            Map<String, List<String>> assembledTags) {
        if (shapes.nonParents().contains(recipe.result())) {
            return new CleanupOutcome(Disposition.REMOVE_NON_PARENT_RESULT, false);
        }

        if (shapes.parents().contains(recipe.result())) {
            for (String ingredient : recipe.ingredients()) {
                for (String option : expandIngredient(ingredient, assembledTags)) {
                    String parent = shapes.parentByMember().get(option);
                    if (parent != null && parent.equals(recipe.result())) {
                        return new CleanupOutcome(Disposition.REMOVE_PARENT_RESULT, false);
                    }
                }
            }
        }

        boolean literalChanged = false;
        for (String ingredient : recipe.ingredients()) {
            if (!ingredient.startsWith("#")) {
                literalChanged |= !ingredient.equals(
                        shapes.parentByMember().getOrDefault(ingredient, ingredient));
            } else {
                canonicalize(expandIngredient(ingredient, assembledTags), shapes.parentByMember());
            }
        }
        return new CleanupOutcome(Disposition.KEEP, literalChanged);
    }

    private static void assertCanonicalTag(Map<String, List<String>> tags, ShapeFamilies shapes,
            String tag, Set<String> expectedParents) {
        List<String> expanded = resolveTag(tag, tags, new HashSet<>());
        List<String> canonical = canonicalize(expanded, shapes.parentByMember());
        assertEquals(expectedParents, new LinkedHashSet<>(canonical), tag);
        assertEquals(expectedParents.size(), canonical.size(), tag + " retained duplicates");
        assertTrue(expanded.size() > canonical.size(), tag + " did not collapse provider shapes");
        assertTrue(canonical.stream().noneMatch(shapes.nonParents()::contains),
                tag + " retained a non-parent shape");
    }

    private static Set<String> expectedParents(AuditedShapeFamily.Category category, String excluded) {
        LinkedHashSet<String> parents = new LinkedHashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families(category)) {
            String parent = family.canonicalParent().toString();
            if (!parent.equals(excluded)) parents.add(parent);
        }
        return parents;
    }

    private static Map<String, List<String>> assembledTags() throws Exception {
        Map<String, List<String>> tags = new LinkedHashMap<>();
        tags.put("minecraft:wooden_doors", new ArrayList<>(
                expectedParents(AuditedShapeFamily.Category.TWO_HIGH_DOOR, "minecraft:iron_door")));
        tags.put("minecraft:doors", new ArrayList<>(List.of(
                "#minecraft:wooden_doors", "minecraft:iron_door")));
        tags.put("minecraft:wooden_trapdoors", new ArrayList<>(
                expectedParents(AuditedShapeFamily.Category.TRAPDOOR, "minecraft:iron_trapdoor")));
        tags.put("minecraft:trapdoors", new ArrayList<>(List.of(
                "#minecraft:wooden_trapdoors", "minecraft:iron_trapdoor")));
        tags.put("minecraft:wooden_fences", new ArrayList<>(
                expectedParents(AuditedShapeFamily.Category.FENCE_GATE, null)));

        mergeTagContribution(tags, "macawsDoorsReferenceJar", "minecraft:wooden_doors");
        mergeTagContribution(tags, "macawsDoorsReferenceJar", "minecraft:doors");
        mergeTagContribution(tags, "macawsTrapdoorsReferenceJar", "minecraft:wooden_trapdoors");
        mergeTagContribution(tags, "macawsTrapdoorsReferenceJar", "minecraft:trapdoors");
        return tags;
    }

    private static void mergeTagContribution(Map<String, List<String>> tags, String jarProperty, String tag)
            throws Exception {
        String[] split = tag.split(":", 2);
        String entryName = "data/" + split[0] + "/tags/item/" + split[1] + ".json";
        try (JarFile jar = new JarFile(requiredPath(jarProperty).toFile())) {
            JarEntry entry = jar.getJarEntry(entryName);
            assertTrue(entry != null, jarProperty + " is missing " + entryName);
            try (InputStream input = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement value : json.getAsJsonArray("values")) {
                    assertTrue(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(), entryName);
                    tags.computeIfAbsent(tag, ignored -> new ArrayList<>()).add(value.getAsString());
                }
            }
        }
    }

    private static List<String> resolveTag(String tag, Map<String, List<String>> tags, Set<String> visiting) {
        assertTrue(visiting.add(tag), "Cyclic fixture tag " + tag);
        List<String> values = tags.get(tag);
        assertTrue(values != null, "Missing assembled tag " + tag);
        List<String> expanded = new ArrayList<>();
        for (String value : values) {
            if (value.startsWith("#")) {
                expanded.addAll(resolveTag(value.substring(1), tags, visiting));
            } else {
                expanded.add(value);
            }
        }
        visiting.remove(tag);
        return expanded;
    }

    private static List<String> expandIngredient(String ingredient, Map<String, List<String>> tags) {
        if (!ingredient.startsWith("#")) return List.of(ingredient);
        return resolveTag(ingredient.substring(1), tags, new HashSet<>());
    }

    private static List<String> canonicalize(List<String> expanded, Map<String, String> parentByMember) {
        LinkedHashSet<String> canonical = new LinkedHashSet<>();
        for (String item : expanded) canonical.add(parentByMember.getOrDefault(item, item));
        return List.copyOf(canonical);
    }

    private static List<RecipeFixture> recipesFromJar(String jarProperty, String namespace, String corpus)
            throws Exception {
        String prefix = "data/" + namespace + "/recipe/";
        List<RecipeFixture> recipes = new ArrayList<>();
        try (JarFile jar = new JarFile(requiredPath(jarProperty).toFile())) {
            List<JarEntry> entries = jar.stream()
                    .filter(entry -> !entry.isDirectory()
                            && entry.getName().startsWith(prefix)
                            && entry.getName().endsWith(".json"))
                    .sorted(java.util.Comparator.comparing(JarEntry::getName))
                    .toList();
            for (JarEntry entry : entries) {
                JsonObject json;
                try (InputStream input = jar.getInputStream(entry);
                     InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
                String path = entry.getName().substring(prefix.length(), entry.getName().length() - 5);
                String recipeId = namespace + ":" + path;
                JsonElement resultElement = json.get("result");
                String result = resultElement.isJsonObject()
                        ? resultElement.getAsJsonObject().get("id").getAsString()
                        : resultElement.getAsString();
                recipes.add(new RecipeFixture(corpus, recipeId, result, ingredientSlots(json, recipeId)));
            }
        }
        return List.copyOf(recipes);
    }

    private static List<String> ingredientSlots(JsonObject json, String recipeId) {
        List<String> slots = new ArrayList<>();
        if (json.has("pattern")) {
            Map<Character, String> key = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("key").entrySet()) {
                assertEquals(1, entry.getKey().length(), recipeId);
                key.put(entry.getKey().charAt(0), literalIngredient(entry.getValue(), recipeId));
            }
            for (JsonElement rowElement : json.getAsJsonArray("pattern")) {
                for (char symbol : rowElement.getAsString().toCharArray()) {
                    if (symbol == ' ') continue;
                    String ingredient = key.get(symbol);
                    assertTrue(ingredient != null, recipeId + " has no key for " + symbol);
                    slots.add(ingredient);
                }
            }
        } else if (json.has("ingredients")) {
            for (JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                slots.add(literalIngredient(ingredient, recipeId));
            }
        } else if (json.has("ingredient")) {
            slots.add(literalIngredient(json.get("ingredient"), recipeId));
        }
        return List.copyOf(slots);
    }

    private static String literalIngredient(JsonElement element, String recipeId) {
        assertTrue(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(),
                recipeId + " contains a non-literal ingredient fixture");
        return element.getAsString();
    }

    private static List<RecipeFixture> dynamicAndModeledRecipes() throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        for (String[] row : AuditFixtures.tsv("dynamic-and-modeled-recipes.tsv", 4)) {
            recipes.add(new RecipeFixture(row[0], row[1], row[2], List.of(row[3].split("\\|", -1))));
        }
        return List.copyOf(recipes);
    }

    private static Map<String, Long> countByCorpus(List<RecipeFixture> recipes) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RecipeFixture recipe : recipes) counts.merge(recipe.corpus(), 1L, Long::sum);
        return counts;
    }

    private static Path requiredPath(String property) {
        String configured = System.getProperty(property);
        assertTrue(configured != null && !configured.isBlank(), "Missing Gradle-wired property " + property);
        Path path = Path.of(configured);
        assertTrue(Files.isRegularFile(path), "Missing exact provider artifact " + path);
        return path;
    }

    private enum Disposition {
        KEEP,
        REMOVE_NON_PARENT_RESULT,
        REMOVE_PARENT_RESULT
    }

    private record CleanupOutcome(Disposition disposition, boolean literalIngredientChanged) {}

    private record RecipeFixture(String corpus, String recipeId, String result, List<String> ingredients) {}

    private record ShapeFamilies(Map<String, String> parentByMember, Set<String> parents,
                                 Set<String> nonParents) {
        private static ShapeFamilies fromCatalog() {
            Map<String, String> parentByMember = new LinkedHashMap<>();
            Set<String> parents = new LinkedHashSet<>();
            Set<String> nonParents = new LinkedHashSet<>();
            for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
                String parent = family.canonicalParent().toString();
                parents.add(parent);
                for (int index = 0; index < family.members().size(); index++) {
                    String member = family.members().get(index).toString();
                    assertTrue(parentByMember.put(member, parent) == null, member);
                    if (index > 0) nonParents.add(member);
                }
            }
            return new ShapeFamilies(Map.copyOf(parentByMember), Set.copyOf(parents), Set.copyOf(nonParents));
        }
    }
}
