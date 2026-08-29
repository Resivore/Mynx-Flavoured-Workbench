package dev.resivore.mapmarkerextension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.client.MapMarkerTargetRepository;
import dev.resivore.mapmarkerextension.compat.worldmap.XaeroWorldMapMarkerRenderer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import xaero.map.element.render.ElementRenderLocation;

final class NativeSpriteOrientationTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));
    private static final String DESERT_VILLAGE =
        "assets/minecraft/textures/map/decorations/desert_village.png";

    @Test
    void worldMapKeepsTheAcceptedUprightUvCorrectionAndDoubleScale() throws IOException {
        BufferedImage sprite = loadNativeSprite();
        assertEquals(8, sprite.getWidth());
        assertEquals(8, sprite.getHeight());
        assertTrue(isVerticallyAsymmetric(sprite));

        String worldMap = source("compat/worldmap/XaeroWorldMapMarkerRenderer.java");
        assertTrue(worldMap.contains(
            "graphics.pose().scale(optionalScale * 2.0F, optionalScale * 2.0F, 1.0F);"
        ));
        assertTrue(worldMap.contains("ImmediateRenderUtil.texturedRect("));
        assertFalse(worldMap.contains("graphics.blit("));

        int quad = worldMap.indexOf("ImmediateRenderUtil.texturedRect(");
        int left = worldMap.indexOf("-4.0F", quad);
        int top = worldMap.indexOf("-4.0F", left + 1);
        int u0 = worldMap.indexOf("sprite.getU0()", top);
        int firstV = worldMap.indexOf("sprite.getV1()", u0);
        int width = worldMap.indexOf("8.0F", firstV);
        int height = worldMap.indexOf("8.0F", width + 1);
        int u1 = worldMap.indexOf("sprite.getU1()", height);
        int secondV = worldMap.indexOf("sprite.getV0()", u1);
        assertTrue(
            quad < left && left < top && top < u0 && u0 < firstV
                && firstV < width && width < height && height < u1 && u1 < secondV
        );
    }

    @Test
    void worldMapEligibilityOrderAndDimensionFilteringRemainFrozen() throws IOException {
        XaeroWorldMapMarkerRenderer renderer = new XaeroWorldMapMarkerRenderer(
            new MapMarkerTargetRepository()
        );
        ElementRenderLocation[] locations = {
            ElementRenderLocation.UNKNOWN,
            ElementRenderLocation.IN_MINIMAP,
            ElementRenderLocation.OVER_MINIMAP,
            ElementRenderLocation.IN_WORLD,
            ElementRenderLocation.WORLD_MAP,
            ElementRenderLocation.WORLD_MAP_MENU
        };
        for (ElementRenderLocation location : locations) {
            assertEquals(
                location == ElementRenderLocation.WORLD_MAP,
                renderer.shouldRender(location, false)
            );
            assertFalse(renderer.shouldRender(location, true));
        }
        assertEquals(300, renderer.getOrder());
        assertFalse(renderer.shouldBeDimScaled());

        String source = source("compat/worldmap/XaeroWorldMapMarkerRenderer.java");
        assertTrue(source.contains("renderInfo.mapDimension == null"));
        assertTrue(source.contains(
            "target.dimensionId().equals(renderInfo.mapDimension.identifier().toString())"
        ));
    }

    @Test
    void minimapClippingScaleAndIntegrationDeduplicationRemainFrozen() throws IOException {
        String minimap = source("compat/minimap/XaeroMinimapMarkerRenderer.java");
        String integration = source("compat/XaeroIntegration.java");

        assertTrue(minimap.contains("if (outOfBounds || renderInfo.mapDimension == null"));
        assertTrue(minimap.contains(
            "graphics.pose().scale(optionalScale, optionalScale, 1.0F);"
        ));
        assertFalse(minimap.contains("optionalScale * 2.0F"));
        assertTrue(integration.contains("handler != installedMinimapHandler"));
        assertTrue(integration.contains("worldMapHandler != installedWorldMapHandler"));
        assertTrue(integration.contains("manager == installedInfoManager"));
    }

    private static boolean isVerticallyAsymmetric(BufferedImage sprite) {
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if (sprite.getRGB(x, y)
                    != sprite.getRGB(x, sprite.getHeight() - 1 - y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BufferedImage loadNativeSprite() throws IOException {
        try (InputStream input = NativeSpriteOrientationTest.class
            .getClassLoader().getResourceAsStream(DESERT_VILLAGE)) {
            assertNotNull(input, "Missing native desert-village decoration sprite");
            BufferedImage image = ImageIO.read(input);
            assertNotNull(image);
            return image;
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(PROJECT.resolve(
            "src/main/java/dev/resivore/mapmarkerextension/" + relativePath
        ));
    }
}
