package dev.resivore.macawsmoreculling;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProviderAndScopeContractTest {
    private static final String MACAWS_SHA256 =
            "6411cb0ff6c6cc4312deed48c3ca69cdbcbae72e80ff4ed0269f2d0a78acd32d";
    private static final String MORECULLING_SHA256 =
            "ea04505496e4d35a8c94199884b6fafa69057efe50f2096d2988c11163d49122";
    private static final String IMPLEMENTATION =
            "src/main/java/dev/resivore/macawsmoreculling/MacawsTrapdoorsMoreCullingCompat.java";

    private static final Set<String> EXPECTED_IDS = Set.of(
            "mcwtrpdoors:acacia_bark_trapdoor",
            "mcwtrpdoors:acacia_ranch_trapdoor",
            "mcwtrpdoors:bamboo_bark_trapdoor",
            "mcwtrpdoors:birch_bark_trapdoor",
            "mcwtrpdoors:birch_ranch_trapdoor",
            "mcwtrpdoors:cherry_bark_trapdoor",
            "mcwtrpdoors:cherry_ranch_trapdoor",
            "mcwtrpdoors:crimson_bark_trapdoor",
            "mcwtrpdoors:crimson_ranch_trapdoor",
            "mcwtrpdoors:dark_oak_bark_trapdoor",
            "mcwtrpdoors:dark_oak_ranch_trapdoor",
            "mcwtrpdoors:jungle_bark_trapdoor",
            "mcwtrpdoors:jungle_ranch_trapdoor",
            "mcwtrpdoors:mangrove_bark_trapdoor",
            "mcwtrpdoors:mangrove_ranch_trapdoor",
            "mcwtrpdoors:oak_bark_trapdoor",
            "mcwtrpdoors:oak_ranch_trapdoor",
            "mcwtrpdoors:pale_oak_bark_trapdoor",
            "mcwtrpdoors:pale_oak_ranch_trapdoor",
            "mcwtrpdoors:spruce_bark_trapdoor",
            "mcwtrpdoors:spruce_ranch_trapdoor",
            "mcwtrpdoors:warped_bark_trapdoor",
            "mcwtrpdoors:warped_ranch_trapdoor"
    );

    @Test
    void exactProviderArtifactsAndMetadataArePinned() throws Exception {
        Path macaws = requiredPath("macawsTrapdoorsReferenceJar");
        Path moreCulling = requiredPath("moreCullingReferenceJar");
        assertEquals(MACAWS_SHA256, sha256(macaws));
        assertEquals(MORECULLING_SHA256, sha256(moreCulling));

        String macawsMetadata = zipText(macaws, "fabric.mod.json");
        assertTrue(macawsMetadata.contains("\"id\": \"mcwtrpdoors\""));
        assertTrue(macawsMetadata.contains("\"version\": \"1.1.5\""));

        String moreCullingMetadata = zipText(moreCulling, "fabric.mod.json");
        assertTrue(moreCullingMetadata.contains("\"id\": \"moreculling\""));
        assertTrue(moreCullingMetadata.contains("\"version\": \"1.8.1\""));
    }

    @Test
    void literalPatchSetExactlyMatchesRegisteredRanchAndBarkProviders() throws Exception {
        Path macaws = requiredPath("macawsTrapdoorsReferenceJar");
        Set<String> sourceIds = implementationIds();
        Set<String> providerModelIds = ranchAndBarkBlockstateIds(macaws);
        Set<String> registeredIds = registeredTrapdoorIds(macaws);

        assertEquals(23, EXPECTED_IDS.size());
        assertEquals(EXPECTED_IDS, sourceIds, "implementation literal set drifted");
        assertEquals(EXPECTED_IDS, providerModelIds,
                "provider ranch/bark blockstate inventory changed");
        assertTrue(registeredIds.containsAll(EXPECTED_IDS),
                "every patched ID must be a literal BlockInit registration");
        assertTrue(EXPECTED_IDS.stream().allMatch(id ->
                        id.endsWith("_ranch_trapdoor") || id.endsWith("_bark_trapdoor")),
                "the patch may contain only ranch and bark trapdoors");
        assertFalse(EXPECTED_IDS.contains("mcwtrpdoors:bamboo_ranch_trapdoor"),
                "Macaw 1.1.5 does not register a Bamboo ranch trapdoor");
    }

    @Test
    void implementationUsesOnlyTheSupportedPerBlockApi() throws IOException {
        String source = Files.readString(projectRoot().resolve(IMPLEMENTATION), StandardCharsets.UTF_8);
        assertTrue(source.contains("culling.moreculling$setCanCull(false)"));
        assertTrue(source.contains("culling.moreculling$canCull()"));
        assertTrue(source.contains("ClientTickEvents.START_CLIENT_TICK.register"),
                "the override must run after MoreCulling initializes its block compatibility flags");
        assertFalse(source.contains("endsWith("), "runtime suffix discovery is forbidden");
        assertFalse(source.contains("BuiltInRegistries.BLOCK.forEach"), "runtime registry sweeps are forbidden");
        assertFalse(source.contains("StateCullingShapeCache"), "model culling-shape mutation is unnecessary");
    }

    @Test
    void controlledMoreCullingProviderExposesTheRequiredApiContract() throws Exception {
        Path moreCulling = requiredPath("moreCullingReferenceJar");
        Set<String> methods = classMethods(moreCulling,
                "ca/fxco/moreculling/api/block/MoreBlockCulling.class");
        assertTrue(methods.contains("moreculling$canCull()Z"));
        assertTrue(methods.contains("moreculling$setCanCull(Z)V"));

        Set<String> initializerMethods = classMethods(moreCulling,
                "ca/fxco/moreculling/mixin/blockstates/Minecraft_loadBlocksMixin.class");
        assertTrue(initializerMethods.stream().anyMatch(method -> method.startsWith("moreculling$onInit(")),
                "controlled provider must retain the initialization pass that the first-tick override follows");
    }

    @Test
    void ibfAlreadyContainsTheSameLiteralIdentitiesAndNeedsNoCatalogChange() throws IOException {
        String ibfCatalog = Files.readString(workbenchRoot().resolve(
                "projects/interchangeable-block-families/src/main/java/"
                        + "dev/resivore/blockfamilies/cnm/catalog/AuditedShapeFamilies.java"));
        for (String id : EXPECTED_IDS) {
            String path = id.substring("mcwtrpdoors:".length());
            assertTrue(ibfCatalog.contains("mcwTrapdoors(\"" + path + "\")"),
                    () -> "IBF is missing existing literal identity " + id);
        }
    }

    private static Set<String> implementationIds() throws IOException {
        String source = Files.readString(projectRoot().resolve(IMPLEMENTATION), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"(mcwtrpdoors:[a-z0-9_]+_trapdoor)\"").matcher(source);
        Set<String> ids = new HashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return Set.copyOf(ids);
    }

    private static Set<String> ranchAndBarkBlockstateIds(Path jar) throws IOException {
        Set<String> ids = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            zip.stream().map(ZipEntry::getName)
                    .filter(name -> name.startsWith("assets/mcwtrpdoors/blockstates/"))
                    .filter(name -> name.endsWith("_ranch_trapdoor.json")
                            || name.endsWith("_bark_trapdoor.json"))
                    .map(name -> name.substring("assets/mcwtrpdoors/blockstates/".length(),
                            name.length() - ".json".length()))
                    .map(path -> "mcwtrpdoors:" + path)
                    .forEach(ids::add);
        }
        return Set.copyOf(ids);
    }

    private static Set<String> registeredTrapdoorIds(Path jar) throws IOException {
        Set<String> ids = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("net/kikoz/mcwtrpdoors/init/BlockInit.class");
            assertNotNull(entry, "audited Macaw provider is missing BlockInit.class");
            try (InputStream input = zip.getInputStream(entry)) {
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                if (value instanceof String text && text.endsWith("_trapdoor")) {
                                    ids.add("mcwtrpdoors:" + text);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        return Set.copyOf(ids);
    }

    private static Set<String> classMethods(Path jar, String classEntry) throws IOException {
        Set<String> methods = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(classEntry);
            assertNotNull(entry, () -> "provider is missing " + classEntry);
            try (InputStream input = zip.getInputStream(entry)) {
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        methods.add(name + Type.getMethodType(descriptor).getDescriptor());
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        return Set.copyOf(methods);
    }

    private static String zipText(Path jar, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            assertNotNull(entry, () -> jar + " is missing " + entryName);
            try (InputStream input = zip.getInputStream(entry)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) >= 0;) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Path requiredPath(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, () -> "Gradle must provide " + property);
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static Path projectRoot() {
        return requiredPath("projectRoot");
    }

    private static Path workbenchRoot() {
        return requiredPath("workbenchRoot");
    }
}
