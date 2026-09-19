package dev.resivore.enderscapepruning;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static C1 release contract for exact audited resources and narrow runtime hooks. */
final class EnderscapePruningContractTest {
    private static final String PACK_PREFIX = "resourcepacks/enderscape_pruning/";
    private static final List<String> LOOT_TABLES = List.of(
            "end_city/chest", "end_city/elytra_vault", "end_city/vault", "end_city/spawner/basic",
            "end_haven/chest", "mirestone_ruins/chest", "stronghold/chest/altar",
            "stronghold/chest/armory", "stronghold/chest/bedroom", "stronghold/chest/garden",
            "stronghold/chest/library", "stronghold/chest/mansion", "stronghold/chest/secret",
            "stronghold/spawner/basic", "supplements/end_city_treasure", "supplements/stronghold_library");
    private static final Set<String> REMOVED_LOOT_ITEMS = Set.of("enderscape:mirror", "enderscape:dagger");
    private static final Map<String, String> MATCHA_SOURCE_HASHES = Map.of(
            "food/recipe/bread.json", "af1b66671b199b4fe6d7f828e3354928c570c6d8af71a3c93e01ca5342ccc3cb",
            "food/recipe/golden_carrot.json", "e2a10c7adecfa1f5c543de7faff54398f94bcf66e3a8d5fd955589a6010e8ba9",
            "minecraft/loot_table/food/carrot.json", "3a835cbbbb03bd1f44c6ab69b321746527f9d22308a95f8075f7299cd05f80d4",
            "minecraft/loot_table/food/golden_apple.json", "e0bc60586726b342e6e588dccebe4b1970c39618cc6dcfc1e12d6fbed1ae0563",
            "minecraft/loot_table/food/enchanted_golden_apple.json", "98cd922eb2666c0235072eab4fc9cf4a1ad2e8834da44dff17da86bdac1dfa44",
            "minecraft/loot_table/blocks/chorus_plant.json", "9e3bf88454f5badccadda87d962913df073776b35b64d4731856e8b87b849a4e");

    @Test
    void exactInputsAndMatchaIdentityAuthoritiesRemainPinned() throws Exception {
        assertEquals(PruningContract.ENDERSCAPE_SHA256, sha256(enderscapeJar()));
        for (Map.Entry<String, String> entry : MATCHA_SOURCE_HASHES.entrySet()) {
            assertEquals(entry.getValue(), sha256(matchaDataRoot().resolve(entry.getKey())), entry.getKey());
        }
        String build = Files.readString(projectRoot().resolve("build.gradle"));
        MATCHA_SOURCE_HASHES.forEach((path, hash) -> assertTrue(build.contains(hash), path));
        assertTrue(build.contains("No Matcha component identity"));
    }

    @Test
    void generatedOverlayRemovesCandidateLootAtEqualWeightWithoutChangingPoolMath() throws Exception {
        try (ZipFile source = new ZipFile(enderscapeJar().toFile()); ZipFile jar = new ZipFile(packagedJar().toFile())) {
            assertEquals(Set.of(
                    "end_city/elytra_vault", "end_city/vault", "stronghold/chest/armory",
                    "stronghold/chest/garden", "stronghold/chest/library", "stronghold/chest/mansion",
                    "stronghold/chest/secret", "supplements/end_city_treasure", "supplements/stronghold_library"),
                    lootTablesContaining(source, "enderscape:mirror"), "all nine audited Mirror paths");
            assertEquals(Set.of("mirestone_ruins/chest"), lootTablesContaining(source, "enderscape:dagger"),
                    "the audited Dagger path");
            for (String table : LOOT_TABLES) {
                JsonObject upstream = json(source, "data/enderscape/loot_table/" + table + ".json");
                JsonObject generated = json(jar, PACK_PREFIX + "data/enderscape/loot_table/" + table + ".json");
                assertEquals(poolShapes(upstream), poolShapes(generated), table + " pool rolls/bonus rolls/entry counts");
                assertFalse(generated.toString().contains("enderscape:mirror"), table);
                assertFalse(generated.toString().contains("enderscape:dagger"), table);
                for (JsonObject removed : itemEntries(upstream, REMOVED_LOOT_ITEMS)) {
                    int weight = removed.has("weight") ? removed.get("weight").getAsInt() : 1;
                    assertTrue(emptyEntries(generated).stream().anyMatch(empty ->
                                    (empty.has("weight") ? empty.get("weight").getAsInt() : 1) == weight),
                            () -> table + " must retain an equally weighted empty result for " + removed.get("name"));
                }
            }
        }
    }

