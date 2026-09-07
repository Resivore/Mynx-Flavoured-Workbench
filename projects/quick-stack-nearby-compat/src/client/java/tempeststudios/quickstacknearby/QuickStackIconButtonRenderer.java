package tempeststudios.quickstacknearby;

import net.minecraft.client.gui.GuiGraphics;

final class QuickStackIconButtonRenderer {
    static final int SIZE = 12;

    private static final int ICON_COLOR = 0xFF1C1C1C;
    private static final int ICON_HOVER_COLOR = 0xFF000000;

    private QuickStackIconButtonRenderer() {
    }

    static void render(GuiGraphics guiGraphics, int x, int y, boolean hovered) {
        drawBeveledBackground(guiGraphics, x, y, SIZE, SIZE, hovered);
        drawQuickStackIcon(guiGraphics, x, y, hovered);
    }

    private static void drawBeveledBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        fill(guiGraphics, x + 2, y, x + width - 2, y + 1, 0xFF000000);
        fill(guiGraphics, x + 2, y + height - 1, x + width - 2, y + height, 0xFF000000);
        fill(guiGraphics, x, y + 2, x + 1, y + height - 2, 0xFF000000);
        fill(guiGraphics, x + width - 1, y + 2, x + width, y + height - 2, 0xFF000000);
        fill(guiGraphics, x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        fill(guiGraphics, x + width - 2, y + 1, x + width - 1, y + 2, 0xFF000000);
        fill(guiGraphics, x + 1, y + height - 2, x + 2, y + height - 1, 0xFF000000);
        fill(guiGraphics, x + width - 2, y + height - 2, x + width - 1, y + height - 1, 0xFF000000);

        int centerColor = hovered ? 0xFFE0E0E0 : 0xFFC6C6C6;
        fill(guiGraphics, x + 2, y + 2, x + width - 2, y + height - 2, centerColor);

        fill(guiGraphics, x + 2, y + 1, x + width - 2, y + 2, 0xFFFFFFFF);
        fill(guiGraphics, x + 1, y + 2, x + 2, y + height - 2, 0xFFFFFFFF);
        fill(guiGraphics, x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFF555555);
        fill(guiGraphics, x + width - 2, y + 2, x + width - 1, y + height - 2, 0xFF555555);
    }

    private static void drawQuickStackIcon(GuiGraphics guiGraphics, int x, int y, boolean hovered) {
        int color = hovered ? ICON_HOVER_COLOR : ICON_COLOR;

        // Downward arrow
        fill(guiGraphics, x + 5, y + 3, x + 7, y + 5, color); // shaft
        fill(guiGraphics, x + 4, y + 5, x + 8, y + 6, color); // arrowhead, wide
        fill(guiGraphics, x + 5, y + 6, x + 7, y + 7, color); // arrowhead tip

        // Open tray (no top edge - items dropping in)
        fill(guiGraphics, x + 3, y + 7, x + 4, y + 9, color); // left wall
        fill(guiGraphics, x + 8, y + 7, x + 9, y + 9, color); // right wall
        fill(guiGraphics, x + 3, y + 8, x + 9, y + 9, color); // bottom
    }

    private static void fill(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, bottom, color);
    }
}
