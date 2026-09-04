package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

class UpstreamBinaryContractTest {
    private static final String XAERO_SHA =
            "69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048";
    private static final String GECKOLIB_SHA =
            "4bf1c86b4b47aa2c5d84208255695f10d79d609b23d802c995711e64b45cfce0";
    private static final String XAEROLIB_SHA =
            "7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065";
    private static final String RIBBITS_SHA =
            "6b18658c5a68d66623b9a388cc644e2f7a1b864e490b6f8b35d57fcd73a5bf74";
    private static final String TRINKETS_SHA =
            "4c1fa6ac36c0457483fd0d395b99bbd94c9334aad6defece7633bbf0552d1724";
    private static final String ACCEPTED_C3_FULL_SHA =
            "4f34d743f5fffd8e938c8f5157c630fd85f3b263ac1ae9f96c432cfe51668df2";
    private static final String MODEL_MANIFEST_SHA =
            "0d41371da10e5328a803ed960914de54b6bf7f28e19133e2b0bc5305cfa0060c";

    private static final Path XAERO = requiredPropertyPath("xaeroJar");
    private static final Path XAEROLIB = requiredPropertyPath("xaeroLibJar");
    private static final Path WORLD_MAP = requiredPropertyPath("worldMapJar");
    private static final Path GECKOLIB = requiredPropertyPath("geckolibJar", "geckoLibJar");
    private static final Path ACCEPTED_C3 = requiredPropertyPath("acceptedC3Jar");
    private static final Path EMF = requiredPropertyPath("emfJar");
    private static final Path ETF = requiredPropertyPath("etfJar");
    private static final Path FABRIC_API = requiredPropertyPath("fabricApiJar");
    private static final Path FRESH_ANIMATIONS = requiredPropertyPath("freshAnimationsPack");
    private static final Path TRINKETS = requiredPropertyPath("trinketsJar");
    private static final String DISTINCT_WANDERING_MODEL =
            "assets/ribbits/geckolib/models/wandering_ribbit.geo.json";

    @Test
    void exactPublicAndAcceptedBinaryIdentitiesRemainPinned() throws Exception {
        assertArtifact(XAERO, 2_221_925L, XAERO_SHA);
        assertArtifact(XAEROLIB, 621_485L, XAEROLIB_SHA);
        assertArtifact(WORLD_MAP, 1_473_719L,
                "d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389");
        assertArtifact(GECKOLIB, 703_096L, GECKOLIB_SHA);
        assertArtifact(ACCEPTED_C3, 28_351L, ACCEPTED_C3_FULL_SHA);
        assertArtifact(EMF, 587_342L,
                "876a3e4ffda021a6266df87208f2d9980322cf86223d4fe1e313ca996631f115");
        assertArtifact(ETF, 762_131L,
                "f469bc914302a13a5c767296623df60fb0cc3d4e4a02a77c56541a733ad36e3a");
        assertArtifact(FABRIC_API, 2_533_297L,
                "acb7dc90a0430519c49548074d3fbf6fd81d13063f08f0af344b2a6b08a42620");
        assertArtifact(FRESH_ANIMATIONS, 645_816L,
                "cf9f17a2977e171b33cb0b598bc4357dd0383e09c10d5f768ff17c12d0a028ee");
        assertArtifact(TRINKETS, 560_208L, TRINKETS_SHA);
    }

    @Test
    void exactXaeroCreatorStillContainsBothTheUnsupportedRendererBoundaryAndHookSeam()
            throws Exception {
        ClassNode creator = readClass(XAERO,
                "xaero/hud/minimap/radar/icon/creator/RadarIconCreator.class");
        List<MethodInsnNode> calls = methodCalls(creator);

        assertEquals(1L, calls.stream()
                .filter(call -> call.owner.endsWith("/EntityRenderTracer")
                        && call.name.equals("getEntityRendererModel"))
                .count());
        assertEquals(1L, calls.stream()
                .filter(call -> call.owner.endsWith("/RadarIconForm")
                        && call.name.equals("getPrerenderer"))
                .count());
    }

    @Test
    void exactGeckoLibRendererRemainsOutsideTheVanillaLivingRendererHierarchy()
            throws Exception {
        ClassNode renderer = readClass(GECKOLIB, "com/geckolib/renderer/GeoEntityRenderer.class");
        ClassNode model = readClass(GECKOLIB, "com/geckolib/model/GeoModel.class");

        assertEquals("net/minecraft/client/renderer/entity/EntityRenderer", renderer.superName);
        assertEquals("java/lang/Object", model.superName);
    }

    @Test
    void acceptedEmfCanaryThreeIdentityAndScopeRemainUntouched() throws Exception {
        try (ZipFile zip = new ZipFile(ACCEPTED_C3.toFile())) {
            List<byte[]> classes = zip.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                    .map(entry -> readUnchecked(zip, entry))
                    .toList();
            byte[] joined = join(classes);
            String constantPoolText = new String(joined, StandardCharsets.ISO_8859_1);

            assertTrue(constantPoolText.contains("EMFModelPartRoot"));
            assertTrue(constantPoolText.contains("net/minecraft/client/model/geom/ModelPart"));
            assertFalse(constantPoolText.toLowerCase(java.util.Locale.ROOT).contains("geckolib"));
            assertFalse(constantPoolText.contains("GeoEntityRenderer"));
            assertFalse(constantPoolText.contains("EntityRenderTracer"));
            assertFalse(constantPoolText.contains("RadarIconCreator"));
        }
    }

