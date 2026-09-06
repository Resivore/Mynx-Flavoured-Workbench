package dev.resivore.slotreservations.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.util.OptionalInt;

/** Resolves the bottom frame from the highest-priority active shulker GUI resource. */
public final class ShulkerPanelTextureLayout {
    public static final Identifier SHULKER_TEXTURE = Identifier.withDefaultNamespace(
            "textures/gui/container/shulker_box.png");
    private static final int LOGICAL_TEXTURE_SIZE = 256;
    private static volatile OptionalInt bottomFrameSourceY = OptionalInt.empty();

    private ShulkerPanelTextureLayout() {}

    /** Called once for each client resource reload; rendering only reads the cached result. */
    public static void reload(ResourceManager resources) {
        OptionalInt resolved = OptionalInt.empty();
        try (var input = resources.getResource(SHULKER_TEXTURE).orElseThrow().open();
             NativeImage image = NativeImage.read(input)) {
            resolved = resolve(image.getWidth(), image.getHeight(),
                    (x, y) -> ARGB.alpha(image.getPixel(x, y)));
        } catch (IOException | RuntimeException ignored) {
            // A malformed or non-standard resource must never turn a player-inventory row into a bezel.
        }
        cache(resolved);
    }

    public static OptionalInt bottomFrameSourceY() {
        return bottomFrameSourceY;
    }

    static OptionalInt resolve(int width, int height, AlphaReader alpha) {
        if (width <= 0 || height != width || width % LOGICAL_TEXTURE_SIZE != 0) return OptionalInt.empty();
        int scale = width / LOGICAL_TEXTURE_SIZE;
        if (scale <= 0) return OptionalInt.empty();

        int lastContentRow = -1;
        int guiWidth = ShulkerPanelGeometry.WIDTH * scale;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < guiWidth; x++) {
                if (alpha.alphaAt(x, y) != 0) {
                    lastContentRow = y;
                    break;
                }
            }
        }
        if (lastContentRow < 0) return OptionalInt.empty();
        int logicalLastContentRow = lastContentRow / scale;
        if (logicalLastContentRow < ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT - 1) return OptionalInt.empty();
        return OptionalInt.of(logicalLastContentRow - ShulkerPanelGeometry.BOTTOM_FRAME_HEIGHT + 1);
    }

    static void reloadForTest(int width, int height, AlphaReader alpha) {
        cache(resolve(width, height, alpha));
    }

    private static void cache(OptionalInt resolved) {
        bottomFrameSourceY = resolved;
    }

    @FunctionalInterface
    interface AlphaReader {
        int alphaAt(int x, int y);
    }
}
