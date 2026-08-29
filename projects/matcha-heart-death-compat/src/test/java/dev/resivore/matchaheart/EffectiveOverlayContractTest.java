package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class EffectiveOverlayContractTest {
    private static final String ECHO_SHARD = "minecraft:echo_shard";
    private static final Path WORKBENCH = Path.of(System.getProperty("workbenchRoot"));
    private static final Path MATCHA = WORKBENCH.resolve(HeartDataContract.MATCHA_ARCHIVE);

    @Test
    void pinnedMatchaArchiveStillMatchesTheAuditedContract() throws Exception {
        assertEquals(HeartDataContract.MATCHA_SHA256, sha256(MATCHA));
    }

    @Test
    void effectiveMatchaAndUnifiedHeartDataHaveOnlyTheIntendedEchoReferences() throws Exception {
        Map<String, String> effective = effectiveData();
        Set<String> references = new HashSet<>();
        Set<String> lootSources = new HashSet<>();

        effective.forEach((path, text) -> {
            if (!text.contains(ECHO_SHARD)) {
                return;
            }
            if (!path.endsWith(".json")) {
                references.add(path);
                return;
            }
            JsonElement parsed = JsonParser.parseString(text);
            if (!containsExactString(parsed, ECHO_SHARD)) {
                return;
            }
            references.add(path);
            if (path.contains("/loot_table/")) {
                lootSources.add(path);
            }
        });

        assertEquals(Set.of(
                "data/blessings/recipe/swift_sneak_soul_speed.json",
                "data/crafting/recipe/crystal_heart.json",
                "data/crafting/recipe/music_disc_5.json",
                "data/main/advancement/recipe_unlocks/echo_shard.json",
                "data/matcha_heart_death_compat/recipe/reinforced_crystal_heart.json",
                "data/minecraft/loot_table/chests/ancient_city.json"), references);
        assertEquals(Set.of("data/minecraft/loot_table/chests/ancient_city.json"), lootSources);
    }

    @Test
    void everyPackWideEchoShardTextReferenceIsClassified() throws Exception {
        Set<String> references = new HashSet<>();
        effectiveData().forEach((path, text) -> {
            if (text.contains("echo_shard")) {
                references.add(path);
            }
        });
        assertEquals(Set.of(
                "data/blessings/recipe/swift_sneak_soul_speed.json",
                "data/crafting/recipe/crystal_heart.json",
                "data/crafting/recipe/music_disc_5.json",
                "data/main/advancement/recipe_unlocks/echo_shard.json",
                "data/main/function/setup/revoke_all_recipe_unlock_advancements.mcfunction",
                "data/matcha_heart_death_compat/recipe/reinforced_crystal_heart.json",
                "data/minecraft/loot_table/chests/ancient_city.json"), references);
    }

    @Test
    void ancientCityRemainsTheUnmodifiedMatchaLootSource() throws Exception {
        String path = "data/minecraft/loot_table/chests/ancient_city.json";
        assertFalse(HeartDataContract.LOOT_TABLE_RESOURCES.containsValue(path));
        assertEquals(matchaEntry(path), effectiveData().get(path));
    }

    @Test
    void intendedEchoShardSinksRemainExact() throws Exception {
        Map<String, String> effective = effectiveData();

        JsonObject disc = JsonParser.parseString(
                effective.get("data/crafting/recipe/music_disc_5.json")).getAsJsonObject();
        assertEquals("minecraft:echo_shard", disc.getAsJsonObject("key").get("X").getAsString());
        assertEquals(1, countSymbol(disc, 'X'));

        JsonObject blessing = JsonParser.parseString(
                effective.get("data/blessings/recipe/swift_sneak_soul_speed.json")).getAsJsonObject();
        assertEquals("minecraft:echo_shard", blessing.getAsJsonObject("key").get("F").getAsString());
        assertEquals(3, countSymbol(blessing, 'F'));

        JsonObject crystal = JsonParser.parseString(
                effective.get("data/crafting/recipe/crystal_heart.json")).getAsJsonObject();
        assertEquals(List.of("ddd", "ded", "ddd"),
                crystal.getAsJsonArray("pattern").asList().stream()
                        .map(JsonElement::getAsString).toList());
        assertEquals("minecraft:echo_shard", crystal.getAsJsonObject("key").get("e").getAsString());
        assertEquals(1, countSymbol(crystal, 'e'));
        assertEquals("minecraft:diamond", crystal.getAsJsonObject("key").get("d").getAsString());
        assertEquals(8, countSymbol(crystal, 'd'));
        JsonObject crystalResult = crystal.getAsJsonObject("result");
        JsonObject crystalComponents = crystalResult.getAsJsonObject("components");
        assertEquals("minecraft:poisonous_potato", crystalResult.get("id").getAsString());
        assertEquals(1, crystalResult.get("count").getAsInt());
        assertEquals("minecraft:heart_container",
                crystalComponents.get("minecraft:item_model").getAsString());
        assertEquals("item.kleispack.crystal_heart",
                crystalComponents.getAsJsonObject("minecraft:item_name").get("translate").getAsString());
        assertEquals("rare", crystalComponents.get("minecraft:rarity").getAsString());
        assertTrue(crystalComponents.get("minecraft:enchantment_glint_override").getAsBoolean());
        assertTrue(crystalComponents.has("!minecraft:consumable"));

        JsonObject reinforced = JsonParser.parseString(effective.get(
                "data/matcha_heart_death_compat/recipe/reinforced_crystal_heart.json")).getAsJsonObject();
        assertEquals("minecraft:echo_shard", reinforced.getAsJsonObject("key").get("E").getAsString());
        assertEquals(6, countSymbol(reinforced, 'E'));
        assertEquals("fabric:components", reinforced.getAsJsonObject("key")
                .getAsJsonObject("H").get("fabric:type").getAsString());
    }

    @Test
    void recipeDiscoveryRemainsOwnedByTheCorrectMaterials() throws Exception {
        Map<String, String> effective = effectiveData();
        JsonObject vein = JsonParser.parseString(effective.get(
                "data/main/advancement/recipe_unlocks/sculk_vein.json")).getAsJsonObject();
        Set<String> veinRewards = new HashSet<>();
        vein.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .forEach(recipe -> veinRewards.add(recipe.getAsString()));
        assertEquals(Set.of("crafting:sculk", "crafting:sculk_sensor", "crafting:sculk_shrieker"),
                veinRewards);

        JsonObject echo = JsonParser.parseString(effective.get(
                "data/main/advancement/recipe_unlocks/echo_shard.json")).getAsJsonObject();
        Set<String> echoRewards = new HashSet<>();
        echo.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .forEach(recipe -> echoRewards.add(recipe.getAsString()));
        assertEquals(Set.of("crafting:music_disc_5"), echoRewards);

        JsonObject hellBound = JsonParser.parseString(effective.get(
                "data/main/advancement/recipe_unlocks/hell_bound_book.json")).getAsJsonObject();
        assertTrue(hellBound.getAsJsonObject("rewards").getAsJsonArray("recipes").asList().stream()
                .anyMatch(recipe -> "blessings:swift_sneak_soul_speed".equals(recipe.getAsString())));
    }

    @Test
    void allRenewableMatchaSourcePathsAreOverlaidWithoutShards() throws Exception {
        Map<String, String> effective = effectiveData();
        HeartDataContract.LOOT_TABLE_RESOURCES.values().forEach(path -> {
            assertTrue(effective.containsKey(path), path);
            assertFalse(containsExactString(JsonParser.parseString(effective.get(path)), ECHO_SHARD), path);
        });
    }

    @Test
    void matchaStillFiltersTheVanillaRecoveryCompassRecipe() throws Exception {
        try (ZipFile zip = new ZipFile(MATCHA.toFile())) {
            var entry = zip.getEntry("pack.mcmeta");
            assertTrue(entry != null, "Matcha pack.mcmeta is missing");
            String text;
            try (var stream = zip.getInputStream(entry)) {
                text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject pack = JsonParser.parseString(text).getAsJsonObject();
            assertTrue(pack.getAsJsonObject("filter").getAsJsonArray("block").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .anyMatch(rule -> "minecraft".equals(rule.get("namespace").getAsString())
                            && "recipe/recovery_compass.json".equals(rule.get("path").getAsString())));
        }
    }

    private static Map<String, String> effectiveData() throws IOException {
        Map<String, String> effective = new HashMap<>();
        try (ZipFile zip = new ZipFile(MATCHA.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith("data/")) {
                    continue;
                }
                try (var stream = zip.getInputStream(entry)) {
                    effective.put(entry.getName(), new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        }

        Path heartResources = WORKBENCH.resolve(
                "projects/matcha-heart-death-compat/src/main/resources/data");
        try (var files = Files.walk(heartResources)) {
            files.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relative = "data/" + heartResources.relativize(path).toString().replace('\\', '/');
                        try {
                            effective.put(relative, Files.readString(path, StandardCharsets.UTF_8));
                        } catch (IOException exception) {
                            throw new IllegalStateException("Could not read unified Heart data " + path, exception);
                        }
                    });
        }
        return effective;
    }

    private static String matchaEntry(String path) throws IOException {
        try (ZipFile zip = new ZipFile(MATCHA.toFile())) {
            var entry = zip.getEntry(path);
            if (entry == null) {
                throw new IllegalStateException("Missing Matcha entry " + path);
            }
            try (var stream = zip.getInputStream(entry)) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static boolean containsExactString(JsonElement element, String expected) {
        if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().isString() && expected.equals(element.getAsString());
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsExactString(child, expected)) {
                    return true;
                }
            }
        } else if (element.isJsonObject()) {
            for (var child : element.getAsJsonObject().entrySet()) {
                if (containsExactString(child.getValue(), expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countSymbol(JsonObject recipe, char symbol) {
        return recipe.getAsJsonArray("pattern").asList().stream()
                .mapToInt(row -> (int) row.getAsString().chars().filter(character -> character == symbol).count())
                .sum();
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }
}
