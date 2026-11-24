package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MatchaCompassCatalogFingerprintTest {
    private static final String MATCHA_SHA256 =
            "966843d0aeb3860479721ef34791a631e62bc225fdaa918dd2ad90c49977e5b4";

    @Test
    void recipeAndLootFingerprintsPinAllThreeOptionalCompassContracts() {
        assertEquals(2, MatchaCompassCatalog.RecipeCompass.values().length);
        assertEquals(Identifier.parse("matcha:crafting/copper_compass"),
                MatchaCompassCatalog.RecipeCompass.COPPER.recipeId());
        assertEquals("aeccd676282170cdf6cc468baf4338f64a64d4c5ae4c8e0dede5b545a979af4c",
                MatchaCompassCatalog.RecipeCompass.COPPER.resultContractSha256());
        assertEquals(Identifier.parse("matcha:copper_compass"),
                MatchaCompassCatalog.RecipeCompass.COPPER.itemModel());
        assertEquals("item.kleispack.copper_compass",
                MatchaCompassCatalog.RecipeCompass.COPPER.itemNameKey());

        assertEquals(Identifier.parse("matcha:crafting/golden_compass"),
                MatchaCompassCatalog.RecipeCompass.GOLDEN.recipeId());
        assertEquals("e566c6eb12041dc927b4d9afcf6f6de0799237b3014beb7da256f6f5a22b353e",
                MatchaCompassCatalog.RecipeCompass.GOLDEN.resultContractSha256());
        assertEquals(Identifier.parse("matcha:golden_compass"),
                MatchaCompassCatalog.RecipeCompass.GOLDEN.itemModel());
        assertEquals("item.kleispack.golden_compass",
                MatchaCompassCatalog.RecipeCompass.GOLDEN.itemNameKey());

        assertEquals(Identifier.parse(
                        "matcha:loot_table/equipment/compass/titanium.json"),
                MatchaCompassCatalog.TITANIUM_LOOT_RESOURCE);
        assertEquals("0ce3e6cb700e8fd841637e7117676b0ad54c61213f76bf78c14f90e1a34d1969",
                MatchaCompassCatalog.TITANIUM_ENTRY_CONTRACT_SHA256);
        assertEquals(Identifier.parse("matcha:titanium_compass"),
                MatchaCompassCatalog.TITANIUM_MODEL);
        assertEquals("item.kleispack.titanium_compass",
                MatchaCompassCatalog.TITANIUM_NAME_KEY);
    }

    @Test
    void canonicalContractsRejectModelNameAndBaseItemDrift() {
        JsonElement copper = JsonParser.parseString("""
                {"components": {"minecraft:item_model": "matcha:copper_compass", "minecraft:item_name": {"translate": "item.kleispack.copper_compass"}, "minecraft:custom_data": {"version": 1}}, "count": 1, "id": "minecraft:compass"}
                """);
        JsonElement golden = JsonParser.parseString("""
                {"components": {"minecraft:item_model": "matcha:golden_compass", "minecraft:item_name": {"translate": "item.kleispack.golden_compass"}, "minecraft:custom_data": {"version": 1}}, "count": 1, "id": "minecraft:compass"}
                """);
        JsonElement titanium = JsonParser.parseString("""
                {"type": "minecraft:item", "name": "minecraft:compass", "functions": [{"function": "minecraft:set_components", "components": {"minecraft:item_model": "matcha:titanium_compass", "minecraft:item_name": {"translate": "item.kleispack.titanium_compass"}, "minecraft:custom_data": {"version": 1}}}]}
                """);

        assertEquals(MatchaCompassCatalog.RecipeCompass.COPPER.resultContractSha256(),
                MatchaStackCatalog.canonicalContractSha256(copper));
        assertEquals(MatchaCompassCatalog.RecipeCompass.GOLDEN.resultContractSha256(),
                MatchaStackCatalog.canonicalContractSha256(golden));
        assertEquals(MatchaCompassCatalog.TITANIUM_ENTRY_CONTRACT_SHA256,
                MatchaStackCatalog.canonicalContractSha256(titanium));

        for (String drifted : List.of(
                copper.toString().replace("copper_compass", "titanium_compass"),
                golden.toString().replace("minecraft:compass", "minecraft:clock"),
                titanium.toString().replace("item.kleispack.titanium_compass",
                        "Titanium Compass")
        )) {
            JsonElement changed = JsonParser.parseString(drifted);
            assertNotEquals(MatchaCompassCatalog.TITANIUM_ENTRY_CONTRACT_SHA256,
                    MatchaStackCatalog.canonicalContractSha256(changed));
        }
        assertThrows(IllegalStateException.class, () ->
                MatchaStackCatalog.validateFingerprint("Copper Compass", golden,
                        MatchaCompassCatalog.RecipeCompass.COPPER.resultContractSha256()));
    }

    @Test
    void pinnedHashesReproduceFromTheImmutableMatchaReferenceWhenAvailable() throws Exception {
        String configured = System.getenv("MATCHA_26_2_REFERENCE_ZIP");
        assumeTrue(configured != null && !configured.isBlank(),
                "set MATCHA_26_2_REFERENCE_ZIP for immutable-reference provenance validation");

        Path matchaZip = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(matchaZip), matchaZip.toString());
        assertEquals(MATCHA_SHA256, sha256(matchaZip), "immutable Matcha 1.12 archive identity");

        try (ZipFile zip = new ZipFile(matchaZip.toFile())) {
            for (MatchaCompassCatalog.RecipeCompass compass
                    : MatchaCompassCatalog.RecipeCompass.values()) {
                String entryName = "data/" + compass.recipeId().getNamespace() + "/recipe/"
                        + compass.recipeId().getPath() + ".json";
                JsonElement result = readZipJson(zip, entryName)
                        .getAsJsonObject().get("result");
                assertNotNull(result, entryName + " result");
                assertEquals(compass.resultContractSha256(),
                        MatchaStackCatalog.canonicalContractSha256(result), entryName);
            }

            JsonElement titaniumRoot = readZipJson(zip,
                    "data/matcha/loot_table/equipment/compass/titanium.json");
            JsonElement titaniumEntry = titaniumRoot.getAsJsonObject()
                    .getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0);
            assertEquals(MatchaCompassCatalog.TITANIUM_ENTRY_CONTRACT_SHA256,
                    MatchaStackCatalog.canonicalContractSha256(titaniumEntry));
        }
    }

    private static JsonElement readZipJson(ZipFile zip, String entryName) throws Exception {
        ZipEntry entry = zip.getEntry(entryName);
        assertNotNull(entry, entryName);
        try (var reader = new InputStreamReader(zip.getInputStream(entry),
                StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
