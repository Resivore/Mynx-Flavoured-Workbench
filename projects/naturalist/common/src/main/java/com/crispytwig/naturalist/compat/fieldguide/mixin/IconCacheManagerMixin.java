package com.crispytwig.naturalist.compat.fieldguide.mixin;

import com.crispytwig.naturalist.client.NaturalistPortraitRenderState;
import com.evandev.fieldguide.client.gui.util.IconCacheManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

@Mixin(IconCacheManager.class)
public class IconCacheManagerMixin {

    @Unique
    private static final int naturalist$ALPHA_THRESHOLD = 10;

    @Unique
    private static final int naturalist$SIZE = 256;

    @Unique
    private static final float naturalist$MEASURE = 0.25F;

    @Unique
    private static final float naturalist$MARGIN = 8.0F;

    @Redirect(
        method = "generateAndSaveIcon(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Runnable;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V")
    )
    private static void naturalist$autoFit(Runnable renderAction, String namespace, String fileName, String key, Runnable renderActionArg) {
        if (!"naturalist".equals(namespace)) {
            renderAction.run();
            return;
        }
        try {
            NaturalistPortraitRenderState.SCALE = naturalist$MEASURE;
            renderAction.run();

            float maxExtent = naturalist$maxExtentFromCenter();
            float mul = 1.0F;
            if (maxExtent > 0.0F) {
                mul = Math.min(1.0F, (naturalist$SIZE / 2.0F - naturalist$MARGIN) * naturalist$MEASURE / maxExtent);
            }

            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(16640, Minecraft.ON_OSX);

            NaturalistPortraitRenderState.SCALE = mul;
            renderAction.run();
        } finally {
            NaturalistPortraitRenderState.SCALE = 1.0F;
        }
    }

    @Unique
    private static float naturalist$maxExtentFromCenter() {
        int size = naturalist$SIZE;
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        GL11.glReadPixels(0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

        int minX = size, minY = size, maxX = -1, maxY = -1;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int alpha = buf.get((y * size + x) * 4 + 3) & 0xFF;
                if (alpha >= naturalist$ALPHA_THRESHOLD) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return 0.0F;
        }
        int c = size / 2;
        return Math.max(Math.max(c - minX, maxX - c), Math.max(c - minY, maxY - c));
    }

    @Redirect(
        method = "generateAndSaveIcon(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Runnable;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;flipY()V")
    )
    private static void naturalist$flipAndCenter(NativeImage image, String namespace, String fileName, String key, Runnable renderAction) {
        image.flipY();
        if ("naturalist".equals(namespace)) {
            naturalist$centerContent(image);
        }
    }

    @Unique
    private static void naturalist$centerContent(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = (image.getPixelRGBA(x, y) >>> 24) & 0xFF;
                if (alpha >= naturalist$ALPHA_THRESHOLD) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) {
            return;
        }

        int dx = w / 2 - (minX + maxX + 1) / 2;
        int dy = h / 2 - (minY + maxY + 1) / 2;
        if (dx == 0 && dy == 0) {
            return;
        }

        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = image.getPixelRGBA(x, y);
            }
        }
        image.fillRect(0, 0, w, h, 0);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    image.setPixelRGBA(nx, ny, pixels[y * w + x]);
                }
            }
        }
    }
}
