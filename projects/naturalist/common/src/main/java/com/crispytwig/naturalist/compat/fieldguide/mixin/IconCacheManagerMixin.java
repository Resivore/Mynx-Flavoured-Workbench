package com.crispytwig.naturalist.compat.fieldguide.mixin;

import com.evandev.fieldguide.client.gui.util.IconCacheManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(IconCacheManager.class)
public class IconCacheManagerMixin {

    @Unique
    private static final int naturalist$ALPHA_THRESHOLD = 10;

    @Unique
    private static final float naturalist$MARGIN = 8.0F;

    @Unique
    private static final String naturalist$KEY_PREFIX = "naturalist_";

    @Shadow
    private static void mcRegisterWithSilhouette(String key, Identifier textureLocation, NativeImage image) {
        throw new AssertionError();
    }

    @Redirect(
        method = "lambda$generateAndSaveIcon$2(Lcom/mojang/blaze3d/buffers/GpuBuffer;" +
                "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;" +
                "Lcom/mojang/blaze3d/pipeline/RenderTarget;" +
                "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/evandev/fieldguide/client/gui/util/IconCacheManager;" +
                    "mcRegisterWithSilhouette(Ljava/lang/String;Lnet/minecraft/resources/Identifier;" +
                    "Lcom/mojang/blaze3d/platform/NativeImage;)V"
        )
    )
    private static void naturalist$fitAndRegister(String key, Identifier textureLocation, NativeImage image) {
        if (key.startsWith(naturalist$KEY_PREFIX)) {
            naturalist$fitAndCenter(image);
        }
        mcRegisterWithSilhouette(key, textureLocation, image);
    }

    @Unique
    private static void naturalist$fitAndCenter(NativeImage image) {
        int[] bounds = naturalist$contentBounds(image);
        if (bounds == null) {
            return;
        }

        int centerX = image.getWidth() / 2;
        int centerY = image.getHeight() / 2;
        int maxExtent = Math.max(
                Math.max(centerX - bounds[0], bounds[2] - centerX),
                Math.max(centerY - bounds[1], bounds[3] - centerY));
        float targetExtent = Math.min(image.getWidth(), image.getHeight()) / 2.0F - naturalist$MARGIN;
        if (maxExtent > targetExtent) {
            naturalist$scaleAboutCenter(image, targetExtent / maxExtent);
        }

        naturalist$centerContent(image);
    }

    @Unique
    private static void naturalist$scaleAboutCenter(NativeImage image, float scale) {
        int width = image.getWidth();
        int height = image.getHeight();
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        if (scaledWidth == width && scaledHeight == height) {
            return;
        }

        NativeImage scaled = new NativeImage(scaledWidth, scaledHeight, false);
        try {
            image.resizeSubRectTo(0, 0, width, height, scaled);
            image.fillRect(0, 0, width, height, 0);
            int offsetX = (width - scaledWidth) / 2;
            int offsetY = (height - scaledHeight) / 2;
            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    image.setPixel(offsetX + x, offsetY + y, scaled.getPixel(x, y));
                }
            }
        } finally {
            scaled.close();
        }
    }

    @Unique
    private static void naturalist$centerContent(NativeImage image) {
        int[] bounds = naturalist$contentBounds(image);
        if (bounds == null) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int dx = width / 2 - (bounds[0] + bounds[2] + 1) / 2;
        int dy = height / 2 - (bounds[1] + bounds[3] + 1) / 2;
        if (dx == 0 && dy == 0) {
            return;
        }

        int[] pixels = image.getPixels();
        image.fillRect(0, 0, width, height, 0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX = x + dx;
                int targetY = y + dy;
                if (targetX >= 0 && targetX < width && targetY >= 0 && targetY < height) {
                    image.setPixel(targetX, targetY, pixels[y * width + x]);
                }
            }
        }
    }

    @Unique
    private static int[] naturalist$contentBounds(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (ARGB.alpha(image.getPixel(x, y)) >= naturalist$ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        return maxX < 0 ? null : new int[] {minX, minY, maxX, maxY};
    }
}