    @Test
    void everyAuditedMatchaFoodOccurrenceGetsIdentityOnlyComponents() throws Exception {
        Map<String, JsonObject> canonical = canonicalComponents();
        try (ZipFile jar = new ZipFile(packagedJar().toFile())) {
            for (String table : LOOT_TABLES) {
                JsonObject generated = json(jar, PACK_PREFIX + "data/enderscape/loot_table/" + table + ".json");
                for (Map.Entry<String, JsonObject> expected : canonical.entrySet()) {
                    for (JsonObject entry : itemEntries(generated, Set.of(expected.getKey()))) {
                        JsonObject components = lastSetComponents(entry);
                        assertNotNull(components, () -> table + " missing identity patch for " + expected.getKey());
                        assertEquals(expected.getValue(), components, table + " " + expected.getKey());
                        assertFalse(entry.toString().contains("minecraft:set_count\",\"count\":{\"min\":1"),
                                "Matcha helper count must not replace Enderscape count");
                    }
                }
                // Current Matcha cod presentation is fishing-source-only.  The
                // structure-loot Cod entry must remain component-free.
                itemEntries(generated, Set.of("minecraft:cod")).forEach(entry ->
                        assertEquals(null, lastSetComponents(entry), table + " structure cod"));
                itemEntries(generated, Set.of("minecraft:potato", "minecraft:poisonous_potato", "minecraft:honey_bottle", "minecraft:suspicious_stew"))
                        .forEach(entry -> assertEquals(null, lastSetComponents(entry), table + " unaltered carrier"));
            }
        }
    }

    @Test
    void recipeAdvancementEnchantDiscoveryAndNativeFoodHooksAreNarrow() throws IOException {
        assertEquals(12, PruningContract.BLOCKED_RECIPE_IDS.size());
        assertTrue(PruningContract.BLOCKED_RECIPE_IDS.contains("enderscape:mirror_dying"));
        assertTrue(PruningContract.BLOCKED_RECIPE_IDS.contains("enderscape:shadoline_nugget_from_smelting"));
        assertTrue(PruningContract.BLOCKED_RECIPE_IDS.contains("enderscape:shadoline_nugget_from_blasting"));
        assertFalse(PruningContract.BLOCKED_RECIPE_IDS.stream().anyMatch(id -> id.contains("magnia")));
        assertFalse(PruningContract.BLOCKED_ADVANCEMENT_IDS.contains("enderscape:explore_end"));
        assertFalse(PruningContract.SUPPRESSED_ENCHANTMENT_IDS.contains("enderscape:resonance"));
        assertFalse(PruningContract.SUPPRESSED_ENCHANTMENT_IDS.contains("enderscape:rebound"));

        String tags = source("mixin/TagLoaderMixin.java");
        assertTrue(tags.contains("enderscape:bundling"));
        assertTrue(tags.contains("enderscape:stun_burst"));
        assertTrue(tags.contains("enderscape:transdimensional"));
        assertTrue(tags.contains("enderscape:nebulite_tools"));
        assertFalse(tags.contains("enderscape:magnia_attractor"),
                "the existing tag retains the Magnia Attractor by removing only mirror/dagger");
        String enchanting = source("mixin/EnchantmentHelperMixin.java");
        assertTrue(enchanting.contains("isMagniaAttractor"));
        assertTrue(enchanting.contains("isResonance"));
        String food = source("mixin/FoodPropertiesMixin.java");
        assertTrue(food.contains("skipNativeHunger"));
        assertTrue(food.contains("grantOneHeart"));
        String cake = source("mixin/ChorusCakeRollMixin.java");
        assertTrue(cake.contains("skipCakeHunger"));
        assertTrue(cake.contains("grantTwoHearts"));
        assertTrue(cake.contains("canEat"));
        assertTrue(cake.contains("remap = false"));
        assertTrue(source("client/EnderscapePruningClient.java").contains("getSearchTabStacks"));
        assertTrue(source("client/EnderscapePruningJeiPlugin.java").contains("getAllIngredients"));
    }

