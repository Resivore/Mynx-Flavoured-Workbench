package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MapMarkerArchitectureTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));

    @Test
    void xaeroConsumesOnlyTheLiveDecorationAsset() throws IOException {
        String minimap = source("compat/minimap/XaeroMinimapMarkerRenderer.java");
        String worldMap = source("compat/worldmap/XaeroWorldMapMarkerRenderer.java");
        String sprite = source("client/MapDecorationSprite.java");

        assertTrue(minimap.contains("MapDecorationSprite.get(target.decorationAssetId())"));
        assertTrue(worldMap.contains("MapDecorationSprite.get(target.decorationAssetId())"));
        assertTrue(sprite.contains("AtlasIds.MAP_DECORATIONS"));
        assertTrue(sprite.contains("Identifier.parse(decorationAssetId)"));
        assertFalse(minimap.contains("MapMarkerIdentity"));
        assertFalse(worldMap.contains("MapMarkerIdentity"));
        assertFalse(minimap.contains("poi_icons"));
        assertFalse(worldMap.contains("poi_icons"));
        assertFalse(minimap.contains("preferredSprite"));
        assertFalse(worldMap.contains("preferredSprite"));
    }

    @Test
    void filledMapItemArtworkUsesItsSeparateModelRouteWithSafeFallback()
        throws IOException {
        String client = source("MapMarkerExtensionClient.java");
        String model = source("client/MapMarkerItemModel.java");

        assertTrue(client.contains("MapMarkerItemModel.register()"));
        assertTrue(model.contains("modifyItemModelAfterBake()"));
        assertTrue(model.contains("minecraft:filled_map"));
        assertTrue(model.contains("minecraft:papal_outpost_map"));
        assertTrue(model.contains("minecraft:abbey_map"));
        assertTrue(model.contains("identity.filledMapItemAssetId()"));
        assertTrue(model.contains("textures/map/decorations/map_sprites/"));
        assertTrue(model.contains("model instanceof MissingItemModel"));
        assertTrue(model.contains("super.update("));
        assertFalse(model.contains("identity.markerAssetId()"));
        assertFalse(model.contains("MapDecorationRenderState"));
    }

    @Test
    void xaeroIsGuardedBeforeAnyVersionSpecificIntegrationIsConstructed()
        throws IOException {
        String client = source("MapMarkerExtensionClient.java");
        int guard = client.indexOf("isModLoaded(\"xaerominimap\")");
        int integration = client.indexOf("new XaeroIntegration(targets)");
        assertTrue(guard >= 0 && integration > guard);
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(PROJECT.resolve(
            "src/main/java/dev/resivore/mapmarkerextension/" + relativePath
        ));
    }
}
