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
    private static final Set<String> MINECRAFT_DISPLAY_RESULTS = Set.of(
            "minecraft:oak_sign", "minecraft:oak_hanging_sign", "minecraft:oak_shelf",
            "minecraft:spruce_sign", "minecraft:spruce_hanging_sign", "minecraft:spruce_shelf",
            "minecraft:birch_sign", "minecraft:birch_hanging_sign", "minecraft:birch_shelf",
            "minecraft:jungle_sign", "minecraft:jungle_hanging_sign", "minecraft:jungle_shelf",
            "minecraft:acacia_sign", "minecraft:acacia_hanging_sign", "minecraft:acacia_shelf",
            "minecraft:dark_oak_sign", "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_shelf",
            "minecraft:mangrove_sign", "minecraft:mangrove_hanging_sign", "minecraft:mangrove_shelf",
            "minecraft:cherry_sign", "minecraft:cherry_hanging_sign", "minecraft:cherry_shelf",
            "minecraft:pale_oak_sign", "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_shelf",
            "minecraft:bamboo_sign", "minecraft:bamboo_hanging_sign", "minecraft:bamboo_shelf",
            "minecraft:crimson_sign", "minecraft:crimson_hanging_sign", "minecraft:crimson_shelf",
            "minecraft:warped_sign", "minecraft:warped_hanging_sign", "minecraft:warped_shelf");

    private static final Set<String> ENDERSCAPE_APPROVED_RESULTS = Set.of(
            "enderscape:veiled_sign", "enderscape:veiled_hanging_sign", "enderscape:veiled_shelf",
            "enderscape:celestial_sign", "enderscape:celestial_hanging_sign", "enderscape:celestial_shelf",
            "enderscape:murublight_sign", "enderscape:murublight_hanging_sign",
            "enderscape:murublight_shelf",
            "enderscape:veiled_fence", "enderscape:veiled_fence_gate",
            "enderscape:celestial_fence", "enderscape:celestial_fence_gate",
            "enderscape:murublight_fence", "enderscape:murublight_fence_gate",
            "enderscape:veiled_button", "enderscape:veiled_pressure_plate",
            "enderscape:celestial_button", "enderscape:celestial_pressure_plate",
            "enderscape:murublight_button", "enderscape:murublight_pressure_plate",
            "enderscape:polished_end_stone_button",
            "enderscape:polished_end_stone_pressure_plate",
            "enderscape:polished_mirestone_button",
            "enderscape:polished_mirestone_pressure_plate",
            "enderscape:polished_veradite_button",
            "enderscape:polished_veradite_pressure_plate",
            "enderscape:polished_kurodite_button",
            "enderscape:polished_kurodite_pressure_plate",
            "enderscape:shadoline_bars", "enderscape:shadoline_chain");

    private static final Set<String> C8_CANONICAL_RECIPE_IDS = Set.of(
            "minecraft:oak_sign", "minecraft:spruce_sign", "minecraft:birch_sign",
            "minecraft:jungle_sign", "minecraft:acacia_sign", "minecraft:dark_oak_sign",
            "minecraft:mangrove_sign", "minecraft:cherry_sign", "minecraft:pale_oak_sign",
            "minecraft:bamboo_sign", "minecraft:crimson_sign", "minecraft:warped_sign",
            "enderscape:veiled_sign", "enderscape:celestial_sign", "enderscape:murublight_sign",
            "enderscape:murublight_sign_from_celestial_sign",
            "enderscape:veiled_fence", "enderscape:celestial_fence", "enderscape:murublight_fence",
            "enderscape:murublight_fence_from_celestial_fence",
            "enderscape:veiled_button", "enderscape:celestial_button", "enderscape:murublight_button",
            "enderscape:murublight_button_from_celestial_button",
            "enderscape:polished_end_stone_button", "enderscape:polished_mirestone_button",
            "enderscape:polished_veradite_button", "enderscape:polished_kurodite_button",
            "enderscape:polished_kurodite_button_from_polished_veradite_button",
            "enderscape:shadoline_bars");

    private static final Set<String> C8_REMOVED_ALTERNATE_RECIPE_IDS = Set.of(
            "minecraft:oak_hanging_sign", "minecraft:oak_shelf",
            "minecraft:spruce_hanging_sign", "minecraft:spruce_shelf",
            "minecraft:birch_hanging_sign", "minecraft:birch_shelf",
            "minecraft:jungle_hanging_sign", "minecraft:jungle_shelf",
            "minecraft:acacia_hanging_sign", "minecraft:acacia_shelf",
            "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_shelf",
            "minecraft:mangrove_hanging_sign", "minecraft:mangrove_shelf",
            "minecraft:cherry_hanging_sign", "minecraft:cherry_shelf",
            "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_shelf",
            "minecraft:bamboo_hanging_sign", "minecraft:bamboo_shelf",
            "minecraft:crimson_hanging_sign", "minecraft:crimson_shelf",
            "minecraft:warped_hanging_sign", "minecraft:warped_shelf",
            "enderscape:veiled_hanging_sign", "enderscape:celestial_hanging_sign",
            "enderscape:murublight_hanging_sign",
            "enderscape:veiled_shelf", "enderscape:celestial_shelf", "enderscape:murublight_shelf",
            "enderscape:veiled_fence_gate", "enderscape:celestial_fence_gate",
            "enderscape:murublight_fence_gate",
            "enderscape:veiled_pressure_plate", "enderscape:celestial_pressure_plate",
            "enderscape:murublight_pressure_plate",
            "enderscape:polished_end_stone_pressure_plate",
            "enderscape:polished_mirestone_pressure_plate",
            "enderscape:polished_veradite_pressure_plate",
            "enderscape:polished_kurodite_pressure_plate",
            "enderscape:shadoline_chain");

    private static final Set<String> C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS = Set.of(
            "enderscape:murublight_hanging_sign_from_celestial_hanging_sign",
            "enderscape:murublight_shelf_from_celestial_shelf",
            "enderscape:murublight_fence_gate_from_celestial_fence_gate",
            "enderscape:murublight_pressure_plate_from_celestial_pressure_plate",
            "enderscape:polished_kurodite_pressure_plate_from_polished_veradite_pressure_plate");

    @Test
    void exactAssembledCorpusReplaysCnmCleanupOrder() throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        recipes.addAll(recipesFromJar("macawsDoorsReferenceJar", "mcwdoors", "two_high_doors"));
        recipes.addAll(recipesFromJar("macawsPathsReferenceJar", "mcwpaths", "macaws_paths"));
        recipes.addAll(recipesFromJar("macawsTrapdoorsReferenceJar", "mcwtrpdoors", "trapdoors"));
        recipes.addAll(recipesFromJar("macawsWindowsReferenceJar", "mcwwindows", "windows_and_shutters"));
        recipes.addAll(recipesFromJar(
                "dramaticDoorsReferenceJar", "dramaticdoors", "dramatic_packaged_static"));
        recipes.addAll(recipesFromJarResults(
                "minecraftReferenceJar", "minecraft", "minecraft_display_fixtures",
                MINECRAFT_DISPLAY_RESULTS));
        recipes.addAll(recipesFromJarResults(
                "enderscapeReferenceJar", "enderscape", "enderscape_approved_families",
                ENDERSCAPE_APPROVED_RESULTS));
        recipes.addAll(dynamicAndModeledRecipes());

        assertEquals(Map.of(
                        "two_high_doors", 278L,
                        "macaws_paths", 316L,
                        "trapdoors", 207L,
                        "windows_and_shutters", 335L,
                        "dramatic_packaged_static", 59L,
                        "minecraft_display_fixtures", 36L,
                        "enderscape_approved_families", 40L,
                        "three_high_doors", 217L,
                        "fence_gates", 24L,
                        "vanilla_building_accessories", 42L),
                countByCorpus(recipes));
        assertEquals(1_554, recipes.size());
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
        expectedRemoved.put("minecraft_display_fixtures", 24L);
        expectedRemoved.put("enderscape_approved_families", 17L);
        expectedRemoved.put("three_high_doors", 217L);
        expectedRemoved.put("fence_gates", 12L);
        expectedRemoved.put("vanilla_building_accessories", 20L);
        assertEquals(expectedRemoved, removedNonParents);
        assertEquals(260 + 217 + 195 + 215 + 143 + 12 + 20 + 24 + 17,
                removedNonParents.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(1_103L, removedNonParents.values().stream().mapToLong(Long::longValue).sum());
        assertTrue(dangerousParentRemovals.isEmpty(), dangerousParentRemovals.toString());
        assertTrue(survivingLiteralRewrites.isEmpty(), survivingLiteralRewrites.toString());
    }

    @Test
    void newFamiliesRemainObtainableAndCleanupTouchesOnlyTheirNonParentOutputs() throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        recipes.addAll(recipesFromJar("macawsPathsReferenceJar", "mcwpaths", "macaws_paths"));
        recipes.addAll(recipesFromJar(
                "macawsWindowsReferenceJar", "mcwwindows", "windows_and_shutters"));
        recipes.addAll(recipesFromJarResults(
                "minecraftReferenceJar", "minecraft", "minecraft_display_fixtures",
                MINECRAFT_DISPLAY_RESULTS));
        recipes.addAll(recipesFromJarResults(
                "enderscapeReferenceJar", "enderscape", "enderscape_approved_families",
                ENDERSCAPE_APPROVED_RESULTS));
        recipes.addAll(dynamicAndModeledRecipes());

        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        Map<String, List<String>> tags = assembledTags();
        Set<String> newMembers = new LinkedHashSet<>();
        List<AuditedShapeFamily> newFamilies = new ArrayList<>();
        newFamilies.addAll(AuditedShapeFamilies.families(AuditedShapeFamily.Category.BAR_CHAIN));
        newFamilies.addAll(AuditedShapeFamilies.families(
                AuditedShapeFamily.Category.BUILDING_ACCESSORY));
        newFamilies.addAll(AuditedShapeFamilies.families(
                AuditedShapeFamily.Category.DISPLAY_FIXTURE));
        AuditedShapeFamilies.families(AuditedShapeFamily.Category.FENCE_GATE).stream()
                .filter(family -> family.key().getPath().startsWith("cnm/fence_gate/enderscape_"))
                .forEach(newFamilies::add);
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

        assertEquals(255, newRemovedRecipeIds.size(), newRemovedRecipeIds.toString());
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
                "mcwwindows:metal_curtain_rod",
                "minecraft:oak_hanging_sign",
                "minecraft:warped_shelf",
                "enderscape:murublight_hanging_sign",
                "enderscape:murublight_fence_gate",
                "enderscape:polished_kurodite_pressure_plate",
                "enderscape:shadoline_chain")),
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
    void exactC8ProviderRecipesPreserveCanonicalAndCustomConversionResultsAndRemoveCraftingAlternates()
            throws Exception {
        List<RecipeFixture> recipes = new ArrayList<>();
        recipes.addAll(recipesFromJarResults(
                "minecraftReferenceJar", "minecraft", "minecraft_display_fixtures",
                MINECRAFT_DISPLAY_RESULTS));
        recipes.addAll(recipesFromJarResults(
                "enderscapeReferenceJar", "enderscape", "enderscape_approved_families",
                ENDERSCAPE_APPROVED_RESULTS));

        assertEquals(76, recipes.size());
        Set<String> expectedResults = new LinkedHashSet<>(MINECRAFT_DISPLAY_RESULTS);
        expectedResults.addAll(ENDERSCAPE_APPROVED_RESULTS);
        assertEquals(expectedResults, recipes.stream()
                .map(RecipeFixture::result)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));

        ShapeFamilies shapes = ShapeFamilies.fromCatalog();
        Map<String, List<String>> tags = assembledTags();
        Set<String> retained = new LinkedHashSet<>();
        Set<String> removed = new LinkedHashSet<>();
        Set<String> dangerous = new LinkedHashSet<>();
        for (RecipeFixture recipe : recipes) {
            CleanupOutcome outcome = cleanup(recipe, shapes, tags);
            switch (outcome.disposition()) {
                case KEEP -> retained.add(recipe.recipeId());
                case REMOVE_NON_PARENT_RESULT -> removed.add(recipe.recipeId());
                case REMOVE_PARENT_RESULT -> dangerous.add(recipe.recipeId());
            }
            assertTrue(!outcome.literalIngredientChanged(), recipe.recipeId());
        }

        Set<String> expectedRetained = new LinkedHashSet<>(C8_CANONICAL_RECIPE_IDS);
        expectedRetained.addAll(C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS);
        assertEquals(expectedRetained, retained);
        assertEquals(C8_REMOVED_ALTERNATE_RECIPE_IDS, removed);
        assertTrue(dangerous.isEmpty(), dangerous.toString());
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
        if (!recipe.cleanupEligible()) {
            return new CleanupOutcome(Disposition.KEEP, false);
        }
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
        return recipesFromJarResults(jarProperty, namespace, corpus, null);
    }

    private static List<RecipeFixture> recipesFromJarResults(String jarProperty, String namespace,
            String corpus, Set<String> allowedResults) throws Exception {
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
                if (resultElement == null) {
                    assertTrue(allowedResults != null, recipeId + " has no result");
                    continue;
                }
                String result = resultElement.isJsonObject()
                        ? resultElement.getAsJsonObject().get("id").getAsString()
                        : resultElement.getAsString();
                if (allowedResults != null && !allowedResults.contains(result)) continue;
                String type = json.get("type").getAsString();
                boolean cleanupEligible = !type.equals("enderscape:void_lachryma");
                recipes.add(new RecipeFixture(
                        corpus, recipeId, result, ingredientSlots(json, recipeId), cleanupEligible));
            }
        }
        if (allowedResults != null) {
            Set<String> actualResults = recipes.stream()
                    .map(RecipeFixture::result)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            assertEquals(allowedResults, actualResults,
                    jarProperty + " did not cover the complete literal result set");
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
            recipes.add(new RecipeFixture(
                    row[0], row[1], row[2], List.of(row[3].split("\\|", -1)), true));
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

    private record RecipeFixture(
            String corpus,
            String recipeId,
            String result,
            List<String> ingredients,
            boolean cleanupEligible
    ) {}

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