    @Test
    void exactUpstreamRecipeAndAdvancementGraphAreCoveredWithoutDeletingRetainedParents() throws Exception {
        try (ZipFile source = new ZipFile(enderscapeJar().toFile())) {
            for (String recipe : PruningContract.BLOCKED_RECIPE_IDS) {
                assertNotNull(source.getEntry("data/enderscape/recipe/" + pathAfterNamespace(recipe) + ".json"), recipe);
            }
            for (String advancement : PruningContract.BLOCKED_ADVANCEMENT_IDS) {
                assertNotNull(source.getEntry("data/enderscape/advancement/" + pathAfterNamespace(advancement) + ".json"), advancement);
            }
            for (ZipEntry entry : Collections.list(source.entries())) {
                String name = entry.getName();
                if (!name.startsWith("data/enderscape/advancement/") || !name.endsWith(".json")) continue;
                JsonObject advancement = json(source, name);
                if (advancement.has("parent") && PruningContract.BLOCKED_ADVANCEMENT_IDS.contains(advancement.get("parent").getAsString())) {
                    String id = "enderscape:" + name.substring("data/enderscape/advancement/".length(), name.length() - ".json".length());
                    assertTrue(PruningContract.BLOCKED_ADVANCEMENT_IDS.contains(id),
                            () -> "retained advancement would have a removed parent: " + id);
                }
            }
        }
    }

