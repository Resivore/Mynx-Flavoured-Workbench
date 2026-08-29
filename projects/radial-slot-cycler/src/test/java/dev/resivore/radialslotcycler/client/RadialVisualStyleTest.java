package dev.resivore.radialslotcycler.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadialVisualStyleTest {
    @Test
    void exactPrivateVisualConstantsRemainFrozen() {
        assertEquals(256, RadialVisualStyle.TEXTURE_SIZE);
        assertEquals(42, RadialVisualStyle.ITEM_RING_RADIUS);
        assertEquals(16, RadialVisualStyle.ITEM_SIZE);
        assertEquals(2, RadialVisualStyle.HIGHLIGHT_PADDING);
        assertEquals(0x80FFFFFF, RadialVisualStyle.HIGHLIGHT_COLOR);
        assertEquals(180L, RadialVisualStyle.OPEN_ANIMATION_MILLIS);
        assertEquals(0.75F, RadialVisualStyle.OVERLAY_OPACITY);
    }

    @Test
    void sevenEntryOffsetsMatchTheAuditedPrivatePresentation() {
        int[][] expected = {
                {-8, -50}, {24, -35}, {32, 1}, {10, 29},
                {-27, 29}, {-49, 1}, {-41, -35}
        };
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index][0], RadialVisualStyle.itemX(0, index, 7));
            assertEquals(expected[index][1], RadialVisualStyle.itemY(0, index, 7));
        }
    }

    @Test
    void openingUsesTheAuditedSmoothstepTiming() {
        assertEquals(0.0F, RadialVisualStyle.easedOpenProgress(1000L, 1000L));
        assertEquals(0.5F, RadialVisualStyle.easedOpenProgress(1000L, 1090L));
        assertEquals(1.0F, RadialVisualStyle.easedOpenProgress(1000L, 1180L));
        assertEquals(1.0F, RadialVisualStyle.easedOpenProgress(1000L, 2000L));
    }
}
