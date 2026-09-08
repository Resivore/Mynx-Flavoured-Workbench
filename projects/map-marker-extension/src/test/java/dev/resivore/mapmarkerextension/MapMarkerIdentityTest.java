package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerIdentity;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
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
    private static final Path C9_ICON_PACK = PROJECT.resolve(
        "artifacts/map-marker-extension-icons-0.4.0-canary9.zip"
    );
    private static final Path C10_ICON_PACK = PROJECT.resolve(
        "artifacts/map-marker-extension-icons-0.4.0-canary10.zip"
    );
    private static final Path ORIGINALS = Path.of(System.getProperty("originalsRoot"));
    private static final Set<String> EXPECTED_CUSTOM_POI_IDS = Set.of(
        "abbey",
        "ancient_city",
        "buried_mineshaft",
        "desert_pyramid",
        "desert_village",
        "jungle_pyramid",
        "ocean_monument",
        "papal_outpost",
        "plains_village",
        "savannah_village",
        "snowy_village",
        "taiga_village",
        "trail_ruins",
        "trial_chamber",
        "warm_ocean_ruins",
        "witch_hut",
        "woodland_mansion"
    );

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
            assertPng(mapSprite, 16, 16);
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
                assertPng(packMarker, 8, 8);
                assertPng(bundledMarker, 8, 8);
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
    void c11PoiIconsReframeAuthoritativeArtworkIntoTheNativeRedXCanvas()
        throws IOException {
        Path packNamespace = RESOURCE_PACK.resolve("assets/map_marker_extension");
        Path sourcePoiIcons = ORIGINALS.resolve("assets/poi_icons");
        Path packPoiIcons = packNamespace.resolve("textures/map/decorations/poi_icons");
        Path bundledPoiIcons = PROJECT.resolve(
            "src/main/resources/assets/map_marker_extension/textures/map/decorations/poi_icons"
        );
        Set<String> expectedFromIdentities = Arrays.stream(MapMarkerIdentity.values())
            .filter(MapMarkerIdentity::customDecoration)
            .map(MapMarkerIdentity::id)
            .collect(Collectors.toSet());
        assertEquals(EXPECTED_CUSTOM_POI_IDS, expectedFromIdentities);
        assertFalse(EXPECTED_CUSTOM_POI_IDS.contains("buried_treasure"));
        assertEquals(EXPECTED_CUSTOM_POI_IDS, pngBaseNames(sourcePoiIcons));
        assertEquals(EXPECTED_CUSTOM_POI_IDS, pngBaseNames(packPoiIcons));
        assertEquals(EXPECTED_CUSTOM_POI_IDS, pngBaseNames(bundledPoiIcons));

        BufferedImage redX = loadMinecraftRedX();
        Bounds redXBounds = bounds(redX);
        assertEquals(8, redX.getWidth());
        assertEquals(8, redX.getHeight());
        assertEquals(new Bounds(0, 0, 7, 7), redXBounds);

        for (String id : EXPECTED_CUSTOM_POI_IDS) {
            Path source = sourcePoiIcons.resolve(id + ".png");
            Path packOutput = packPoiIcons.resolve(id + ".png");
            Path bundledOutput = bundledPoiIcons.resolve(id + ".png");
            assertArrayEquals(Files.readAllBytes(packOutput), Files.readAllBytes(bundledOutput));

            BufferedImage original = ImageIO.read(source.toFile());
            BufferedImage output = ImageIO.read(packOutput.toFile());
            assertEquals(16, original.getWidth(), id);
            assertEquals(16, original.getHeight(), id);
            assertEquals(redX.getWidth(), output.getWidth(), id);
            assertEquals(redX.getHeight(), output.getHeight(), id);
            assertImageEquals(reframeArtwork(original, redXBounds), output, id);

            Bounds sourceBounds = bounds(original);
            Bounds outputBounds = bounds(output);
            Bounds expectedOutputBounds = bounds(reframeArtwork(original, redXBounds));
            assertEquals(expectedOutputBounds, outputBounds, id);
            assertTrue(outputBounds.width() <= redXBounds.width(), id);
            assertTrue(outputBounds.height() <= redXBounds.height(), id);
            assertTrue(hasRedXComparableNormalizedFootprint(output, redX), id);
            assertTrue(
                normalizedArea(outputBounds, output) > normalizedArea(sourceBounds, original),
                id + " must enlarge artwork inside the logical sprite canvas"
            );
        }
    }

    @Test
    void c11VisibleFootprintRejectsTheC10WholeCanvasDoublingMistake() throws IOException {
        Path sourcePoiIcons = ORIGINALS.resolve("assets/poi_icons");
        BufferedImage redX = loadMinecraftRedX();
        assertTrue(Files.isRegularFile(C10_ICON_PACK));

        try (ZipFile c10 = new ZipFile(C10_ICON_PACK.toFile())) {
            for (String id : EXPECTED_CUSTOM_POI_IDS) {
                BufferedImage original = ImageIO.read(sourcePoiIcons.resolve(id + ".png").toFile());
                String entryName = "assets/map_marker_extension/textures/map/decorations/poi_icons/"
                    + id + ".png";
                var entry = c10.getEntry(entryName);
                assertNotNull(entry, entryName);
                BufferedImage c10Image;
                try (InputStream input = c10.getInputStream(entry)) {
                    c10Image = ImageIO.read(input);
                }

                Bounds originalBounds = bounds(original);
                Bounds c10Bounds = bounds(c10Image);
                assertEquals(
                    normalizedArea(originalBounds, original),
                    normalizedArea(c10Bounds, c10Image),
                    0.000001D,
                    id + " proves C10 preserved the normalized visible footprint"
                );
                assertFalse(
                    hasRedXComparableNormalizedFootprint(c10Image, redX),
                    id + " must fail when both canvas and artwork are merely doubled"
                );
            }
        }
    }

    @Test
    void c10LeavesAllC9ItemSideMapSpritesByteIdentical()
        throws IOException {
        Path packNamespace = RESOURCE_PACK.resolve("assets/map_marker_extension");
        assertTrue(Files.isRegularFile(C9_ICON_PACK));

        try (ZipFile c9 = new ZipFile(C9_ICON_PACK.toFile())) {
            for (MapMarkerIdentity identity : MapMarkerIdentity.values()) {
                Path itemSprite = packNamespace.resolve(
                    "textures/map/decorations/map_sprites/" + identity.id() + ".png"
                );
                BufferedImage item = ImageIO.read(itemSprite.toFile());
                assertEquals(16, item.getWidth(), itemSprite.toString());
                assertEquals(16, item.getHeight(), itemSprite.toString());

                int transparent = 0;
                int visible = 0;
                int minX = 16;
                int minY = 16;
                int maxX = -1;
                int maxY = -1;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        if (((item.getRGB(x, y) >>> 24) & 0xff) == 0) {
                            transparent++;
                            continue;
                        }
                        visible++;
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }

                int width = maxX - minX + 1;
                int height = maxY - minY + 1;
                assertTrue(transparent >= 72, "C9 item art must not retain C8's opaque map canvas");
                assertTrue(visible >= 90 && visible <= 184, "C9 item art has an unexpected footprint");
                assertTrue(width >= 12 && width <= 14, "C9 item art is horizontally too small");
                assertTrue(height >= 10 && height <= 14, "C9 item art is vertically too small");
                assertTrue(Math.abs((minX + maxX) - 15) <= 1, "C9 item art must remain centered");
                assertTrue(Math.abs((minY + maxY) - 15) <= 1, "C9 item art must remain centered");
                String entryName = "assets/map_marker_extension/textures/map/decorations/map_sprites/"
                    + identity.id() + ".png";
                try (InputStream c9Sprite = c9.getInputStream(c9.getEntry(entryName))) {
                    assertArrayEquals(c9Sprite.readAllBytes(), Files.readAllBytes(itemSprite), identity.id());
                }
            }
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

    private static Set<String> pngBaseNames(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith(".png"))
                .map(name -> name.substring(0, name.length() - 4))
                .collect(Collectors.toSet());
        }
    }

    private static BufferedImage loadMinecraftRedX() throws IOException {
        try (InputStream input = MapMarkerIdentityTest.class.getClassLoader().getResourceAsStream(
            "assets/minecraft/textures/map/decorations/red_x.png"
        )) {
            assertNotNull(input, "Missing Minecraft 26.2 red-X decoration sprite");
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image, "Unable to decode Minecraft 26.2 red-X decoration sprite");
            return image;
        }
    }

    private static BufferedImage reframeArtwork(BufferedImage source, Bounds target) {
        Bounds artwork = bounds(source);
        double scale = Math.min(
            (double) target.width() / artwork.width(),
            (double) target.height() / artwork.height()
        );
        int outputWidth = Math.min(target.width(), Math.max(1, (int) Math.round(artwork.width() * scale)));
        int outputHeight = Math.min(target.height(), Math.max(1, (int) Math.round(artwork.height() * scale)));
        int offsetX = (target.width() - outputWidth) / 2;
        int offsetY = (target.height() - outputHeight) / 2;
        BufferedImage output = new BufferedImage(target.width(), target.height(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < outputHeight; y++) {
            int sourceY = artwork.minY() + Math.min(
                artwork.height() - 1,
                (int) Math.floor((y + 0.5D) * artwork.height() / outputHeight)
            );
            for (int x = 0; x < outputWidth; x++) {
                int sourceX = artwork.minX() + Math.min(
                    artwork.width() - 1,
                    (int) Math.floor((x + 0.5D) * artwork.width() / outputWidth)
                );
                output.setRGB(offsetX + x, offsetY + y, source.getRGB(sourceX, sourceY));
            }
        }
        return output;
    }

    private static Bounds bounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertTrue(maxX >= 0, "A map-decoration sprite must contain visible artwork");
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static boolean hasRedXComparableNormalizedFootprint(
        BufferedImage candidate,
        BufferedImage redX
    ) {
        Bounds candidateBounds = bounds(candidate);
        Bounds redXBounds = bounds(redX);
        return (double) candidateBounds.width() / candidate.getWidth() >= 0.75D
            && (double) candidateBounds.height() / candidate.getHeight() >= 0.75D
            && normalizedArea(candidateBounds, candidate) >= normalizedArea(redXBounds, redX) * 0.75D;
    }

    private static double normalizedArea(Bounds bounds, BufferedImage image) {
        return (double) bounds.width() * bounds.height() / (image.getWidth() * image.getHeight());
    }

    private static void assertImageEquals(BufferedImage expected, BufferedImage actual, String id) {
        assertEquals(expected.getWidth(), actual.getWidth(), id);
        assertEquals(expected.getHeight(), actual.getHeight(), id);
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), id + " pixel " + x + "," + y);
            }
        }
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {
        int width() {
            return maxX - minX + 1;
        }

        int height() {
            return maxY - minY + 1;
        }
    }

    private static void assertPng(Path path, int width, int height) throws IOException {
        assertTrue(Files.isRegularFile(path), path.toString());
        byte[] bytes = Files.readAllBytes(path);
        assertTrue(bytes.length >= 33, path.toString());
        assertEquals("89504E470D0A1A0A", java.util.HexFormat.of().withUpperCase().formatHex(
            Arrays.copyOf(bytes, 8)
        ));
        assertEquals("IHDR", new String(bytes, 12, 4, StandardCharsets.US_ASCII));
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        assertEquals(width, header.getInt(16), path.toString());
        assertEquals(height, header.getInt(20), path.toString());
        assertEquals(8, Byte.toUnsignedInt(bytes[24]), path.toString());
        assertEquals(6, Byte.toUnsignedInt(bytes[25]), path.toString());
    }
}