    @Test
    void exactPrivateRibbitsArchiveRetainsTheAuditedUniversalMainBodyContract()
            throws Exception {
        Optional<Path> supplied = optionalPropertyPath("ribbitsJar");
        Assumptions.assumeTrue(supplied.isPresent(),
                "set -DribbitsJar to the exact ignored private C7 archive");
        Path ribbits = supplied.orElseThrow();
        assertArtifact(ribbits, 3_320_708L, RIBBITS_SHA);

        ClassNode renderer = readClass(ribbits,
                "com/yungnickyoung/minecraft/ribbits/client/render/RibbitRenderer.class");
        ClassNode model = readClass(ribbits,
                "com/yungnickyoung/minecraft/ribbits/client/model/RibbitModel.class");
        assertEquals("com/geckolib/renderer/GeoEntityRenderer", renderer.superName);
        assertEquals("com/geckolib/model/GeoModel", model.superName);

        Map<Integer, Integer> directCubeDistribution = new TreeMap<>();
        MessageDigest manifest = MessageDigest.getInstance("SHA-256");
        int modelCount = 0;
        int headCount = 0;
        int directCubeCount = 0;

        try (ZipFile zip = new ZipFile(ribbits.toFile())) {
            List<? extends ZipEntry> allGeoEntries = zip.stream()
                    .filter(entry -> entry.getName().startsWith("assets/ribbits/")
                            && entry.getName().endsWith(".geo.json"))
                    .sorted(java.util.Comparator.comparing(ZipEntry::getName))
                    .toList();
            assertEquals(42, allGeoEntries.size());
            assertTrue(allGeoEntries.stream()
                    .anyMatch(entry -> entry.getName().equals(DISTINCT_WANDERING_MODEL)));
            List<? extends ZipEntry> entries = allGeoEntries.stream()
                    .filter(entry -> !entry.getName().equals(DISTINCT_WANDERING_MODEL))
                    .toList();
            for (ZipEntry entry : entries) {
                byte[] bytes = read(zip, entry);
                manifest.update((entry.getName() + "\t" + sha256(bytes) + "\n")
                        .getBytes(StandardCharsets.UTF_8));
                JsonObject document = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                JsonArray geometries = document.getAsJsonArray("minecraft:geometry");
                assertNotNull(geometries, entry.getName());
                assertFalse(geometries.isEmpty(), entry.getName());

                for (JsonElement geometryElement : geometries) {
                    JsonArray bones = geometryElement.getAsJsonObject().getAsJsonArray("bones");
                    assertNotNull(bones, entry.getName());
                    List<JsonObject> mainBones = namedBones(bones, "main");
                    List<JsonObject> bodyBones = namedBones(bones, "body");
                    assertEquals(1, mainBones.size(), entry.getName() + " main count");
                    assertEquals(1, bodyBones.size(), entry.getName() + " body count");
                    assertFalse(mainBones.getFirst().has("parent"), entry.getName() + " main must be top-level");
                    assertEquals("main", bodyBones.getFirst().get("parent").getAsString(),
                            entry.getName() + " body parent");

                    JsonArray cubes = bodyBones.getFirst().getAsJsonArray("cubes");
                    assertNotNull(cubes, entry.getName());
                    assertFalse(cubes.isEmpty(), entry.getName());
                    directCubeDistribution.merge(cubes.size(), 1, Integer::sum);
                    directCubeCount += cubes.size();
                    headCount += namedBonesCaseInsensitive(bones, "head").size();
                    modelCount++;
                }
            }
        }

        assertEquals(41, modelCount);
        assertEquals(156, directCubeCount);
        assertEquals(0, headCount);
        assertEquals(Map.of(3, 29, 4, 8, 9, 3, 10, 1), directCubeDistribution);
        assertEquals(MODEL_MANIFEST_SHA, HexFormat.of().formatHex(manifest.digest()));
    }

    private static List<JsonObject> namedBones(JsonArray bones, String name) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("name") && bone.get("name").getAsString().equals(name)) {
                result.add(bone);
            }
        }
        return result;
    }

    private static List<JsonObject> namedBonesCaseInsensitive(JsonArray bones, String name) {
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : bones) {
            JsonObject bone = element.getAsJsonObject();
            if (bone.has("name") && bone.get("name").getAsString().equalsIgnoreCase(name)) {
                result.add(bone);
            }
        }
        return result;
    }

    private static List<MethodInsnNode> methodCalls(ClassNode owner) {
        List<MethodInsnNode> result = new ArrayList<>();
        owner.methods.forEach(method -> {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    result.add(call);
                }
            }
        });
        return result;
    }

    private static ClassNode readClass(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = Objects.requireNonNull(zip.getEntry(entryName), entryName);
            try (InputStream stream = zip.getInputStream(entry)) {
                ClassNode result = new ClassNode();
                new ClassReader(stream).accept(result, 0);
                return result;
            }
        }
    }

    private static void assertArtifact(Path path, long size, String hash) throws Exception {
        assertEquals(size, Files.size(path), path.toString());
        assertEquals(hash, sha256(path), path.toString());
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream stream = zip.getInputStream(entry)) {
            return stream.readAllBytes();
        }
    }

    private static byte[] readUnchecked(ZipFile zip, ZipEntry entry) {
        try {
            return read(zip, entry);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static byte[] join(List<byte[]> parts) {
        int length = parts.stream().mapToInt(part -> part.length).sum();
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static Path requiredPropertyPath(String... names) {
        return optionalPropertyPath(names).orElseThrow(() ->
                new IllegalStateException("missing system property: " + String.join(" or ", names)));
    }

    private static Optional<Path> optionalPropertyPath(String... names) {
        for (String name : names) {
            String value = System.getProperty(name);
            if (value != null && !value.isBlank()) {
                return Optional.of(Path.of(value));
            }
        }
        return Optional.empty();
    }
}
