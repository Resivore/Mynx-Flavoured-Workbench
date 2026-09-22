package dev.resivore.enderscapeintegration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Contract for the three presentation-only Enderscape English name overrides. */
final class EnderscapeLocalizationContractTest {
    private static final String ENGLISH_LANG = "assets/enderscape/lang/en_us.json";
    private static final Map<String, String> OVERRIDES = Map.of(
            "block.enderscape.veiled_end_stone", "Veiled Nullium",
            "block.enderscape.celestial_overgrowth", "Celestial Nullium",
            "block.enderscape.corrupt_overgrowth", "Corrupted Nullium");
    private static final Map<String, String> UNCHANGED_PATH_NAMES = Map.of(
            "block.enderscape.celestial_path", "Celestial Path",
            "block.enderscape.corrupt_path", "Corrupt Path");

    @Test
    void exactEnglishOverridesResolveWithoutRenamingEitherPath() throws Exception {
        try (ZipFile upstreamJar = new ZipFile(enderscapeJar().toFile());
             ZipFile companionJar = new ZipFile(packagedJar().toFile())) {
            JsonObject upstream = json(upstreamJar, ENGLISH_LANG);
            JsonObject companion = json(companionJar, ENGLISH_LANG);
            JsonObject metadata = json(companionJar, "fabric.mod.json");

            assertEquals(OVERRIDES.size(), companion.size(),
                    "the companion language file must contain only the three requested overrides");
            assertEquals("enderscape", metadata.getAsJsonObject("custom")
                    .getAsJsonObject("fabric:resource_load_order")
                    .get("after").getAsString(),
                    "the companion resource pack must load after Enderscape");
            OVERRIDES.forEach((key, value) -> {
                assertEquals(value, companion.get(key).getAsString(), key);
                assertNotNull(upstream.get(key), () -> "upstream key vanished: " + key);
            });
            UNCHANGED_PATH_NAMES.forEach((key, value) -> {
                assertEquals(value, upstream.get(key).getAsString(), key);
                assertFalse(companion.has(key), () -> "path name must remain upstream-owned: " + key);
            });

            JsonObject effectiveEnglish = upstream.deepCopy();
            companion.entrySet().forEach(entry -> effectiveEnglish.add(entry.getKey(), entry.getValue()));
            OVERRIDES.forEach((key, value) -> assertEquals(value, effectiveEnglish.get(key).getAsString(), key));
            UNCHANGED_PATH_NAMES.forEach((key, value) ->
                    assertEquals(value, effectiveEnglish.get(key).getAsString(), key));
        }
    }

    @Test
    void companionRedistributesNoOtherEnderscapeAssetsOrClasses() throws Exception {
        try (ZipFile companionJar = new ZipFile(packagedJar().toFile())) {
            Set<String> enderscapeAssets = new LinkedHashSet<>();
            companionJar.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("assets/enderscape/") && !name.endsWith("/"))
                    .forEach(enderscapeAssets::add);
            assertEquals(Set.of(ENGLISH_LANG), enderscapeAssets);
            assertEquals(1L, companionJar.stream()
                    .filter(entry -> ENGLISH_LANG.equals(entry.getName()))
                    .count());
            assertFalse(companionJar.stream().map(ZipEntry::getName)
                    .anyMatch(name -> name.startsWith("net/penumbra/enderscape/")),
                    "upstream Enderscape classes must not be redistributed");
            assertFalse(companionJar.stream().map(ZipEntry::getName)
                    .anyMatch(name -> name.contains("enderscape-fabric-3.0.2")),
                    "the upstream Enderscape jar must not be nested or repackaged");
        }
    }

    private static JsonObject json(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, () -> "Missing " + name);
        try (InputStream input = zip.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.getAsJsonObject();
        }
    }

    private static Path packagedJar() {
        return Path.of(System.getProperty("packagedJar")).toAbsolutePath().normalize();
    }

    private static Path enderscapeJar() {
        return Path.of(System.getProperty("workbenchRoot"),
                "originals/mods/enderscape-fabric-3.0.2+mc26.2.jar");
    }
}
