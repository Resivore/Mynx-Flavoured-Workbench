package dev.resivore.matchafrost;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class MatchaDataContractTest {
    private static final String RECIPE_ENTRY = "data/matcha/recipe/blessing/frost_walker_frost_protection.json";
    private static final String FREEZING_PENALTIES_ENTRY = "data/matcha/function/environmental/freezing_water.mcfunction";

    @Test
    void usesThePublishedTickChainAndChestOnlyEnchantment() throws IOException {
        try (ZipFile zip = openArchive()) {
            JsonObject enchantment = parseObject(readEntry(zip, "data/matcha/enchantment/freezing_protection.json"));
            assertEquals(3, enchantment.get("max_level").getAsInt());
            assertEquals("#minecraft:enchantable/chest_armor", enchantment.get("supported_items").getAsString());
            assertTrue(enchantment.toString().contains("minecraft:is_freezing"));
            assertEquals(List.of(MatchaFreezingWaterFunctionEnforcer.EXPECTED_UPSTREAM_CONDITION_COMMAND),
                    activeLines(readEntry(zip, "data/matcha/function/environmental/check_freezing_water_conditions.mcfunction")));
            assertTrue(readEntry(zip, "data/minecraft/tags/function/tick.json").contains("matcha:timers/tick"));
        }
    }

    @Test
    void canonicalGuardBypassesTheWholePenaltyFunctionForExactLevelThreeOnAnySlot()
            throws IOException {
        List<String> commands = activeLines(Files.readString(canonicalColdWaterPath(), UTF_8));
        assertEquals(1, commands.size());
        String command = commands.get(0);
        assertEquals(
                MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_CONDITION_COMMAND,
                command);

        assertAll(
                () -> assertTrue(command.startsWith(
                        "execute as @a[gamemode=!creative,gamemode=!spectator] at @s if block ~ ~1 ~ water")),
                () -> assertTrue(command.contains("equipment:{head:")),
                () -> assertTrue(command.contains("equipment:{chest:")),
                () -> assertTrue(command.contains("equipment:{legs:")),
                () -> assertTrue(command.contains("equipment:{feet:")),
                () -> assertEquals(4, occurrences(command, "\"matcha:freezing_protection\":3")),
                () -> assertEquals(4, occurrences(command, "unless entity @s[nbt=")),
                () -> assertTrue(command.endsWith(
                        "if biome ~ ~ ~ #minecraft:is_frozen run function "
                                + "matcha_frost_protection:environmental/freezing_water")),
                () -> assertFalse(command.contains(
                        "run function matcha:environmental/freezing_water")),
                () -> assertFalse(command.contains("effect give")),
                () -> assertFalse(command.contains("effect clear")),
                () -> assertFalse(command.contains("damage @")),
                () -> assertFalse(command.contains("TicksFrozen")));

        try (ZipFile zip = openArchive()) {
            List<String> upstreamPenalties = activeLines(
                    readEntry(zip, FREEZING_PENALTIES_ENTRY));
            List<String> playerLocalPenalties = activeLines(
                    Files.readString(canonicalPenaltyPath(), UTF_8));
            assertEquals(
                    upstreamPenalties.stream()
                            .map(line -> line.replace("@p", "@s"))
                            .toList(),
                    playerLocalPenalties,
                    "The canonical penalty body may differ only by using the guarded player");
            assertEquals(
                    MatchaFreezingWaterFunctionEnforcer.EXPECTED_CANONICAL_PENALTY_COMMANDS,
                    playerLocalPenalties);
            assertTrue(playerLocalPenalties.stream().allMatch(line -> line.contains("@s")));
            assertFalse(playerLocalPenalties.stream().anyMatch(line -> line.contains("@p")));
        }
    }

    @Test
    void changesOnlyDemetersCompleteStoredEnchantmentSet() throws IOException {
        assertFalse(BlessingRecipeEnforcer.CANONICAL_RESOURCE.startsWith("data/"),
                "The canonical JSON must not add or compete for the Matcha recipe by pack priority");

        JsonObject upstream;
        try (ZipFile zip = openArchive()) {
            upstream = parseObject(readEntry(zip, RECIPE_ENTRY));
        }
        JsonObject canonical = parseObject(Files.readString(canonicalRecipePath(), UTF_8));

        JsonObject upstreamStored = storedEnchantments(upstream);
        assertEquals(2, upstreamStored.size());
        assertEquals(2, upstreamStored.get("minecraft:frost_walker").getAsInt());
        assertEquals(2, upstreamStored.get("matcha:freezing_protection").getAsInt());

        JsonObject canonicalStored = storedEnchantments(canonical);
        assertEquals(1, canonicalStored.size());
        assertEquals(3, canonicalStored.get("matcha:freezing_protection").getAsInt());
        assertFalse(canonicalStored.has("minecraft:frost_walker"));

        storedComponents(upstream).add(
                "minecraft:stored_enchantments", canonicalStored.deepCopy());
        assertEquals(upstream, canonical,
                "Ingredients, pattern, method, item identity, name, lore, and model must remain exact");
    }

    private static JsonObject storedEnchantments(JsonObject recipe) {
        return storedComponents(recipe).getAsJsonObject("minecraft:stored_enchantments");
    }

    private static JsonObject storedComponents(JsonObject recipe) {
        return recipe.getAsJsonObject("result").getAsJsonObject("components");
    }

    private static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static Path archivePath() {
        return requiredSystemPath("workbenchRoot")
                .resolve("projects/matcha-flavoured-data/upstream/MF_1_12_5_26_2.zip");
    }

    private static Path canonicalRecipePath() {
        return requiredSystemPath("projectRoot")
                .resolve("src/main/resources")
                .resolve(BlessingRecipeEnforcer.CANONICAL_RESOURCE);
    }

    private static Path canonicalColdWaterPath() {
        return requiredSystemPath("projectRoot")
                .resolve("src/main/resources/data/matcha_frost_protection/function/environmental")
                .resolve("check_freezing_water_conditions.mcfunction");
    }

    private static Path canonicalPenaltyPath() {
        return requiredSystemPath("projectRoot")
                .resolve("src/main/resources/data/matcha_frost_protection/function/environmental")
                .resolve("freezing_water.mcfunction");
    }

    private static List<String> activeLines(String content) {
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .toList();
    }

    private static int occurrences(String content, String needle) {
        return (content.length() - content.replace(needle, "").length()) / needle.length();
    }

    private static Path requiredSystemPath(String propertyName) {
        String value = System.getProperty(propertyName);
        assertNotNull(value, () -> "Gradle must provide the " + propertyName + " system property");
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static ZipFile openArchive() throws IOException {
        return new ZipFile(archivePath().toFile(), UTF_8);
    }

    private static String readEntry(ZipFile archive, String name) throws IOException {
        return new String(readEntryBytes(archive, name), UTF_8);
    }

    private static byte[] readEntryBytes(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        assertNotNull(entry, () -> "Missing Matcha ZIP entry: " + name);
        try (InputStream input = archive.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                .toUpperCase(Locale.ROOT);
    }
}