    @Test
    void retainedExplorationAndUpstreamBoundariesStayIntact() throws Exception {
        try (ZipFile jar = new ZipFile(packagedJar().toFile())) {
            JsonObject explore = json(jar, PACK_PREFIX + "data/enderscape/advancement/explore_end.json");
            assertEquals("enderscape:magnia_attractor", explore.getAsJsonObject("display")
                    .getAsJsonObject("icon").get("id").getAsString());
            assertTrue(explore.getAsJsonObject("criteria").has("enderscape:magnia_fields"));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("net/penumbra/enderscape/")));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().contains("enderscape-fabric-3.0.2")));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().contains("potion/") || entry.getName().contains("worldgen/")));
            Set<String> expectedOverlay = new LinkedHashSet<>();
            expectedOverlay.add(PACK_PREFIX + "pack.mcmeta");
            LOOT_TABLES.forEach(table -> expectedOverlay.add(PACK_PREFIX + "data/enderscape/loot_table/" + table + ".json"));
            expectedOverlay.add(PACK_PREFIX + "data/enderscape/advancement/explore_end.json");
            Set<String> actualOverlay = new LinkedHashSet<>();
            jar.stream().map(ZipEntry::getName)
                    .filter(name -> name.startsWith(PACK_PREFIX) && !name.endsWith("/"))
                    .forEach(actualOverlay::add);
            assertEquals(expectedOverlay, actualOverlay, "C1 overlays only audited loot and the retained display icon");
        }
    }

    private static Set<String> lootTablesContaining(ZipFile source, String item) throws IOException {
        Set<String> tables = new LinkedHashSet<>();
        for (ZipEntry entry : Collections.list(source.entries())) {
            String name = entry.getName();
            if (!name.startsWith("data/enderscape/loot_table/") || !name.endsWith(".json")) continue;
            if (!itemEntries(json(source, name), Set.of(item)).isEmpty()) {
                tables.add(name.substring("data/enderscape/loot_table/".length(), name.length() - ".json".length()));
            }
        }
        return tables;
    }

    private static String pathAfterNamespace(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    private static List<String> poolShapes(JsonObject table) {
        List<String> shapes = new ArrayList<>();
        for (JsonElement element : table.getAsJsonArray("pools")) {
            JsonObject pool = element.getAsJsonObject();
            shapes.add((pool.has("rolls") ? pool.get("rolls").toString() : "1") + "|"
                    + (pool.has("bonus_rolls") ? pool.get("bonus_rolls").toString() : "0") + "|"
                    + pool.getAsJsonArray("entries").size());
        }
        return shapes;
    }

    private static List<JsonObject> itemEntries(JsonElement node, Set<String> items) {
        List<JsonObject> matches = new ArrayList<>();
        collectItems(node, items, matches);
        return matches;
    }

    private static void collectItems(JsonElement node, Set<String> items, List<JsonObject> matches) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (object.has("type") && "minecraft:item".equals(object.get("type").getAsString())
                    && object.has("name") && items.contains(object.get("name").getAsString())) {
                matches.add(object);
            }
            object.entrySet().forEach(entry -> collectItems(entry.getValue(), items, matches));
        } else if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(child -> collectItems(child, items, matches));
        }
    }

    private static List<JsonObject> emptyEntries(JsonElement node) {
        List<JsonObject> matches = new ArrayList<>();
        collectEmpty(node, matches);
        return matches;
    }

    private static void collectEmpty(JsonElement node, List<JsonObject> matches) {
        if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (object.has("type") && "minecraft:empty".equals(object.get("type").getAsString())) matches.add(object);
            object.entrySet().forEach(entry -> collectEmpty(entry.getValue(), matches));
        } else if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(child -> collectEmpty(child, matches));
        }
    }

    private static JsonObject lastSetComponents(JsonObject entry) {
        if (!entry.has("functions")) return null;
        JsonObject components = null;
        for (JsonElement function : entry.getAsJsonArray("functions")) {
            JsonObject object = function.getAsJsonObject();
            if ("minecraft:set_components".equals(object.get("function").getAsString())) {
                components = object.getAsJsonObject("components");
            }
        }
        return components;
    }

    private static Map<String, JsonObject> canonicalComponents() throws IOException {
        Map<String, JsonObject> components = new LinkedHashMap<>();
        components.put("minecraft:bread", recipeComponents("food/recipe/bread.json"));
        components.put("minecraft:golden_carrot", recipeComponents("food/recipe/golden_carrot.json"));
        components.put("minecraft:carrot", sourceComponents("minecraft/loot_table/food/carrot.json", "minecraft:carrot"));
        components.put("minecraft:golden_apple", sourceComponents("minecraft/loot_table/food/golden_apple.json", "minecraft:golden_apple"));
        components.put("minecraft:enchanted_golden_apple", sourceComponents("minecraft/loot_table/food/enchanted_golden_apple.json", "minecraft:enchanted_golden_apple"));
        components.put("minecraft:chorus_fruit", sourceComponents("minecraft/loot_table/blocks/chorus_plant.json", "minecraft:chorus_fruit"));
        return components;
    }

    private static JsonObject recipeComponents(String relative) throws IOException {
        return parse(matchaDataRoot().resolve(relative)).getAsJsonObject("result").getAsJsonObject("components");
    }

    private static JsonObject sourceComponents(String relative, String item) throws IOException {
        for (JsonObject entry : itemEntries(parse(matchaDataRoot().resolve(relative)), Set.of(item))) {
            JsonObject components = lastSetComponents(entry);
            if (components != null) return components;
        }
        throw new AssertionError("Missing Matcha components for " + item + " in " + relative);
    }

    private static JsonObject json(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, () -> "Missing " + name);
        try (InputStream input = zip.getInputStream(entry)) {
            return JsonParser.parseReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject parse(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String source(String relative) throws IOException {
        return Files.readString(projectRoot().resolve("src/main/java/dev/resivore/enderscapepruning").resolve(relative));
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }

    private static Path packagedJar() {
        return Path.of(System.getProperty("packagedJar")).toAbsolutePath().normalize();
    }

    private static Path enderscapeJar() {
        return Path.of(System.getProperty("workbenchRoot"), "originals/mods/enderscape-fabric-3.0.2+mc26.2.jar");
    }

    private static Path matchaDataRoot() {
        return Path.of(System.getProperty("workbenchRoot"), "projects/matcha-flavoured-data/src/main/resources/resourcepacks/matcha_flavoured_1_12/data");
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) > 0;) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
