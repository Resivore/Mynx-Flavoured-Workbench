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
                    "f26a16561d4f6ab37cd524546a6a4c03a65590d10ef2b6060d74bad0f69c26d0"),
            entry(FixedStack.HONEY_GINGER_TEA,
                    "371e965c948c1e8a8df46a019b911069814342d432a2ae1628395de0e25f5944"),
            entry(FixedStack.PICKLED_CARROTS,
                    "2cad963ecad6876140ad3b545d222f34611f159b296069f3f608177d3cc797af"),
            entry(FixedStack.RIND_JAM,
                    "f054af52683bacfc9abd86b4fdc4cdb67dce08b8e514cf7fd09dc5da8543a4d0"),
            entry(FixedStack.GIMMARI,
                    "6a6d10d5ecf1ca2ccc3a2914b20f6c816a859c371db3e72081bc710921fa95a1"),
            entry(FixedStack.BOKGUK,
                    "1d424efa6b25858990d5259b2da6a03ff17ea3f20a762b3eba4394d8c9129507"),
            entry(FixedStack.GOLDEN_PICKLED_CARROTS,
                    "488ac0a4abafd74f364acc6d79aac51696912ceb50fffbce3b5911b6f60926ba"),
            entry(FixedStack.MELON_SORBET,
                    "ac896ede1636afedb077c28a33d72a6bf42a0fd8e0576761497049e73556921a"),
            entry(FixedStack.PUMPKIN_EMPANADA,
                    "4aa72313400ae49ffb26c257b210df4127df2018ce111d15732fc971259368c2"),
            entry(FixedStack.WARPED_PIZZA,
                    "e161f06665198d6241bec8d3be5a57627b4f87fcb125961529ce27aac9c6a589"),
            entry(FixedStack.WARPED_STROGANOFF,
                    "075e537632f1bf7c0cc18b9ca5b2d8531ef9ea14dbeda093c2411daf849058c8"),
            entry(FixedStack.SWEET_BERRY_DANISH,
                    "8440acfc8fadf6e42a72cd05d48e2ced0bb7b7e00e1b0eb6920d3d1678fbf0ef"),
            entry(FixedStack.GOLDEN_CARROT_CUPCAKE,
                    "f2e87843ef3303c285057b6fe9b7695c92d67cacc7ae553b28175e268121039f"),
            entry(FixedStack.JAPANESE_CURRY,
                    "25f04d89698e3fb2d6aeae6872660c801631f38fa12852b291d7fa50d9567925"),
            entry(FixedStack.GREEN_CURRY,
                    "7b02e440ea5bd0aa9bb7f6d03adbf8389583c7731f39f2f09bf98ef2813d90ea"),
            entry(FixedStack.TONKOTSU_RAMEN,
                    "40eee757ea734d1f720d38b222cbf6d7dae9d720be7c3655c7af319faf8c060c"),
            entry(FixedStack.BRACHIO_CHICKEN_NUGGET,
                    "3ce805798b1c705c6e313c260a2d77d95f790fd5df6328c483875752da979e34"),
            entry(FixedStack.BUNUELO,
                    "60d131f1a22e832616796723d95ad650c668c5021b17c747b8d90f7718196f8a"),
            entry(FixedStack.PTERO_CHICKEN_NUGGET,
                    "81bb848e5c4628a8ad2f0d1212aef2cdaa157f8c72aec96797e3087af1309fbb"),
            entry(FixedStack.PUERQUITO,
                    "6dc1056407d72b0e8bd01dab4ce23722aa489345eecca11ccbf4f47e5ae678d1"),
            entry(FixedStack.STEGO_CHICKEN_NUGGET,
                    "c583bd8150d5aacbb2680dc1abac0c3e6c61bf63137c9d2a8e49721ea33c188a"),
            entry(FixedStack.TRICERO_CHICKEN_NUGGET,
                    "d145928ec936206b30664a3f9e205b0785204fdc9731f91f8e5758ab5cca7860"),
            entry(FixedStack.CHEESE_PIZZA,
                    "a3e03bd71b2f79477a6a6dfda8afc2c4486316db33a74cfab7a9e6a5d197f5e2"),
            entry(FixedStack.CHOCOLATE_CHIP_COOKIE,
                    "67fdf2492f9d34ee2a65683655b2dc3f09a360ac3a0074ab8ebdab15606553a5"),
            entry(FixedStack.FRENCH_TOAST,
                    "2965b336da2090a37160672e46c8bd5b0b3380b2f85b875abd60526266311d45"),
            entry(FixedStack.LATKE,
                    "8e5192188a175954f5e427d2605f591680ba795062ebadbb110a29786414c510"),
            entry(FixedStack.MEAD,
                    "6088835f7e627b8612d5f418d0aa197df4ab0709df6b1c2015d0e2f151f02cae"),
            entry(FixedStack.MEAT_PIZZA,
                    "59bfa5c19171dd333cf6bc26bce6f964bbfad5d0f8410e1c8dfe8e57cce74278"),
            entry(FixedStack.MUSHROOM_PIZZA,
                    "3a3e16c1b81a196c376d39fb48e91f5aabd01a1c19ec9165fea0d21c84c1540f"),
            entry(FixedStack.APPLE_EMPANADA,
                    "45085c351674ebbc2c175afa9abd1dd6a84ec09d92eece24dca21491a762e30e"),
            entry(FixedStack.BROWNIE,
                    "20f6032b14c1104e9749e887d57342a9dbaadc9bc09c7d3977c8300a6268b6ea"),
            entry(FixedStack.BRUSCHETTA,
                    "82253e525f7de21a8390799fb91ac18f3392ebe3966f294907d807f3a83f254b"),
            entry(FixedStack.CARROT_CUPCAKE,
                    "3763294b1e6d0c558ebe6526e45dee1a5855aa270203ac8ab87c597c84062bdb"),
            entry(FixedStack.FISH_AND_CHIPS,
                    "8903bd001640d8daa875259865c58955d5e64c2ed0bd0ca841cb30a6fe912422"),
            entry(FixedStack.GNOCCHI,
                    "4cd2b2e24ef576d42da4221608fb8a70efe8d21b0a8d834e471b82961e8afb95"),
            entry(FixedStack.HONIED_FRENCH_TOAST,
                    "1b7498c5cee0e36827d5e370957c1e7814bdb63309aae3437477428d93afafa8"),
            entry(FixedStack.KONTOMIRE_STEW,
                    "0034796b74c90e264369c849ee31b1a267a7b1656e77e12ae650a40c2fac9b04"),
            entry(FixedStack.LUMPIA,
                    "4b8a3159d7a5e9ae6cf0740e81808601e59299511b5bf379d1cd6332fdc6b322"),
            entry(FixedStack.PAD_THAI,
                    "d7ddb03f39490efc030dfa457778185869546d1951bec47dac6afdbb3be828a3"),
            entry(FixedStack.STROGANOFF,
                    "5cd3387b3ba62127cc81c02092c667f4ba675b7674a685a8cf1c306b3330c818"),
            entry(FixedStack.ICE_CREAM,
                    "fc2ee7cb8323fc9f65e9a70fb5dab3dd10ef37783775671061aa2f5c7e1d9406"),
            entry(FixedStack.OATMEAL,
                    "2ce5007c5c12825a7a8c1ae6462aa8161cdc3b0f8c9dc40b3b02e2b4cd25d238"),
            entry(FixedStack.PANEER_MAKHANI,
                    "d72c45cf2232bbafab76517fbb0fc3f40d15bbff709e2e34bc29d26ac9d2c70b"),
            entry(FixedStack.PUMPKIN_CURRY,
                    "dc0b5a1c0840f5473b9ee9cb765416bb15be9cd748326a12a0f3c6809b0cf27d"),
            entry(FixedStack.BENZENE,
                    "bb4fb30940e4151f5be4ae7f6fb23df0a30192e4a9dd258bd50a9933a343cf6e"),
            entry(FixedStack.ESTUS_FLASK,
                    "ec7b84853cad4b17a8edb5992e33604465c12f36b2cfcb1e485307b8aa4cc56a"),
            entry(FixedStack.REACH_BLESSING,
                    "c68cd1485734e645344bab124a553ca6afdbb950e6d9f3466038a63c13f3bc13"),
            entry(FixedStack.SILK_TOUCH_BLESSING,
                    "872bb5b94ca61cbcf4d589957c19de839b9bc9907831355b4a27ea3eefa677eb"),
            entry(FixedStack.CARBON_RICH_IRON,
                    "2bb6ace178337ddf65f0af7e1de9354ac2751da3daef98da60a62903469531fa"),
            entry(FixedStack.HEPATIZON_ALLOY,
                    "eb76460c156a6bd20b103b8ed747e0c6e540ed2a1163fa5f768a5e78de02adf8"),
            entry(FixedStack.SHAKUDO_ALLOY,
                    "89b858dfdffadc865c29f9ca055869fbd70a913992c12f03a346e38a0493dc41"),
            entry(FixedStack.ELECTRUM_ALLOY,
                    "b9d815288f637d303bb5e28ea8ad5cf2d97089debfda218eceb8cb0ef0d41a3f"),
            entry(FixedStack.DIVINE_FRAGMENT,
                    "8fc469fcc5cf34b389bb3b463fe6ae20ee337267c6754e3b6619539a72e96a24"),
            entry(FixedStack.CRYSTAL_HEART,
                    "7c706ab20f974dc6811eb5cb8efe10ad96354c522e9f95526038c360385423fa")
    );

    @Test
    void everyFixedStackHasThePinnedAuditedResultContract() {
        assertEquals(54, FixedStack.values().length);
        assertEquals(EXPECTED_RESULT_CONTRACTS.keySet(),
                new HashSet<>(java.util.List.of(FixedStack.values())));
        for (FixedStack stack : FixedStack.values()) {
            String digest = stack.resultContractSha256();
            assertEquals(EXPECTED_RESULT_CONTRACTS.get(stack), digest, stack.recipeId().toString());
            assertTrue(digest.matches("[0-9a-f]{64}"), stack.recipeId().toString());
        }
        assertEquals(54, new HashSet<>(EXPECTED_RESULT_CONTRACTS.values()).size());
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
                {"components": {"minecraft:enchantment_glint_override": true, "minecraft:rarity": "rare", "minecraft:custom_data": {"version": 1}}, "count": 9, "id": "minecraft:turtle_scute"}
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
        assertEquals(Identifier.parse("matcha:recipe/crafting/crystal_heart.json"),
                MatchaStackCatalog.recipeResource(FixedStack.CRYSTAL_HEART.recipeId()));
        assertEquals("7c706ab20f974dc6811eb5cb8efe10ad96354c522e9f95526038c360385423fa",
                FixedStack.CRYSTAL_HEART.resultContractSha256());
        assertEquals(Identifier.parse("matcha:loot_table/treasure/opal.json"),
                MatchaStackCatalog.OPAL_LOOT_RESOURCE);
        assertEquals("65878efb532070bf49eb65bcf779d108bca3b9a4396e40616ca7540957f01ce6",
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
        assertEquals("966843d0aeb3860479721ef34791a631e62bc225fdaa918dd2ad90c49977e5b4",
                sha256(matchaZip), "immutable Matcha 1.12 archive identity");

        try (ZipFile zip = new ZipFile(matchaZip.toFile())) {
            for (FixedStack stack : FixedStack.values()) {
                if (stack == FixedStack.CRYSTAL_HEART || stack == FixedStack.ESTUS_FLASK) continue;
                String entryName = "data/" + stack.recipeId().getNamespace() + "/recipe/"
                        + stack.recipeId().getPath() + ".json";
                JsonElement result = readZipJson(zip, entryName).getAsJsonObject().get("result");
                assertNotNull(result, entryName + " result");
                assertEquals(stack.resultContractSha256(),
                        MatchaStackCatalog.canonicalContractSha256(result), entryName);
            }

            JsonElement opalRoot = readZipJson(zip,
                    "data/matcha/loot_table/treasure/opal.json");
            JsonElement opalEntry = opalRoot.getAsJsonObject().getAsJsonArray("pools").get(0)
                    .getAsJsonObject().getAsJsonArray("entries").get(0);
            assertEquals(MatchaStackCatalog.OPAL_ENTRY_CONTRACT_SHA256,
                    MatchaStackCatalog.canonicalContractSha256(opalEntry), "Opal loot entry");
        }

        Path crystalRecipe = Path.of(System.getProperty("user.dir"))
                .resolve("../matcha-heart-death-compat/src/main/resources/data/matcha/recipe/crafting/crystal_heart.json")
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
