package dev.resivore.quickstacknearbycompat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnInventoryButtonPresentationTest {
    @Test
    void iconButtonKeepsTheUpstreamActionsButUsesTheSharedEighteenPixelBounds() throws Exception {
        String source = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackIconButtonMixin.java");

        assertTrue(source.contains("QuickStackIconButton.class"));
        assertTrue(source.contains("Button.OnPress secondaryOnPress"));
        assertTrue(source.contains("setSize(18, 18)"));
        assertFalse(source.contains("setTooltip("));
        assertFalse(source.contains("onPress("));
    }

    @Test
    void iconRendererUsesMinecraftsDefaultButtonSpriteAndOnlyTheSuppliedCrispGlyph() throws Exception {
        String source = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackCustomButtonChromeMixin.java");

        assertTrue(source.contains("extractDefaultSprite(graphics)"));
        assertTrue(source.contains("36x36 (2x GUI-scale) user reference"));
        assertTrue(source.contains("0xFFFFFFFF"));
        assertTrue(source.contains("0xFF3F3F3F"));
        assertTrue(source.contains("callback.cancel()"));
        assertFalse(source.contains("blit("));
    }

    @Test
    void survivalPlacementIsGuardedAndTheClientMixinsArePackaged() throws Exception {
        String placement = read("src/main/java/dev/resivore/quickstacknearbycompat/mixin/client/QuickStackButtonSlotBridgeMixin.java");
        String mixins = read("src/main/resources/quick_stack_nearby_compat.client.mixins.json");

        assertTrue(placement.contains("screen instanceof InventoryScreen"));
        assertTrue(placement.contains("firstBottomUpFreePosition"));
        assertTrue(placement.contains("Screens.getWidgets(screen)"));
        assertTrue(mixins.contains("QuickStackIconButtonMixin"));
        assertTrue(mixins.contains("QuickStackCustomButtonChromeMixin"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(Path.of(System.getProperty("projectRoot")).resolve(relative), StandardCharsets.UTF_8);
    }
}
