package dev.resivore.matchafrost;

import static java.nio.charset.StandardCharsets.UTF_8;
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
    private static final String ENCHANTMENT_ENTRY =
            "data/main/enchantment/freezing_protection.json";
    private static final String RECIPE_ENTRY =
            "data/blessings/recipe/frost_walker_frost_protection.json";

    @Test
    void pinsTheExactMatcha112Inputs() throws Exception {
        Path archive = archivePath();
        assertTrue(Files.isRegularFile(archive), () -> "Missing pristine Matcha archive: " + archive);
        assertEquals(EXPECTED_ARCHIVE_SHA_256, sha256(Files.readAllBytes(archive)));

        try (ZipFile zip = new ZipFile(archive.toFile(), UTF_8)) {
            assertEquals(EXPECTED_ENCHANTMENT_ENTRY_SHA_256, sha256(readEntryBytes(zip, ENCHANTMENT_ENTRY)));
            assertEquals(EXPECTED_RECIPE_ENTRY_SHA_256, sha256(readEntryBytes(zip, RECIPE_ENTRY)));
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
                    zip, "data/main/function/environmental/check_freezing_water_conditions.mcfunction");
            assertTrue(freezingWater.contains(
                    "equipment:{chest:{components:{\"minecraft:enchantments\":{\"main:freezing_protection\":3}}}}"));
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
