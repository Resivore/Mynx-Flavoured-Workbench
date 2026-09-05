package dev.resivore.slotreservations.client;

/** Exact Item Interactions 26.2.2 geometry, independent of physical contents. */
public record TooltipGrid(int x, int y) {
    public static final int CELL = 18, WIDTH = 9, HEIGHT = 3;
    public int slot(double mouseX, double mouseY) {
        double dx = mouseX - x, dy = mouseY - y;
        if (dx < 0 || dy < 0 || dx >= WIDTH * CELL || dy >= HEIGHT * CELL) return -1;
        return (int) (dy / CELL) * WIDTH + (int) (dx / CELL);
    }
}
