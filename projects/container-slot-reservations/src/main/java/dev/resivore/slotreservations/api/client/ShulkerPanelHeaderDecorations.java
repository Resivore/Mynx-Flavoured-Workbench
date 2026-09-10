package dev.resivore.slotreservations.api.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-only, read-only extension point for one small decoration in CSR's carried-shulker header.
 * Providers are queried for the currently bound ItemStack; CSR continues normally when none applies.
 */
public final class ShulkerPanelHeaderDecorations {
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private ShulkerPanelHeaderDecorations() {}

    public static void register(Provider provider) {
        PROVIDERS.add(Objects.requireNonNull(provider, "provider"));
    }

    public static Optional<Decoration> find(ItemStack stack) {
        for (Provider provider : PROVIDERS) {
            Optional<Decoration> decoration = provider.decoration(stack);
            if (decoration.isPresent()) return decoration;
        }
        return Optional.empty();
    }

    @FunctionalInterface
    public interface Provider {
        Optional<Decoration> decoration(ItemStack stack);
    }

    public record Decoration(int width, int height, Renderer renderer) {
        public Decoration {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Decoration dimensions must be positive");
            Objects.requireNonNull(renderer, "renderer");
        }
    }

    @FunctionalInterface
    public interface Renderer {
        void render(GuiGraphicsExtractor graphics, int x, int y);
    }
}
