package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class MapMarkerIdentityTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));
    private static final Path BUILT_JAR = Path.of(System.getProperty("builtJar"));
    private static final Path CORRECTED_PACK = Path.of(System.getProperty("correctedInventoryPack"));
    private static final Path HISTORICAL_ARTIFACTS = Path.of(System.getProperty(
        "historicalArtifactsRoot"
    ));
    private static final Path C11_JAR = HISTORICAL_ARTIFACTS.resolve(
        "artifacts/map-marker-extension-0.4.0-canary11.jar"
    );
    private static final Path C10_ICON_PACK = HISTORICAL_ARTIFACTS.resolve(
        "artifacts/map-marker-extension-icons-0.4.0-canary10.zip"
    );
    private static final Set<String> EXPECTED_CUSTOM_POI_IDS = Set.of(
        "abbey", "ancient_city", "buried_mineshaft", "desert_pyramid",
        "desert_village", "jungle_pyramid", "ocean_monument", "papal_outpost",
        "plains_village", "savannah_village", "snowy_village", "taiga_village",
        "trail_ruins", "trial_chamber", "warm_ocean_ruins", "witch_hut",
        "woodland_mansion"
    );
    private static final String ASSET_ROOT = "assets/map_marker_extension/";

    @Test
    void completeIdentityTablePinsAllEighteenStableProducts() {
        List<MapMarkerIdentity> identities = List.of(MapMarkerIdentity.values());
        assertEquals(18, identities.size());
        assertEquals(18, identities.stream().map(MapMarkerIdentity::id).collect(Collectors.toSet()).size());
        assertEquals(18, identities.stream()
            .map(MapMarkerIdentity::itemNameTranslationKey)
            .collect(Collectors.toSet()).size());
        assertEquals(17, identities.stream().filter(MapMarkerIdentity::customDecoration).count());
        assertEquals(EXPECTED_CUSTOM_POI_IDS, identities.stream()
            .filter(MapMarkerIdentity::customDecoration)
            .map(MapMarkerIdentity::id)
            .collect(Collectors.toSet()));

        for (MapMarkerIdentity identity : identities) {
            assertEquals(identity, MapMarkerIdentity.fromItemNameTranslationKey(
                identity.itemNameTranslationKey()
            ).orElseThrow());
            assertEquals(identity, MapMarkerIdentity.fromDecorationTypeId(
                identity.decorationTypeId()
            ).orElseThrow());
            assertEquals(
                "map_marker_extension:map_sprites/" + identity.id(),
                identity.filledMapItemAssetId()
            );
        }
    }

    @Test
    void c12JarIsSelfContainedAndUsesTheCorrectedRibbitsStyleItemChain()
        throws IOException {
        assertTrue(Files.isRegularFile(BUILT_JAR), BUILT_JAR.toString());
        assertTrue(Files.isRegularFile(CORRECTED_PACK), CORRECTED_PACK.toString());
        assertTrue(Files.isRegularFile(C11_JAR), C11_JAR.toString());

        try (
            ZipFile jar = new ZipFile(BUILT_JAR.toFile());
            ZipFile corrected = new ZipFile(CORRECTED_PACK.toFile());
            ZipFile c11 = new ZipFile(C11_JAR.toFile())
        ) {
            Set<String> expectedIds = Arrays.stream(MapMarkerIdentity.values())
                .map(MapMarkerIdentity::id)
                .collect(Collectors.toSet());
            assertEquals(expectedIds, namesUnder(jar, ASSET_ROOT + "items/", false, ".json"));
            assertEquals(expectedIds, namesUnder(jar, ASSET_ROOT + "items/map_sprites/", false, ".json"));
            assertEquals(expectedIds, namesUnder(jar, ASSET_ROOT + "models/item/", false, ".json"));
            assertEquals(expectedIds, namesUnder(jar, ASSET_ROOT + "textures/item/", false, ".png"));
            assertEquals(expectedIds, namesUnder(
                jar, ASSET_ROOT + "textures/map/decorations/map_sprites/", false, ".png"
            ));
            assertEquals(EXPECTED_CUSTOM_POI_IDS, namesUnder(
                jar, ASSET_ROOT + "textures/map/decorations/poi_icons/", false, ".png"
            ));

            for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
                String id = identity.id();
                String directItem = ASSET_ROOT + "items/" + id + ".json";
                String mapSpriteItem = ASSET_ROOT + "items/map_sprites/" + id + ".json";
                String model = ASSET_ROOT + "models/item/" + id + ".json";
                String inventoryTexture = ASSET_ROOT + "textures/item/" + id + ".png";
                String legacyLookupTexture = ASSET_ROOT
                    + "textures/map/decorations/map_sprites/" + id + ".png";

                assertItemDefinition(jar, directItem, id);
                assertItemDefinition(jar, mapSpriteItem, id);
                assertGeneratedModel(jar, model, id);
                assertArrayEquals(entryBytes(corrected, inventoryTexture), entryBytes(jar, inventoryTexture), id);
                assertNotNull(jar.getEntry(legacyLookupTexture), legacyLookupTexture);

                if (identity.customDecoration()) {
                    String poi = ASSET_ROOT + "textures/map/decorations/poi_icons/" + id + ".png";
                    assertArrayEquals(entryBytes(c11, poi), entryBytes(jar, poi), id);
                    assertArrayEquals(entryBytes(corrected, poi), entryBytes(jar, poi), id);
                    assertPng(jar, poi, 8, 8);
                }
            }

            assertNull(jar.getEntry(ASSET_ROOT
                + "textures/map/decorations/poi_icons/buried_treasure.png"));
            assertTrue(jar.stream().noneMatch(entry -> entry.getName().startsWith(
                ASSET_ROOT + "models/item/map_sprites/"
            )));
            assertTrue(jar.stream().noneMatch(entry -> entry.getName().contains("ribbits")));
            assertTrue(jar.stream().noneMatch(entry -> entry.getName().startsWith("assets/ribbits/")));
        }
    }

    @Test
    void c12RejectsTheC10WholeCanvasPoiRegressionAndDoesNotNeedACompanionPack()
        throws IOException {
        assertTrue(Files.isRegularFile(C10_ICON_PACK));
        assertFalse(Files.exists(PROJECT.resolve("resourcepack")));
        String build = Files.readString(PROJECT.resolve("build.gradle"));
        assertFalse(build.contains("buildMapMarkerExtensionResourcePack"));
        assertFalse(build.contains("resourcepack"));

        try (ZipFile jar = new ZipFile(BUILT_JAR.toFile()); ZipFile c10 = new ZipFile(C10_ICON_PACK.toFile())) {
            for (String id : EXPECTED_CUSTOM_POI_IDS) {
                String poi = ASSET_ROOT + "textures/map/decorations/poi_icons/" + id + ".png";
                assertPng(jar, poi, 8, 8);
                assertPng(c10, poi, 32, 32);
                assertFalse(Arrays.equals(entryBytes(jar, poi), entryBytes(c10, poi)), id);
            }
        }
    }

    private static void assertItemDefinition(ZipFile jar, String entryName, String id)
        throws IOException {
        String definition = entryText(jar, entryName);
        assertTrue(definition.contains("\"type\": \"minecraft:model\""), entryName);
        assertTrue(definition.contains(
            "\"model\": \"map_marker_extension:item/" + id + "\""
        ), entryName);
    }

    private static void assertGeneratedModel(ZipFile jar, String entryName, String id)
        throws IOException {
        String model = entryText(jar, entryName);
        assertTrue(model.contains("\"parent\": \"minecraft:item/generated\""), entryName);
        assertTrue(model.contains(
            "\"layer0\": \"map_marker_extension:item/" + id + "\""
        ), entryName);
        assertFalse(model.contains("map/decorations/map_sprites"), entryName);
    }

    private static Set<String> namesUnder(ZipFile zip, String prefix, boolean recursive, String suffix) {
        return zip.stream()
            .map(ZipEntry::getName)
            .filter(name -> name.startsWith(prefix) && name.endsWith(suffix))
            .map(name -> name.substring(prefix.length(), name.length() - suffix.length()))
            .filter(name -> recursive || !name.contains("/"))
            .collect(Collectors.toSet());
    }

    private static String entryText(ZipFile zip, String name) throws IOException {
        return new String(entryBytes(zip, name), StandardCharsets.UTF_8);
    }

    private static byte[] entryBytes(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, name);
        try (InputStream input = zip.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static void assertPng(ZipFile zip, String name, int width, int height) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        assertNotNull(entry, name);
        try (InputStream input = zip.getInputStream(entry)) {
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image, name);
            assertEquals(width, image.getWidth(), name);
            assertEquals(height, image.getHeight(), name);
        }
    }
}
