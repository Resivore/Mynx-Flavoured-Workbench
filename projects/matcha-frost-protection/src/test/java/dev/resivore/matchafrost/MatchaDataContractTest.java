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
    private static final String EXPECTED_ARCHIVE_SHA_256 =
            "6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248";
    private static final String EXPECTED_ENCHANTMENT_ENTRY_SHA_256 =
            "7AE531C8C0F414A4A3D798321066C8434327FD6F0F7B013CA987C6FAF65E320F";
    private static final String EXPECTED_RECIPE_ENTRY_SHA_256 =
            "5736906FDF677215CBC18D8289F4733CBD2F187D856A419B2380A88957DA0989";
    private static final String EXPECTED_TICK_TAG_ENTRY_SHA_256 =
            "E4091BB87C8498385914B5C1DC8CCB484607D9C6FEF7DBB807F2B4A77288DC6E";
    private static final String EXPECTED_SETUP_TICK_ENTRY_SHA_256 =
            "D24271C88D308A4994CA202F635C20A252910DECE69A4A76AFEE0218B7276BC2";
    private static final String EXPECTED_TICKING_FUNCTIONS_ENTRY_SHA_256 =
            "820A6A68768212256466D88119B589DE35BA52D67B25E95E76A992E4E4A8B10E";
    private static final String EXPECTED_FREEZING_CONDITIONS_ENTRY_SHA_256 =
            "B6690B959DE9377EA3DEF032D00F96F4913C4186825AA8561C1FF88E10C1EE73";
    private static final String EXPECTED_FREEZING_PENALTIES_ENTRY_SHA_256 =
            "84A3195CCD30EF91EB5ED3A2A84DB237F28C65E72EA4C88D1C985D2958FE5087";
    private static final String ENCHANTMENT_ENTRY =
            "data/main/enchantment/freezing_protection.json";
    private static final String RECIPE_ENTRY =
            "data/blessings/recipe/frost_walker_frost_protection.json";
    private static final String TICK_TAG_ENTRY =
            "data/minecraft/tags/function/tick.json";
    private static final String SETUP_TICK_ENTRY =
            "data/main/function/setup/tick.mcfunction";
    private static final String TICKING_FUNCTIONS_ENTRY =
            "data/main/function/setup/ticking_functions.mcfunction";
    private static final String FREEZING_CONDITIONS_ENTRY =
            "data/main/function/environmental/check_freezing_water_conditions.mcfunction";
    private static final String FREEZING_PENALTIES_ENTRY =
            "data/main/function/environmental/freezing_water.mcfunction";

    @Test
    void pinsTheExactMatcha112Inputs() throws Exception {
        Path archive = archivePath();
        assertTrue(Files.isRegularFile(archive), () -> "Missing pristine Matcha archive: " + archive);
        assertEquals(EXPECTED_ARCHIVE_SHA_256, sha256(Files.readAllBytes(archive)));

        try (ZipFile zip = new ZipFile(archive.toFile(), UTF_8)) {
            assertEquals(EXPECTED_ENCHANTMENT_ENTRY_SHA_256, sha256(readEntryBytes(zip, ENCHANTMENT_ENTRY)));
            assertEquals(EXPECTED_RECIPE_ENTRY_SHA_256, sha256(readEntryBytes(zip, RECIPE_ENTRY)));
            assertEquals(EXPECTED_TICK_TAG_ENTRY_SHA_256, sha256(readEntryBytes(zip, TICK_TAG_ENTRY)));
            assertEquals(EXPECTED_SETUP_TICK_ENTRY_SHA_256, sha256(readEntryBytes(zip, SETUP_TICK_ENTRY)));
            assertEquals(
                    EXPECTED_TICKING_FUNCTIONS_ENTRY_SHA_256,
                    sha256(readEntryBytes(zip, TICKING_FUNCTIONS_ENTRY)));
            assertEquals(
                    EXPECTED_FREEZING_CONDITIONS_ENTRY_SHA_256,
                    sha256(readEntryBytes(zip, FREEZING_CONDITIONS_ENTRY)));
            assertEquals(
                    EXPECTED_FREEZING_PENALTIES_ENTRY_SHA_256,
                    sha256(readEntryBytes(zip, FREEZING_PENALTIES_ENTRY)));
        }
    }

    @Test
    void preservesFrostProtectionAndItsSeparateFreezingWaterContract() throws IOException {
        try (ZipFile zip = openArchive()) {
            JsonObject enchantment = parseObject(readEntry(zip, ENCHANTMENT_ENTRY));
            assertEquals(3, enchantment.get("max_level").getAsInt());
            assertEquals("armor", enchantment.getAsJsonArray("slots").get(0).getAsString());
            assertEquals(1, enchantment.getAsJsonArray("slots").size());
            assertEquals(
                    "#minecraft:enchantable/chest_armor",
                    enchantment.get("supported_items").getAsString());

            JsonObject protection = enchantment.getAsJsonObject("effects")
                    .getAsJsonArray("minecraft:damage_protection")
                    .get(0).getAsJsonObject();
            JsonObject value = protection.getAsJsonObject("effect").getAsJsonObject("value");
            assertEquals(24, value.get("base").getAsInt());
            assertEquals(24, value.get("per_level_above_first").getAsInt());
            assertTrue(protection.toString().contains("minecraft:is_freezing"));

            String freezingWater = readEntry(
                    zip, FREEZING_CONDITIONS_ENTRY);
            assertTrue(freezingWater.contains(
                    "equipment:{chest:{components:{\"minecraft:enchantments\":{\"main:freezing_protection\":3}}}}"));
        }
    }

    @Test
    void pinsTheCompleteActiveMatchaFreezingWaterPenaltyChain() throws IOException {
        try (ZipFile zip = openArchive()) {
            JsonObject tickTag = parseObject(readEntry(zip, TICK_TAG_ENTRY));
            assertEquals(1, tickTag.getAsJsonArray("values").size());
            assertEquals("main:setup/tick", tickTag.getAsJsonArray("values").get(0).getAsString());

            assertEquals(
                    List.of("function main:setup/ticking_functions"),
                    activeLines(readEntry(zip, SETUP_TICK_ENTRY)));
            List<String> tickingFunctions = activeLines(readEntry(zip, TICKING_FUNCTIONS_ENTRY));
            assertEquals(
                    1,
                    tickingFunctions.stream()
                            .filter("function main:environmental/check_freezing_water_conditions"::equals)
                            .count());

            List<String> conditions = activeLines(readEntry(zip, FREEZING_CONDITIONS_ENTRY));
            assertEquals(1, conditions.size());
            String condition = conditions.get(0);
            assertEquals(
                    MatchaFreezingWaterFunctionEnforcer.EXPECTED_UPSTREAM_CONDITION_COMMAND,
                    condition);
            assertAll(
                    () -> assertTrue(condition.startsWith("execute at @a[gamemode=!creative]")),
                    () -> assertTrue(condition.contains("if block ~ ~1 ~ water")),
                    () -> assertTrue(condition.contains("equipment:{chest:")),
                    () -> assertTrue(condition.contains("\"main:freezing_protection\":3")),
                    () -> assertTrue(condition.contains("if biome ~ ~ ~ #minecraft:is_frozen")),
                    () -> assertTrue(condition.endsWith(
                            "run function main:environmental/freezing_water")));

            assertEquals(
                    List.of(
                            "effect give @p slowness 5 4 true",
                            "effect give @p darkness 5 0 true",
                            "damage @p 1 freeze"),
                    activeLines(readEntry(zip, FREEZING_PENALTIES_ENTRY)));
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
                        "execute as @a[gamemode=!creative] at @s if block ~ ~1 ~ water")),
                () -> assertTrue(command.contains("equipment:{head:")),
                () -> assertTrue(command.contains("equipment:{chest:")),
                () -> assertTrue(command.contains("equipment:{legs:")),
                () -> assertTrue(command.contains("equipment:{feet:")),
                () -> assertEquals(4, occurrences(command, "\"main:freezing_protection\":3")),
                () -> assertEquals(4, occurrences(command, "unless entity @s[nbt=")),
                () -> assertTrue(command.endsWith(
                        "if biome ~ ~ ~ #minecraft:is_frozen run function "
                                + "matcha_frost_protection:environmental/freezing_water")),
                () -> assertFalse(command.contains(
                        "run function main:environmental/freezing_water")),
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
        assertEquals(2, upstreamStored.get("main:freezing_protection").getAsInt());

        JsonObject canonicalStored = storedEnchantments(canonical);
        assertEquals(1, canonicalStored.size());
        assertEquals(3, canonicalStored.get("main:freezing_protection").getAsInt());
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
                .resolve("originals")
                .resolve("datapacks")
                .resolve("Matcha_Flavoured_1_12.zip");
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
