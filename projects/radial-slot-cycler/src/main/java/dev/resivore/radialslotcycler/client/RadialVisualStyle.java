package dev.resivore.radialslotcycler.client;

import dev.resivore.radialslotcycler.RadialSlotCycler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** Independently authored rendering geometry for the private radial style. */
public final class RadialVisualStyle {
    static final Identifier OVERLAY = Identifier.fromNamespaceAndPath(
            RadialSlotCycler.MOD_ID, "textures/gui/belt_overlay.png");
    static final int TEXTURE_SIZE = 256;
    static final int ITEM_SIZE = 16;
    static final int ITEM_RING_RADIUS = 42;
    static final int HIGHLIGHT_PADDING = 2;
    static final int HIGHLIGHT_COLOR = 0x80FFFFFF;
    static final long OPEN_ANIMATION_MILLIS = 180L;
    static final float OVERLAY_OPACITY = 0.75F;

    private RadialVisualStyle() {}

    static float easedOpenProgress(long openedAtMillis, long nowMillis) {
        float progress = Mth.clamp(
                (nowMillis - openedAtMillis) / (float) OPEN_ANIMATION_MILLIS,
                0.0F,
                1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }

    static int itemX(int centerX, int index, int entryCount) {
        float angle = (float) RadialSelection.entryAngle(index, entryCount);
        return centerX + Mth.floor(Mth.cos(angle) * ITEM_RING_RADIUS) - ITEM_SIZE / 2;
    }

    static int itemY(int centerY, int index, int entryCount) {
        float angle = (float) RadialSelection.entryAngle(index, entryCount);
        return centerY + Mth.floor(Mth.sin(angle) * ITEM_RING_RADIUS) - ITEM_SIZE / 2;
    }

    static void beginTransform(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            float easedProgress
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(easedProgress, easedProgress);
        graphics.pose().translate(-centerX, -centerY);
    }

    static void endTransform(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    static void drawOverlay(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            float easedProgress
    ) {
        int alpha = ARGB.white(Math.min(easedProgress, OVERLAY_OPACITY));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                OVERLAY,
                centerX - TEXTURE_SIZE / 2,
                centerY - TEXTURE_SIZE / 2,
                0.0F,
                0.0F,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                alpha);
    }

    static void drawHighlight(GuiGraphicsExtractor graphics, int itemX, int itemY) {
        graphics.fill(
                itemX - HIGHLIGHT_PADDING,
                itemY - HIGHLIGHT_PADDING,
                itemX + ITEM_SIZE + HIGHLIGHT_PADDING,
                itemY + ITEM_SIZE + HIGHLIGHT_PADDING,
                HIGHLIGHT_COLOR);
    }
}
