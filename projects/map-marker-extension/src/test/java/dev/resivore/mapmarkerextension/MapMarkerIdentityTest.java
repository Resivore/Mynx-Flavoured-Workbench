package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class MapMarkerIdentityTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));
    private static final Path RESOURCE_PACK = Path.of(System.getProperty("resourcePackRoot"));

    @Test
    void completeIdentityTablePinsAllEighteenStableProducts() {
        List<MapMarkerIdentity> identities = List.of(MapMarkerIdentity.values());
        assertEquals(18, identities.size());
        assertEquals(18, identities.stream().map(MapMarkerIdentity::id).collect(Collectors.toSet()).size());
        assertEquals(18, identities.stream()
            .map(MapMarkerIdentity::itemNameTranslationKey)
            .collect(Collectors.toSet()).size());
        assertEquals(17, identities.stream().filter(MapMarkerIdentity::customDecoration).count());

        for (MapMarkerIdentity identity : identities) {
            assertEquals(identity, MapMarkerIdentity.fromItemNameTranslationKey(
                identity.itemNameTranslationKey()
            ).orElseThrow());
            assertEquals(identity, MapMarkerIdentity.fromDecorationTypeId(
                identity.decorationTypeId()
            ).orElseThrow());
            assertTrue(identity.filledMapItemAssetId().startsWith(
                "map_marker_extension:map_sprites/"
            ));
        }
    }

    @Test
    void formerlySharedVanillaTypesHaveDistinctNativeIdentitiesAndAssets() {
        assertSharedSourceButDistinctResult(MapMarkerIdentity.ABBEY, MapMarkerIdentity.ANCIENT_CITY);
        assertSharedSourceButDistinctResult(MapMarkerIdentity.ABBEY, MapMarkerIdentity.BURIED_MINESHAFT);
        assertSharedSourceButDistinctResult(MapMarkerIdentity.ABBEY, MapMarkerIdentity.PAPAL_OUTPOST);
        assertSharedSourceButDistinctResult(MapMarkerIdentity.ABBEY, MapMarkerIdentity.WARM_OCEAN_RUINS);
        assertSharedSourceButDistinctResult(
            MapMarkerIdentity.DESERT_PYRAMID,
            MapMarkerIdentity.DESERT_VILLAGE
        );
        assertSharedSourceButDistinctResult(
            MapMarkerIdentity.TRAIL_RUINS,
            MapMarkerIdentity.PLAINS_VILLAGE
        );
    }

    @Test
    void markerAndFilledMapArtworkRemainSeparateAndComplete() throws IOException {
        Path packNamespace = RESOURCE_PACK.resolve("assets/map_marker_extension");
        Path jarNamespace = PROJECT.resolve("src/main/resources/assets/map_marker_extension");
        for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
            Path mapSprite = packNamespace.resolve(
                "textures/map/decorations/map_sprites/" + identity.id() + ".png"
            );
            assertPng(mapSprite);
            String descriptor = Files.readString(packNamespace.resolve(
                "items/map_sprites/" + identity.id() + ".json"
            ));
            String model = Files.readString(packNamespace.resolve(
                "models/item/map_sprites/" + identity.id() + ".json"
            ));
            assertTrue(descriptor.contains(
                "\"model\": \"map_marker_extension:item/map_sprites/" + identity.id() + "\""
            ));
            assertTrue(model.contains(
                "\"layer0\": \"map_marker_extension:map/decorations/map_sprites/"
                    + identity.id() + "\""
            ));

            if (identity.customDecoration()) {
                Path packMarker = packNamespace.resolve(
                    "textures/map/decorations/poi_icons/" + identity.id() + ".png"
                );
                Path bundledMarker = jarNamespace.resolve(
                    "textures/map/decorations/poi_icons/" + identity.id() + ".png"
                );
                assertPng(packMarker);
                assertPng(bundledMarker);
                assertArrayEquals(Files.readAllBytes(packMarker), Files.readAllBytes(bundledMarker));
            }
        }

        try (Stream<Path> packPngs = Files.walk(packNamespace)) {
            assertEquals(35, packPngs.filter(path -> path.toString().endsWith(".png")).count());
        }
        try (Stream<Path> jarPngs = Files.walk(jarNamespace)) {
            assertEquals(17, jarPngs.filter(path -> path.toString().endsWith(".png")).count());
        }
    }

    @Test
    void resourcePackTargetsMinecraft26_2AndNeverOverridesVanillaMarkerTextures()
        throws IOException {
        String metadata = Files.readString(RESOURCE_PACK.resolve("pack.mcmeta"));
        assertTrue(metadata.contains("\"min_format\": [88, 0]"));
        assertTrue(metadata.contains("\"max_format\": [88, 0]"));
        assertTrue(metadata.contains("Map Marker Extension"));
        assertFalse(metadata.contains("\"pack_format\""));

        Path minecraftAssets = RESOURCE_PACK.resolve("assets/minecraft");
        if (Files.exists(minecraftAssets)) {
            try (Stream<Path> files = Files.walk(minecraftAssets)) {
                assertTrue(files.noneMatch(Files::isRegularFile));
            }
        }
    }

    private static void assertSharedSourceButDistinctResult(
        MapMarkerIdentity first,
        MapMarkerIdentity second
    ) {
        assertEquals(first.sourceDecorationTypeId(), second.sourceDecorationTypeId());
        assertFalse(first.decorationTypeId().equals(second.decorationTypeId()));
        assertFalse(first.markerAssetId().equals(second.markerAssetId()));
        assertFalse(first.filledMapItemAssetId().equals(second.filledMapItemAssetId()));
    }

    private static void assertPng(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path.toString());
        byte[] bytes = Files.readAllBytes(path);
        assertTrue(bytes.length >= 33, path.toString());
        assertEquals("89504E470D0A1A0A", java.util.HexFormat.of().withUpperCase().formatHex(
            Arrays.copyOf(bytes, 8)
        ));
        assertEquals("IHDR", new String(bytes, 12, 4, StandardCharsets.US_ASCII));
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        assertEquals(16, header.getInt(16), path.toString());
        assertEquals(16, header.getInt(20), path.toString());
        assertEquals(8, Byte.toUnsignedInt(bytes[24]), path.toString());
        assertEquals(6, Byte.toUnsignedInt(bytes[25]), path.toString());
    }
}
