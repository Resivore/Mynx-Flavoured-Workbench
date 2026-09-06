package dev.resivore.slotreservations.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

final class ShulkerPanelGeometryTest {
    private static final ShulkerPanelGeometry.Rect HOST = new ShulkerPanelGeometry.Rect(100, 80, 18, 18);

    @Test void everyCellMapsToItsExactPhysicalIndex() {
        var geometry = new ShulkerPanelGeometry(10, 20, true);
        for (int slot = 0; slot < 27; slot++) {
            var bounds = geometry.cellBounds(slot);
            assertEquals(slot, geometry.slot(bounds.x() + 0.5, bounds.y() + 0.5));
            assertEquals(slot, geometry.slot(bounds.right() - 0.5, bounds.bottom() - 0.5));
            assertEquals(geometry.x() + 8 + slot % 9 * 18, geometry.itemX(slot));
            assertEquals(geometry.y() + 18 + slot / 9 * 18, geometry.itemY(slot));
        }
        assertEquals(-1, geometry.slot(16.9, 37));
        assertEquals(-1, geometry.slot(179, 37));
        assertThrows(IndexOutOfBoundsException.class, () -> geometry.cellBounds(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> geometry.cellBounds(27));
    }

    @Test void placementPrefersRightThenLeftAndClampsAtAllViewportEdges() {
        var right = ShulkerPanelGeometry.place(500, 300, 40, 176, HOST);
        assertTrue(right.rightSide());
        assertEquals(218, right.x());
        var left = ShulkerPanelGeometry.place(390, 300, 200, 176, HOST);
        assertFalse(left.rightSide());
        assertEquals(22, left.x());
        var top = ShulkerPanelGeometry.place(500, 60, 40, 176,
                new ShulkerPanelGeometry.Rect(100, -30, 18, 18));
        assertEquals(0, top.y());
        var bottom = ShulkerPanelGeometry.place(500, 120, 40, 176,
                new ShulkerPanelGeometry.Rect(100, 200, 18, 18));
        assertEquals(37, bottom.y());
        var tiny = ShulkerPanelGeometry.place(100, 50, 20, 176, HOST);
        assertEquals(0, tiny.x());
        assertEquals(0, tiny.y());
        assertEquals(176, tiny.bounds().width());
        assertEquals(83, tiny.bounds().height());
        assertEquals(77, ShulkerPanelGeometry.MAIN_HEIGHT);
        assertEquals(160, ShulkerPanelGeometry.BOTTOM_FRAME_SOURCE_Y,
                "26.2's true bottom bezel starts after the player inventory and hotbar rows");
        assertEquals(6, ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT,
                "The selected region is only the six-pixel bottom bezel, never a slot row");
    }

    @Test void resizeRecomputesPlacementAndCorridorIsNarrowAndContinuous() {
        var large = ShulkerPanelGeometry.place(600, 400, 100, 176, HOST);
        var resized = ShulkerPanelGeometry.place(300, 180, 100, 176, HOST);
        assertNotEquals(large.x(), resized.x());
        assertTrue(large.corridorContains(HOST, 119, 89));
        assertFalse(large.corridorContains(HOST, 150, 130));
        assertTrue(large.bounds().contains(large.x(), large.y()));
        assertFalse(large.bounds().contains(large.x() + 176, large.y() + 82));
        assertEquals(26, large.slot(large.cellBounds(26).x() + 0.5, large.cellBounds(26).y() + 0.5),
                "Extending the lower frame must not move the 9x3 cell grid");
    }

    @Test void vanilla26BottomSourceIsOnlyTheSolidShulkerBezel() throws Exception {
        try (InputStream input = ShulkerPanelGeometryTest.class.getClassLoader().getResourceAsStream(
                "assets/minecraft/textures/gui/container/shulker_box.png")) {
            assertNotNull(input, "The exact 26.2 runtime-resolved shulker texture is required for this contract");
            BufferedImage texture = ImageIO.read(input);
            assertNotNull(texture);
            assertEquals(256, texture.getWidth());
            assertEquals(256, texture.getHeight());

            // Y=156 is the hotbar slot row: its vertical dividers must never become panel bezel pixels.
            assertNotEquals(texture.getRGB(7, 156), texture.getRGB(24, 156));
            for (int y = ShulkerPanelGeometry.BOTTOM_FRAME_SOURCE_Y;
                 y < ShulkerPanelGeometry.BOTTOM_FRAME_SOURCE_Y + ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT; y++) {
                int uninterruptedBezel = texture.getRGB(8, y);
                for (int dividerPosition : new int[]{26, 44, 62, 80, 98, 116, 134, 152}) {
                    assertEquals(uninterruptedBezel, texture.getRGB(dividerPosition, y),
                            "Bottom source must not include a player-inventory or hotbar slot divider at Y=" + y);
                }
            }
        }
    }
}
