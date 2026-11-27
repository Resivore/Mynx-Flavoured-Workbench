package dev.resivore.villagerwork;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.resivore.villagerwork.client.RibbitsRodContract;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RibbitsAssetContractTest {
    private static final Path RIBBITS = Path.of("../ribbits-26.2");
    private static final String C27 = "ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar";
    private static final String C28 = "ribbits-private-reconstruction-4.1.6+26.2-matcha125-menu-canary28.jar";

    private record Assets(byte[] model, byte[] texture, String version) {}

    private Assets read(String filename, String expectedHash) throws Exception {
        Path artifact = RIBBITS.resolve("artifacts").resolve(filename);
        assertEquals(expectedHash, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(artifact))), filename + " must be exact canonical bytes");
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            return new Assets(zip.getInputStream(zip.getEntry(RibbitsRodContract.MODEL_PATH)).readAllBytes(),
                    zip.getInputStream(zip.getEntry(RibbitsRodContract.TEXTURE_PATH)).readAllBytes(),
                    JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("fabric.mod.json"))
                            .readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("version").getAsString());
        }
    }

    private Assets baseline() throws Exception {
        return read(C27, "088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd");
    }

    @Test void exactC27C28AndCurrentCanonicalArtifactSatisfyContract() throws Exception {
        JsonObject current = JsonParser.parseString(Files.readString(RIBBITS.resolve("WORKBENCH_STATUS.json")))
                .getAsJsonObject().getAsJsonObject("state").getAsJsonObject("releases").getAsJsonObject("current")
                .getAsJsonObject("artifact");
        for (Assets assets : List.of(baseline(),
                read(C28, "9e462df87c704542fa0f85d97d16857823c59b4192f2b03a4c333f3af3b91236"),
                read(current.get("filename").getAsString(), current.get("sha256").getAsString()))) {
            assertTrue(RibbitsRodContract.check(assets.model(), assets.texture()).compatible(), assets.version());
        }
    }

    @Test void fabricResolutionAndAssetContractIgnoreVersionOnlyChanges() throws Exception {
        Assets assets = baseline();
        JsonObject metadata = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json")))
                .getAsJsonObject();
        var predicate = VersionPredicate.parse(metadata.getAsJsonObject("depends").get("ribbits").getAsString());
        for (String version : List.of(assets.version(), "4.1.6+26.2-matcha125-menu-canary28",
                "4.1.6+26.2-future-canary999", "5.0.0", "future-provider")) {
            // Simulate a provider metadata-only edit while passing the very same actual assets.
            assertTrue(predicate.test(Version.parse(version)), version);
            assertTrue(RibbitsRodContract.check(assets.model(), assets.texture()).compatible(), version);
        }
    }

    @Test void formattingAndSuppressedBodyCubesDoNotInvalidateContract() throws Exception {
        Assets assets = baseline();
        JsonObject model = JsonParser.parseString(new String(assets.model(), StandardCharsets.UTF_8)).getAsJsonObject();
        bone(model, "body").remove("cubes");
        byte[] changed = new GsonBuilder().setPrettyPrinting().create().toJson(model).getBytes(StandardCharsets.UTF_8);
        assertTrue(RibbitsRodContract.check(changed, assets.texture()).compatible());
    }

    @Test void incompatibleBoneHierarchyGeometryUvTransformsAndUnknownBonesFailClosed() throws Exception {
        Assets assets = baseline();
        for (String mutation : List.of("rename", "parent", "pivot", "rotation", "cubes", "uv", "extra", "texture_size")) {
            JsonObject model = JsonParser.parseString(new String(assets.model(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject rod = bone(model, "fishing_rod");
            switch (mutation) {
                case "rename" -> rod.addProperty("name", "new_rod");
                case "parent" -> rod.addProperty("parent", "body");
                case "pivot", "rotation" -> rod.add(mutation, JsonParser.parseString("[1,2,3]"));
                case "cubes" -> rod.remove("cubes");
                case "uv" -> rod.getAsJsonArray("cubes").get(0).getAsJsonObject().add("uv", JsonParser.parseString("[1,1]"));
                case "extra" -> model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                        .getAsJsonArray("bones").add(JsonParser.parseString("{\"name\":\"unexpected\",\"pivot\":[0,0,0]}"));
                case "texture_size" -> model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                        .getAsJsonObject("description").addProperty("texture_width", 256);
            }
            var result = RibbitsRodContract.check(model.toString().getBytes(StandardCharsets.UTF_8), assets.texture());
            assertFalse(result.compatible(), mutation);
            assertTrue(result.detail().contains("geometry contract v1 changed"), result.detail());
        }
    }

    @Test void missingMalformedAndChangedTextureFailWithActionableReasonThenRecover() throws Exception {
        Assets assets = baseline();
        assertFalse(RibbitsRodContract.check(null, assets.texture()).compatible());
        assertFalse(RibbitsRodContract.check(assets.model(), null).compatible());
        assertTrue(RibbitsRodContract.check(new byte[]{'{'}, assets.texture()).detail().contains("malformed"));
        byte[] changed = assets.texture().clone();
        changed[changed.length - 1] ^= 1;
        assertTrue(RibbitsRodContract.check(assets.model(), changed).detail().contains("verify rod UV artwork"));
        assertTrue(RibbitsRodContract.check(assets.model(), assets.texture()).compatible());
    }

    private JsonObject bone(JsonObject model, String name) {
        for (var element : model.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            if (element.getAsJsonObject().get("name").getAsString().equals(name)) return element.getAsJsonObject();
        }
        throw new AssertionError(name);
    }
}
