package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.yungnickyoung.minecraft.ribbits.entity.trade.MatchaStackCatalog.FixedStack;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MatchaStackCatalogFingerprintTest {
    private static final Map<FixedStack, String> EXPECTED_RESULT_CONTRACTS = Map.ofEntries(
            entry(FixedStack.GLOW_BERRY_CRUMBLE,
                    "c064c3201c4c44dedd6f8e9ec51107d6bf8db48cb9ca755b3dd69b4cb8fe54d8"),
            entry(FixedStack.HONEY_GINGER_TEA,
                    "0e3faaf4946150a2ced63bb76c7ade6763329ed6cf98959cefa125f69d536e02"),
            entry(FixedStack.PICKLED_CARROTS,
                    "b844bc604d3c9ac0ef6ccf6dc6eb0a07b4a0290b7ed7d4525b007b572640e8d0"),
            entry(FixedStack.RIND_JAM,
                    "0a0b06c482585a83e71ead3addb4ee7629352e091aec34b18fc8b7ec13149734"),
            entry(FixedStack.GIMMARI,
                    "d7e097e932457245e04fd775191a89b77290b36967909ad94e1b539576d85356"),
            entry(FixedStack.BOKGUK,
                    "a9c4b66d5822f96f5f6ff1dc130985ed19904f30d71ddc3358bbc50cf72807db"),
            entry(FixedStack.GOLDEN_PICKLED_CARROTS,
                    "491828999ebf66e4c087b1147ffc02dcd52cce734c87db1895ccdcb09be8bd3b"),
            entry(FixedStack.MELON_SORBET,
                    "0a0a305672c275c80bc1542da1dafdbdbb9b3b1e6836311fb216f990f0d261be"),
            entry(FixedStack.PUMPKIN_EMPANADA,
                    "4c8f335cf0b68a6a7c0e081ece849a61bf4fc56780acfdca6af8ec0a61ee10e9"),
            entry(FixedStack.WARPED_PIZZA,
                    "3348e8211f9785d168a5e961852a280cfe7467355c32b0637581d4f455d34ef5"),
            entry(FixedStack.WARPED_STROGANOFF,
                    "160ef7524ae7b5b7a16c814d45228dbd68add234e5b76066ba6b1836fa80d6cf"),
            entry(FixedStack.SWEET_BERRY_DANISH,
                    "d4ac8d81e6051ec9443e32000e0873c7ffdf0a270291ee5aeed4c1f5c1608062"),
            entry(FixedStack.GOLDEN_CARROT_CUPCAKE,
                    "5f10d771f35a3a65fa0af46fd649b45ccc1d40a6568ebd3e34350ef460dafad7"),
            entry(FixedStack.JAPANESE_CURRY,
                    "5f0cc28abe03a02a29adac81b2b51a214f9ffcb808cf550f98366be4de8fbc4d"),
            entry(FixedStack.GREEN_CURRY,
                    "fb7f996ffe2bbb3b7bac03e8e34f25209436b35e6a3f0213a24e00310a55b10b"),
            entry(FixedStack.TONKOTSU_RAMEN,
                    "aacd5a5fd956eaca546d5494f7efca863909562291e00cf3552918f2341c0da8"),
            entry(FixedStack.BENZENE,
                    "e487be207d1243e21ad3e22b2aae4f01f36991e01f0c1c8818bf2563da93474c"),
            entry(FixedStack.ESTUS_FLASK,
                    "ec7b84853cad4b17a8edb5992e33604465c12f36b2cfcb1e485307b8aa4cc56a"),
            entry(FixedStack.REACH_BLESSING,
                    "96a224ce3ea7d251fbb1fad4de8273b1a5ef67045c0c418f1b7b656b35f1e62d"),
            entry(FixedStack.SILK_TOUCH_BLESSING,
                    "07e7a800dad6478c06038ec63ed56784abd21cc0c88e1772594dfd058fbb1cac"),
            entry(FixedStack.CARBON_RICH_IRON,
                    "768c3c7d98cec838607b075310f164e044eeb7e6e60bd5fc54c5f2072cb11b06"),
            entry(FixedStack.HEPATIZON_ALLOY,
                    "eb76460c156a6bd20b103b8ed747e0c6e540ed2a1163fa5f768a5e78de02adf8"),
            entry(FixedStack.SHAKUDO_ALLOY,
                    "89b858dfdffadc865c29f9ca055869fbd70a913992c12f03a346e38a0493dc41"),
            entry(FixedStack.ELECTRUM_ALLOY,
                    "b9d815288f637d303bb5e28ea8ad5cf2d97089debfda218eceb8cb0ef0d41a3f"),
            entry(FixedStack.DIVINE_FRAGMENT,
                    "593a62e2da9c4c1e3cad1f173bf143b81243a3579e6b9a2a87b0b78582df4174"),
            entry(FixedStack.CRYSTAL_HEART,
                    "34f33cb21c8433418e547ccc97d869097adfe19104331f0885d993d86c16ba6b")
    );

    @Test
    void everyFixedStackHasThePinnedAuditedResultContract() {
        assertEquals(26, FixedStack.values().length);
        assertEquals(EXPECTED_RESULT_CONTRACTS.keySet(),
                new HashSet<>(java.util.List.of(FixedStack.values())));
        for (FixedStack stack : FixedStack.values()) {
            String digest = stack.resultContractSha256();
            assertEquals(EXPECTED_RESULT_CONTRACTS.get(stack), digest, stack.recipeId().toString());
            assertTrue(digest.matches("[0-9a-f]{64}"), stack.recipeId().toString());
        }
        assertEquals(26, new HashSet<>(EXPECTED_RESULT_CONTRACTS.values()).size());
    }

    @Test
    void canonicalizationIgnoresObjectKeyOrderButPreservesArraysAndValues() {
        JsonElement first = JsonParser.parseString(
                "{\"z\":[1,2],\"a\":{\"rarity\":\"rare\",\"glint\":true}}");
        JsonElement reordered = JsonParser.parseString(
                "{\"a\":{\"glint\":true,\"rarity\":\"rare\"},\"z\":[1,2]}");
        JsonElement reversedArray = JsonParser.parseString(
                "{\"a\":{\"glint\":true,\"rarity\":\"rare\"},\"z\":[2,1]}");

        assertEquals(MatchaStackCatalog.canonicalJson(first),
                MatchaStackCatalog.canonicalJson(reordered));
        assertEquals(MatchaStackCatalog.canonicalContractSha256(first),
                MatchaStackCatalog.canonicalContractSha256(reordered));
        assertNotEquals(MatchaStackCatalog.canonicalContractSha256(first),
                MatchaStackCatalog.canonicalContractSha256(reversedArray));
    }

    @Test
    void divineFragmentContractPinsBaseCountRareRarityAndGlint() {
        JsonElement exact = JsonParser.parseString("""
                {
                  "id": "minecraft:turtle_scute",
                  "count": 9,
                  "components": {
                    "minecraft:rarity": "rare",
                    "minecraft:enchantment_glint_override": true
                  }
                }
                """);
        String expected = FixedStack.DIVINE_FRAGMENT.resultContractSha256();

        assertEquals(expected, MatchaStackCatalog.canonicalContractSha256(exact));
        assertDoesNotThrow(() -> MatchaStackCatalog.validateFingerprint(
                "Divine Fragment", exact, expected));

        for (String drifted : java.util.List.of(
                "{\"id\":\"minecraft:turtle_scute\",\"count\":9}",
                "{\"id\":\"minecraft:turtle_scute\",\"count\":1,\"components\":{\"minecraft:rarity\":\"rare\",\"minecraft:enchantment_glint_override\":true}}",
                "{\"id\":\"minecraft:turtle_scute\",\"count\":9,\"components\":{\"minecraft:rarity\":\"common\",\"minecraft:enchantment_glint_override\":true}}",
                "{\"id\":\"minecraft:turtle_scute\",\"count\":9,\"components\":{\"minecraft:rarity\":\"rare\",\"minecraft:enchantment_glint_override\":false}}"
        )) {
            JsonElement changed = JsonParser.parseString(drifted);
            assertNotEquals(expected, MatchaStackCatalog.canonicalContractSha256(changed));
            assertThrows(IllegalStateException.class, () -> MatchaStackCatalog.validateFingerprint(
                    "Divine Fragment", changed, expected));
        }
    }

    @Test
    void crystalHeartAndOpalContractsArePinnedToTheirActiveResourceLocations() {
        assertEquals(Identifier.parse("crafting:recipe/crystal_heart.json"),
                MatchaStackCatalog.recipeResource(FixedStack.CRYSTAL_HEART.recipeId()));
        assertEquals("34f33cb21c8433418e547ccc97d869097adfe19104331f0885d993d86c16ba6b",
                FixedStack.CRYSTAL_HEART.resultContractSha256());
        assertEquals(Identifier.parse("minecraft:loot_table/kleis_items/opal.json"),
                MatchaStackCatalog.OPAL_LOOT_RESOURCE);
        assertEquals("81d6a8b292697f291b3025dbfe4b591fbe86a35b1231164cbdd05d8a8bd04c0e",
                MatchaStackCatalog.OPAL_ENTRY_CONTRACT_SHA256);
    }

    @Test
    void activeContractsAreRevalidatedAfterEverySuccessfulDataPackReload() throws Exception {
        Path projectRoot = Path.of(System.getProperty("projectRoot"));
        String fabricEntryPoint = Files.readString(projectRoot.resolve(
                "fabric/src/main/java/com/yungnickyoung/minecraft/ribbits/fabric/RibbitsFabric.java"));
        int registration = fabricEntryPoint.indexOf(
                "ServerLifecycleEvents.END_DATA_PACK_RELOAD.register");
        int nextRegistration = fabricEntryPoint.indexOf(
                "ServerLifecycleEvents.SERVER_STOPPED.register", registration);

        assertTrue(registration >= 0, "successful reload validation callback must be registered");
        assertTrue(nextRegistration > registration, "reload callback boundary must be stable");
        String callback = fabricEntryPoint.substring(registration, nextRegistration);
        assertTrue(callback.contains("if (success)"),
                "a failed reload retains the already validated active data packs");
        assertTrue(callback.contains("RibbitTradeModule.validateRuntime(server.overworld());"),
                "the newly active resources must be checked before play resumes");
    }

    @Test
    void pinnedHashesReproduceFromTheExactImmutableReferencesWhenAvailable() throws Exception {
        String configured = System.getenv("MATCHA_26_2_REFERENCE_ZIP");
        assumeTrue(configured != null && !configured.isBlank(),
                "set MATCHA_26_2_REFERENCE_ZIP for immutable-reference provenance validation");

        Path matchaZip = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(matchaZip), matchaZip.toString());
        assertEquals("6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248",
                sha256(matchaZip), "immutable Matcha 1.12 archive identity");

        try (ZipFile zip = new ZipFile(matchaZip.toFile())) {
            for (FixedStack stack : FixedStack.values()) {
                String entryName = "data/" + stack.recipeId().getNamespace() + "/recipe/"
                        + stack.recipeId().getPath() + ".json";
                JsonElement result = readZipJson(zip, entryName).getAsJsonObject().get("result");
                assertNotNull(result, entryName + " result");
                assertEquals(stack.resultContractSha256(),
                        MatchaStackCatalog.canonicalContractSha256(result), entryName);
            }

            JsonElement opalRoot = readZipJson(zip,
                    "data/minecraft/loot_table/kleis_items/opal.json");
            JsonElement opalEntry = opalRoot.getAsJsonObject().getAsJsonArray("pools").get(0)
                    .getAsJsonObject().getAsJsonArray("entries").get(0);
            assertEquals(MatchaStackCatalog.OPAL_ENTRY_CONTRACT_SHA256,
                    MatchaStackCatalog.canonicalContractSha256(opalEntry), "Opal loot entry");
        }

        Path crystalRecipe = Path.of(System.getProperty("user.dir"))
                .resolve("../matcha-heart-death-compat/src/main/resources/data/crafting/recipe/crystal_heart.json")
                .normalize();
        assertTrue(Files.isRegularFile(crystalRecipe), crystalRecipe.toString());
        try (var reader = Files.newBufferedReader(crystalRecipe, StandardCharsets.UTF_8)) {
            JsonElement result = JsonParser.parseReader(reader).getAsJsonObject().get("result");
            assertNotNull(result, "Crystal Heart result");
            assertEquals(FixedStack.CRYSTAL_HEART.resultContractSha256(),
                    MatchaStackCatalog.canonicalContractSha256(result), "Crystal Heart result");
        }
    }

    private static JsonElement readZipJson(ZipFile zip, String entryName) throws Exception {
        ZipEntry entry = zip.getEntry(entryName);
        assertNotNull(entry, entryName);
        try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
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
