package dev.resivore.quickstacknearbycompat.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tempeststudios.quickstacknearby.QuickStackIconButton;

/**
 * Reuses Minecraft's own Button sprite, then draws the supplied QSN glyph as crisp logical pixels.
 * Other upstream custom buttons retain their original renderer.
 */
@Mixin(targets = "tempeststudios.quickstacknearby.QuickStackCustomButtonBase", remap = false)
public abstract class QuickStackCustomButtonChromeMixin {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF3F3F3F;

    @Shadow
    protected abstract void extractDefaultSprite(GuiGraphicsExtractor graphics);

    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Inject(
            method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void quickStackNearbyCompat$drawVanillaChromeAndGlyph(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if (!((Object) this instanceof QuickStackIconButton)) {
            return;
        }

        extractDefaultSprite(graphics);
        drawSuppliedGlyph(new GuiGraphics(graphics), getX(), getY());
        callback.cancel();
    }

    /**
     * Logical-pixel transcription of the 36x36 (2x GUI-scale) user reference:
     * white arrow pixels with its #3F3F3F Minecraft text shadow.
     */
    private static void drawSuppliedGlyph(GuiGraphics graphics, int x, int y) {
        fill(graphics, x + 8, y + 5, x + 10, y + 8, WHITE);
        fill(graphics, x + 7, y + 8, x + 11, y + 9, WHITE);
        fill(graphics, x + 8, y + 9, x + 10, y + 10, WHITE);
        fill(graphics, x + 6, y + 10, x + 7, y + 11, WHITE);
        fill(graphics, x + 11, y + 10, x + 12, y + 11, WHITE);
        fill(graphics, x + 6, y + 11, x + 12, y + 12, WHITE);

        fill(graphics, x + 10, y + 6, x + 11, y + 8, SHADOW);
        fill(graphics, x + 11, y + 8, x + 12, y + 9, SHADOW);
        fill(graphics, x + 10, y + 9, x + 11, y + 10, SHADOW);
        fill(graphics, x + 8, y + 10, x + 10, y + 11, SHADOW);
        fill(graphics, x + 12, y + 11, x + 13, y + 12, SHADOW);
        fill(graphics, x + 7, y + 12, x + 13, y + 13, SHADOW);
    }

    private static void fill(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, bottom, color);
    }
}
