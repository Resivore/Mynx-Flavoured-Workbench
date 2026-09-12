package dev.resivore.slotreservations.client;

public record ShulkerPanelGeometry(int x, int y) {
    public static final int WIDTH = 176;
    /** The existing top/title/grid portion of the vanilla shulker screen. */
    public static final int MAIN_HEIGHT = 77;
    /** The full-width bottom frame in the same runtime shulker_box texture. */
    public static final int BOTTOM_FRAME_HEIGHT = 6;
    public static final int HEIGHT = MAIN_HEIGHT + BOTTOM_FRAME_HEIGHT;
    public static final int GRID_X = 7, GRID_Y = 17, CELL = 18, COLUMNS = 9, ROWS = 3;
    /** Native shulker-screen title positioning and its matching small header insets. */
    public static final int TITLE_X = 8, TITLE_Y = 6, HEADER_RIGHT_INSET = 8, HEADER_GAP = 4;
    public static ShulkerPanelGeometry place(int viewportWidth, int viewportHeight, Rect host) {
        // The panel's bottom-left corner belongs at the center of its actual host slot.
        // Clamping is deliberately the only placement adjustment; it never reverts to a
        // screen-edge side rail.
        int x = clamp(host.x() + host.width() / 2, 0, Math.max(0, viewportWidth - WIDTH));
        int y = clamp(host.y() + host.height() / 2 - HEIGHT, 0,
                Math.max(0, viewportHeight - HEIGHT));
        return new ShulkerPanelGeometry(x, y);
    }

    public Rect bounds() { return new Rect(x, y, WIDTH, HEIGHT); }
    public Rect cellBounds(int slot) {
        if (slot < 0 || slot >= COLUMNS * ROWS) throw new IndexOutOfBoundsException(slot);
        return new Rect(x + GRID_X + slot % COLUMNS * CELL,
                y + GRID_Y + slot / COLUMNS * CELL, CELL, CELL);
    }
    public int itemX(int slot) { return cellBounds(slot).x() + 1; }
    public int itemY(int slot) { return cellBounds(slot).y() + 1; }

    public int titleWidth(int decorationWidth) {
        return WIDTH - TITLE_X - HEADER_RIGHT_INSET
                - (decorationWidth > 0 ? decorationWidth + HEADER_GAP : 0);
    }

    public int headerDecorationX(int decorationWidth) {
        return x + WIDTH - HEADER_RIGHT_INSET - decorationWidth;
    }

    public int headerDecorationY(int decorationHeight) {
        return y + Math.max(0, (GRID_Y - decorationHeight) / 2);
    }

    public int slot(double mouseX, double mouseY) {
        double dx = mouseX - x - GRID_X, dy = mouseY - y - GRID_Y;
        if (dx < 0 || dy < 0 || dx >= COLUMNS * CELL || dy >= ROWS * CELL) return -1;
        return (int) (dy / CELL) * COLUMNS + (int) (dx / CELL);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < right() && py < bottom();
        }
    }
}
