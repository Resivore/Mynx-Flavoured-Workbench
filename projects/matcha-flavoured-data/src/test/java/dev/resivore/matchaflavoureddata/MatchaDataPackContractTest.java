package dev.resivore.matchaflavoureddata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

final class MatchaDataPackContractTest {
    private static final String SOURCE_FILENAME = "Matcha_Flavoured_1_12.zip";
    private static final long SOURCE_SIZE = 12_248_989L;
    private static final String SOURCE_SHA_256 =
            "6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248";
    private static final String BUILTIN_PREFIX = "resourcepacks/matcha_flavoured_1_12/";
    private static final List<String> REQUIRED_RESOURCES = List.of(
            "data/blessings/recipe/frost_walker_frost_protection.json",
            "data/food/recipe/glow_berry_crumble.json",
            "data/main/enchantment/freezing_protection.json",
            "data/main/function/environmental/check_freezing_water_conditions.mcfunction",
            "data/minecraft/worldgen/structure_set/villages.json");

    @Test
    void builtInTreeIsAnExactServerDataProjectionOfThePinnedArchive() throws Exception {
        try (ZipFile source = new ZipFile(sourceArchive().toFile())) {
            assertEquals(SOURCE_SIZE, Files.size(sourceArchive()));
            assertEquals(SOURCE_SHA_256, sha256(sourceArchive()));
            assertArrayEquals(entryBytes(source, "pack.mcmeta"), Files.readAllBytes(builtInRoot().resolve("pack.mcmeta")));

            Set<String> expected = sourceDataFiles(source);
            Set<String> actual = localDataFiles();
            assertEquals(expected, actual, "every and only original data/ file must be embedded");
            assertEquals(2433, actual.size(), "source-data file count changed unexpectedly");
            for (String relative : expected) {
                assertArrayEquals(entryBytes(source, relative), Files.readAllBytes(builtInRoot().resolve(relative)), relative);
            }
        }
    }

    @Test
    void namespacesAndConsumerContractsStayAtTheirOriginalIdentifiers() throws Exception {
        Set<String> namespaces = new TreeSet<>();
        for (String file : localDataFiles()) {
            namespaces.add(file.split("/")[1]);
        }
        assertEquals(Set.of("blasting", "blessings", "crafting", "custom_music", "debug", "endless_repairs", "food", "main", "minecraft", "smelting", "smithing_table", "smoking", "stonecutting"), namespaces);
        assertFalse(namespaces.contains("matcha_flavoured_data"));
        for (String resource : REQUIRED_RESOURCES) {
            assertTrue(Files.isRegularFile(builtInRoot().resolve(resource)), resource);
        }
        assertTrue(localDataFiles().stream().anyMatch(path -> path.startsWith("data/minecraft/")), "minecraft overrides must remain");
    }

    @Test
    void productionJarContainsNoClientAssetsAndPreservesEveryDataByte() throws Exception {
        try (ZipFile source = new ZipFile(sourceArchive().toFile()); ZipFile jar = new ZipFile(Path.of(System.getProperty("packagedJar")).toFile())) {
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith("assets/")), "the mod itself must not contain a client asset tree");
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().startsWith(BUILTIN_PREFIX + "assets/")), "the embedded pack must not contain client assets");
            for (String relative : sourceDataFiles(source)) {
                String packaged = BUILTIN_PREFIX + relative;
                ZipEntry entry = jar.getEntry(packaged);
                assertNotNull(entry, () -> "Missing packaged " + packaged);
                assertArrayEquals(entryBytes(source, relative), entryBytes(jar, packaged), packaged);
            }
            assertArrayEquals(entryBytes(source, "pack.mcmeta"), entryBytes(jar, BUILTIN_PREFIX + "pack.mcmeta"));
        }
    }

    @Test
    void legacyExactPackCoexistenceIsIdempotentForSingletonsAndTags() throws Exception {
        try (ZipFile source = new ZipFile(sourceArchive().toFile())) {
            for (String relative : sourceDataFiles(source)) {
                assertArrayEquals(entryBytes(source, relative), Files.readAllBytes(builtInRoot().resolve(relative)), relative);
            }
            for (String relative : sourceDataFiles(source)) {
                if (!relative.contains("/tags/") || !relative.endsWith(".json")) {
                    continue;
                }
                JsonObject tag = JsonParser.parseString(new String(entryBytes(source, relative))).getAsJsonObject();
                assertEquals(tagMembership(List.of(tag)), tagMembership(List.of(tag, tag)), relative + " source then embedded");
                assertEquals(tagMembership(List.of(tag)), tagMembership(List.of(tag, tag)), relative + " embedded then source");
            }
        }
    }

    @Test
    void requiredAlwaysEnabledRegistrationIsFailClosedAndDoesNotReorderOtherPacks() throws IOException {
        String initializer = Files.readString(projectRoot().resolve(
                "src/main/java/dev/resivore/matchaflavoureddata/MatchaFlavouredData.java"));
        assertTrue(initializer.contains("ResourceLoader.registerBuiltinPack("));
        assertTrue(initializer.contains("PackActivationType.ALWAYS_ENABLED"));
        assertTrue(initializer.contains("if (!registered)"));
        assertTrue(initializer.contains("throw new IllegalStateException("));
        assertFalse(initializer.contains("MultiPackResourceManager"), "this data pack must not globally reorder other pack providers");
    }

    private static Set<String> tagMembership(List<JsonObject> layers) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonObject layer : layers) {
            if (layer.has("replace") && layer.get("replace").getAsBoolean()) {
                values.clear();
            }
            JsonArray entries = layer.getAsJsonArray("values");
            if (entries != null) {
                for (JsonElement entry : entries) {
                    values.add(entry.toString());
                }
            }
        }
        return Set.copyOf(values);
    }

    private static Set<String> sourceDataFiles(ZipFile archive) {
        Set<String> files = new TreeSet<>();
        archive.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("data/"))
                .map(ZipEntry::getName).forEach(files::add);
        return files;
    }

    private static Set<String> localDataFiles() throws IOException {
        try (var paths = Files.walk(builtInRoot().resolve("data"))) {
            Set<String> files = new TreeSet<>();
            paths.filter(Files::isRegularFile).map(builtInRoot()::relativize)
                    .map(Path::toString).map(path -> path.replace('\\', '/')).forEach(files::add);
            return files;
        }
    }

    private static byte[] entryBytes(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
        assertNotNull(entry, () -> "Missing archive entry " + name);
        try (InputStream input = archive.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            input.transferTo(new java.io.OutputStream() {
                @Override public void write(int value) { digest.update((byte) value); }
                @Override public void write(byte[] bytes, int offset, int length) { digest.update(bytes, offset, length); }
            });
        }
        return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
    }

    private static Path sourceArchive() {
        return Path.of(System.getProperty("workbenchRoot"), "originals", "datapacks", SOURCE_FILENAME);
    }

    private static Path builtInRoot() {
        return projectRoot().resolve("src/main/resources").resolve(BUILTIN_PREFIX);
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }
}
