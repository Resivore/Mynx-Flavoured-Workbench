package dev.resivore.wearablelanterns;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** Binds the provider registry/model audit to the exact locally supplied current artifacts. */
class Canary6ProviderArtifactAuditTest {
    private record Provider(
            String property,
            String sha256,
            String modId,
            String version,
            Map<String, String> inventoryModels) {}

    private static final Provider ENDERSCAPE = new Provider(
            "enderscapeJar",
            "9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b",
            "enderscape",
            "3.0.2",
            Map.of(
                    "void_lantern", "enderscape:item/void_lantern",
                    "bulb_lantern", "enderscape:item/bulb_lantern"));
    private static final Provider RIBBITS = new Provider(
            "ribbitsJar",
            "088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd",
            "ribbits",
            "4.1.6+26.2-mynx-canary27",
            Map.of("swamp_lantern", "ribbits:item/swamp_lantern"));
    private static final Provider AURORAS_LANTERNS = new Provider(
            "aurorasLanternsJar",
            "e0f8fe41c5ada5746de8db256eca8d2d6854c08b35fb6c83fad0ef3d66158a8a",
            "auroraslanterns",
            "2.1.1+26.2",
            Map.of(
                    "amethyst_lantern", "auroraslanterns:item/amethyst_lantern",
                    "redstone_lantern", "auroraslanterns:item/redstone_lantern"));

    @Test
    void exactProviderArtifactsExposeTheAuditedRealInventoryModels() throws Exception {
        for (Provider provider : new Provider[] {ENDERSCAPE, RIBBITS, AURORAS_LANTERNS}) {
            String configured = System.getProperty(provider.property());
            assumeTrue(configured != null && !configured.isBlank(),
                    () -> "Optional provider audit input not configured: " + provider.property());
            Path archive = Path.of(configured).toAbsolutePath().normalize();
            assertTrue(Files.isRegularFile(archive), "Missing provider artifact: " + archive);
            assertEquals(provider.sha256(), sha256(archive),
                    "Provider bytes differ from the audited artifact: " + archive);

            try (ZipFile zip = new ZipFile(archive.toFile())) {
                JsonObject metadata = json(zip, "fabric.mod.json");
                assertEquals(provider.modId(), metadata.get("id").getAsString());
                assertEquals(provider.version(), metadata.get("version").getAsString());
                for (var model : provider.inventoryModels().entrySet()) {
                    assertInventoryModel(zip, provider.modId(), model.getKey(), model.getValue());
                }
            }
        }
    }

    private static void assertInventoryModel(
            ZipFile zip, String namespace, String itemPath, String expectedModel) throws IOException {
        JsonObject definition = json(zip, "assets/" + namespace + "/items/" + itemPath + ".json")
                .getAsJsonObject("model");
        assertEquals("minecraft:model", definition.get("type").getAsString());
        assertEquals(expectedModel, definition.get("model").getAsString());

        JsonObject model = json(zip,
                "assets/" + namespace + "/models/item/" + itemPath + ".json");
        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals(namespace + ":item/" + itemPath,
                model.getAsJsonObject("textures").get("layer0").getAsString());
        assertNotNull(zip.getEntry("assets/" + namespace + "/textures/item/" + itemPath + ".png"),
                "Provider inventory texture is missing for " + namespace + ":" + itemPath);
    }

    private static JsonObject json(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        assertNotNull(entry, "Missing provider archive entry: " + path);
        try (var reader = new InputStreamReader(zip.getInputStream(entry), UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
