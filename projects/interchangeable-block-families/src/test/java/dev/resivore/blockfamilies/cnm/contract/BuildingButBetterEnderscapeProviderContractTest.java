package dev.resivore.blockfamilies.cnm.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the exact BBB capability required by C9 without making production discover families.
 * The test reads the packaged provider only to verify the audited literal input.
 */
final class BuildingButBetterEnderscapeProviderContractTest {
    private static final String BBB_VERSION = "2.0pre4+26.2-enderscape-dev.7";
    private static final String PREDECESSOR_BBB_VERSION = "2.0pre4+26.2-pale-oak-dev.6";
    private static final List<String> MATERIALS = List.of("veiled", "celestial", "murublight");
    private static final List<String> INCLUDED_FORMS = List.of(
            "frame", "lattice", "trim", "balustrade", "support", "pallet");

    @Test
    void dev7ProviderPackageContainsAllEighteenAuditedEnderscapeItems() throws Exception {
        Set<String> expected = new LinkedHashSet<>();
        for (String material : MATERIALS) {
            for (String form : INCLUDED_FORMS) expected.add("bbb:" + material + "_" + form);
        }

        try (JarFile provider = new JarFile(requiredProviderJar().toFile())) {
            assertEquals(BBB_VERSION, fabricVersion(provider));
            assertEquals(expected, builtInPackIds(provider, "blockstates"));
            assertEquals(expected, builtInPackIds(provider, "items"));
        }
    }

    @Test
    void exactFabricDependencyRejectsTheKnownIncompatibleProviderBeforeRegistryStartup() throws Exception {
        JsonObject metadata = JsonParser.parseString(Files.readString(
                Path.of(System.getProperty("projectRoot"), "src", "main", "resources", "fabric.mod.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        String requirement = metadata.getAsJsonObject("depends").get("bbb").getAsString();
        assertEquals("=" + BBB_VERSION, requirement);

        Version installed = Version.parse(BBB_VERSION);
        Version predecessor = Version.parse(PREDECESSOR_BBB_VERSION);
        assertFalse(installed instanceof SemanticVersion,
                "The BBB provider versions are not ordered semantic versions");
        assertFalse(predecessor instanceof SemanticVersion,
                "The predecessor BBB provider version is not ordered semver either");

        VersionPredicate predicate = VersionPredicate.parse(requirement);
        assertTrue(predicate.test(installed));
        assertFalse(predicate.test(predecessor),
                "Fabric must reject dev.6 before IBF can encounter a missing audited item");
    }

    private static Path requiredProviderJar() {
        String configured = System.getProperty("bbbReferenceJar");
        assertTrue(configured != null && !configured.isBlank(),
                "Missing Gradle-wired BBB provider reference");
        Path path = Path.of(configured);
        assertTrue(Files.isRegularFile(path), "Missing BBB provider artifact " + path);
        return path;
    }

    private static String fabricVersion(JarFile jar) throws Exception {
        JarEntry entry = jar.getJarEntry("fabric.mod.json");
        assertTrue(entry != null, "BBB package lacks Fabric metadata");
        try (InputStream input = jar.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject().get("version").getAsString();
        }
    }

    private static Set<String> builtInPackIds(JarFile jar, String resourceKind) {
        String prefix = "resourcepacks/enderscape_wood_families/assets/bbb/" + resourceKind + "/";
        Set<String> actual = new LinkedHashSet<>();
        for (JarEntry entry : jar.stream().toList()) {
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(".json")) continue;
            String id = name.substring(prefix.length(), name.length() - ".json".length());
            if (MATERIALS.stream().anyMatch(material -> id.startsWith(material + "_"))) {
                actual.add("bbb:" + id);
            }
        }

        Set<String> included = new LinkedHashSet<>();
        for (String material : MATERIALS) {
            for (String form : INCLUDED_FORMS) {
                String id = "bbb:" + material + "_" + form;
                if (actual.contains(id)) included.add(id);
            }
        }
        return included;
    }
}
