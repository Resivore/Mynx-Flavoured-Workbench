package net.minecraft.client.gui;

import net.minecraft.world.item.ItemStack;

public final class GuiGraphics {
    private final GuiGraphicsExtractor extractor;

    public GuiGraphics(GuiGraphicsExtractor extractor) {
        this.extractor = extractor;
    }

    public void fill(int left, int top, int right, int bottom, int color) {
        extractor.fill(left, top, right, bottom, color);
    }

    public void drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
        extractor.text(font, text, x, y, color, dropShadow);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        extractor.item(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        extractor.itemDecorations(font, stack, x, y);
    }
}
