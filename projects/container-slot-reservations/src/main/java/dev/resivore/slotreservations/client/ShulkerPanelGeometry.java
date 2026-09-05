package dev.resivore.slotreservations.client;

public record ShulkerPanelGeometry(int x, int y, boolean rightSide) {
    public static final int WIDTH = 176, HEIGHT = 77;
    public static final int GRID_X = 7, GRID_Y = 17, CELL = 18, COLUMNS = 9, ROWS = 3;
    private static final int GAP = 2;

    public static ShulkerPanelGeometry place(int viewportWidth, int viewportHeight,
                                             int screenLeft, int screenWidth,
                                             Rect host) {
        int right = screenLeft + screenWidth + GAP;
        int left = screenLeft - WIDTH - GAP;
        boolean useRight = right + WIDTH <= viewportWidth || left < 0;
        int rawX = useRight ? right : left;
        int x = clamp(rawX, 0, Math.max(0, viewportWidth - WIDTH));
        int y = clamp(host.y() - GRID_Y, 0, Math.max(0, viewportHeight - HEIGHT));
        return new ShulkerPanelGeometry(x, y, useRight);
    }

    public Rect bounds() { return new Rect(x, y, WIDTH, HEIGHT); }
    public Rect cellBounds(int slot) {
        if (slot < 0 || slot >= COLUMNS * ROWS) throw new IndexOutOfBoundsException(slot);
        return new Rect(x + GRID_X + slot % COLUMNS * CELL,
                y + GRID_Y + slot / COLUMNS * CELL, CELL, CELL);
    }
    public int itemX(int slot) { return cellBounds(slot).x() + 1; }
    public int itemY(int slot) { return cellBounds(slot).y() + 1; }

    public int slot(double mouseX, double mouseY) {
        double dx = mouseX - x - GRID_X, dy = mouseY - y - GRID_Y;
        if (dx < 0 || dy < 0 || dx >= COLUMNS * CELL || dy >= ROWS * CELL) return -1;
        return (int) (dy / CELL) * COLUMNS + (int) (dx / CELL);
    }

    public boolean corridorContains(Rect host, double mouseX, double mouseY) {
        double ax = rightSide ? host.right() : host.x();
        double ay = host.y() + host.height() / 2.0;
        double bx = rightSide ? x : x + WIDTH;
        double by = Math.max(y + 3, Math.min(y + HEIGHT - 3, ay));
        double vx = bx - ax, vy = by - ay;
        double lengthSquared = vx * vx + vy * vy;
        if (lengthSquared == 0) return false;
        double t = Math.max(0, Math.min(1, ((mouseX - ax) * vx + (mouseY - ay) * vy) / lengthSquared));
        double dx = mouseX - (ax + t * vx), dy = mouseY - (ay + t * vy);
        return dx * dx + dy * dy <= 16.0;
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
