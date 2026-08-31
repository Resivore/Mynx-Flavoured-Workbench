package dev.resivore.matchavillagerestoration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class VillageResourceContractTest {
    private static final String C1_FILENAME =
            "matcha-vanilla-village-restoration-0.1.0-canary1.zip";
    private static final String C1_SHA_256 =
            "2B0D29EE6389C457B801091AA7469E6C53468944DB3EDE83760DC7D8FB49B8E4";
    private static final String BUILTIN_PREFIX = "resourcepacks/vanilla_villages/";
    private static final List<String> VILLAGE_FILES = List.of(
            "village_desert.json",
            "village_plains.json",
            "village_savanna.json",
            "village_snowy.json",
            "village_taiga.json");

    @Test
    void c2DefinitionsAreByteIdenticalToTheExactRuntimePassedC1()
            throws IOException, NoSuchAlgorithmException {
        Path c1 = projectRoot().resolve("artifacts").resolve(C1_FILENAME);
        assertEquals(C1_SHA_256, sha256(c1), "C1 predecessor identity changed");

        try (ZipFile archive = new ZipFile(c1.toFile())) {
            for (String fileName : VILLAGE_FILES) {
                String c1Entry = "data/minecraft/worldgen/structure/" + fileName;
                assertArrayEquals(
                        entryBytes(archive, c1Entry),
                        Files.readAllBytes(builtInRoot().resolve(c1Entry)),
                        fileName + " must remain byte-identical to C1");
            }
        }
    }

    @Test
    void builtInPackOwnsExactlyFiveStructureDefinitionsAndNothingElse() throws IOException {
        Set<String> expected = new TreeSet<>();
        expected.add("pack.mcmeta");
        for (String fileName : VILLAGE_FILES) {
            expected.add("data/minecraft/worldgen/structure/" + fileName);
        }

        Set<String> actual = new TreeSet<>();
        try (var paths = Files.walk(builtInRoot())) {
            paths.filter(Files::isRegularFile)
                    .map(builtInRoot()::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .forEach(actual::add);
        }
        assertEquals(expected, actual);
        assertFalse(actual.stream().anyMatch(path -> path.contains("structure_set")));
        assertFalse(actual.stream().anyMatch(path -> path.contains("template_pool")));
        assertFalse(actual.stream().anyMatch(path -> path.contains("processor")));
        assertFalse(actual.stream().anyMatch(path -> path.endsWith(".nbt")));
    }

    @Test
    void productionResourcesContainOnlyMetadataAndTheExactBuiltInPack() throws IOException {
        Path resources = projectRoot().resolve("src/main/resources");
        Set<String> actual = new TreeSet<>();
        try (var paths = Files.walk(resources)) {
            paths.filter(Files::isRegularFile)
                    .map(resources::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .forEach(actual::add);
        }

        Set<String> expected = new TreeSet<>();
        expected.add("fabric.mod.json");
        expected.add("matcha_vanilla_village_restoration.mixins.json");
        expected.add(BUILTIN_PREFIX + "pack.mcmeta");
        for (String fileName : VILLAGE_FILES) {
            expected.add(BUILTIN_PREFIX + "data/minecraft/worldgen/structure/" + fileName);
        }
        assertEquals(expected, actual);
        assertFalse(actual.stream().anyMatch(path -> path.startsWith("data/")),
                "The ordinary mod resource pack must not compete for these resources");
    }

    @Test
    void activationAndPrecedenceHooksAreFailClosed() throws IOException {
        String initializer = Files.readString(projectRoot().resolve(
                "src/main/java/dev/resivore/matchavillagerestoration/"
                        + "MatchaVanillaVillageRestoration.java"));
        assertTrue(initializer.contains("ResourceLoader.registerBuiltinPack("));
        assertTrue(initializer.contains("PackActivationType.ALWAYS_ENABLED"));
        assertTrue(initializer.contains("if (!registered)"));
        assertTrue(initializer.contains("throw new IllegalStateException("));

        String mixin = compact(Files.readString(projectRoot().resolve(
                "src/main/resources/matcha_vanilla_village_restoration.mixins.json")));
        assertTrue(mixin.contains("\"required\":true"));
        assertTrue(mixin.contains("\"mixins\":[\"MultiPackResourceManagerMixin\"]"));
        assertTrue(mixin.contains("\"defaultRequire\":1"));
    }

    private static byte[] entryBytes(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        assertNotNull(entry, () -> "Missing C1 entry " + name);
        try (InputStream input = archive.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[16 * 1024];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
    }

    private static String compact(String value) {
        return value.replaceAll("\\s+", "");
    }

    private static Path builtInRoot() {
        return projectRoot().resolve("src/main/resources/resourcepacks/vanilla_villages");
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }
}
