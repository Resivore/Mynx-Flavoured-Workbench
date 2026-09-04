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
            "6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248";

    @Test
    void recipeAndLootFingerprintsPinAllThreeOptionalCompassContracts() {
        assertEquals(2, MatchaCompassCatalog.RecipeCompass.values().length);
        assertEquals(Identifier.parse("crafting:copper_compass"),
                MatchaCompassCatalog.RecipeCompass.COPPER.recipeId());
        assertEquals("afaf346942b288588aed7fdd018caae5d1a9dc1d1250d4ad8b1d7fdb1bb26a6e",
                MatchaCompassCatalog.RecipeCompass.COPPER.resultContractSha256());
        assertEquals(Identifier.parse("minecraft:copper_compass"),
                MatchaCompassCatalog.RecipeCompass.COPPER.itemModel());
        assertEquals("item.kleispack.copper_compass",
                MatchaCompassCatalog.RecipeCompass.COPPER.itemNameKey());

        assertEquals(Identifier.parse("crafting:golden_compass"),
                MatchaCompassCatalog.RecipeCompass.GOLDEN.recipeId());
        assertEquals("16c631e5ea09614dcdf7625c9631d7ffb2ad5f359e034052031209f1c5bd6fcc",
                MatchaCompassCatalog.RecipeCompass.GOLDEN.resultContractSha256());
        assertEquals(Identifier.parse("minecraft:golden_compass"),
                MatchaCompassCatalog.RecipeCompass.GOLDEN.itemModel());
        assertEquals("item.kleispack.golden_compass",
                MatchaCompassCatalog.RecipeCompass.GOLDEN.itemNameKey());

        assertEquals(Identifier.parse(
                        "minecraft:loot_table/chests/equipment/special_compass.json"),
                MatchaCompassCatalog.TITANIUM_LOOT_RESOURCE);
        assertEquals("d9cb0c40eaed8c05e7b634cf2043ac196927d56120be558be3bd091ae1661116",
                MatchaCompassCatalog.TITANIUM_ENTRY_CONTRACT_SHA256);
        assertEquals(Identifier.parse("minecraft:titanium_compass"),
                MatchaCompassCatalog.TITANIUM_MODEL);
        assertEquals("item.kleispack.titanium_compass",
                MatchaCompassCatalog.TITANIUM_NAME_KEY);
    }

    @Test
    void canonicalContractsRejectModelNameAndBaseItemDrift() {
        JsonElement copper = JsonParser.parseString("""
                {
                  "components": {
                    "minecraft:item_model": "minecraft:copper_compass",
                    "minecraft:item_name": {"translate": "item.kleispack.copper_compass"}
                  },
                  "count": 1,
                  "id": "minecraft:compass"
                }
                """);
        JsonElement golden = JsonParser.parseString("""
                {
                  "components": {
                    "minecraft:item_model": "minecraft:golden_compass",
                    "minecraft:item_name": {"translate": "item.kleispack.golden_compass"}
                  },
                  "count": 1,
                  "id": "minecraft:compass"
                }
                """);
        JsonElement titanium = JsonParser.parseString("""
                {
                  "type": "minecraft:item",
                  "name": "minecraft:compass",
                  "quality": 2,
                  "functions": [{
                    "function": "minecraft:set_components",
                    "components": {
                      "minecraft:item_model": "minecraft:titanium_compass",
                      "minecraft:item_name": {"translate": "item.kleispack.titanium_compass"}
                    }
                  }]
                }
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
                    "data/minecraft/loot_table/chests/equipment/special_compass.json");
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
