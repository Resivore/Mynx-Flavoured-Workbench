package dev.resivore.slotreservations.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Marks exactly one extracted GUI item for an alpha-only final blit. */
public final class GhostItemRenderScope {
    public static final int OPAQUE_ALPHA = 0xFF;

    private static final ThreadLocal<Integer> ACTIVE_ALPHA = new ThreadLocal<>();

    private GhostItemRenderScope() {
    }

    public static void extract(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            int seed
    ) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(stack, "stack");
        withAlpha(ReservationVisualRenderer.GHOST_ALPHA_8, () -> graphics.item(stack, x, y, seed));
    }

    public static int activeAlpha() {
        Integer alpha = ACTIVE_ALPHA.get();
        return alpha == null ? OPAQUE_ALPHA : alpha;
    }

    /** Returns a straight ARGB opacity control whose RGB multiplier is neutral white. */
    public static int alphaOnlyWhite(int alpha) {
        validateAlpha(alpha);
        return ARGB.white(alpha);
    }

    static void withAlpha(int alpha, Runnable extraction) {
        validateAlpha(alpha);
        Objects.requireNonNull(extraction, "extraction");
        Integer previous = ACTIVE_ALPHA.get();
        ACTIVE_ALPHA.set(alpha);
        try {
            extraction.run();
        } finally {
            if (previous == null) {
                ACTIVE_ALPHA.remove();
            } else {
                ACTIVE_ALPHA.set(previous);
            }
        }
    }

    private static void validateAlpha(int alpha) {
        if (alpha < 0 || alpha > OPAQUE_ALPHA) {
            throw new IllegalArgumentException("Alpha must be between 0 and 255: " + alpha);
        }
    }
}
