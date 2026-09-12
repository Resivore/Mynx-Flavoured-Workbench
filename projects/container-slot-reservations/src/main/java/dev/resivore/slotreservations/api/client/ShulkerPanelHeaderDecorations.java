package dev.resivore.slotreservations.api.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-only extension point for one small decoration in CSR's carried-shulker header.
 * Providers may additionally own a click on their own rendered decoration; CSR never infers an
 * action or mutates a provider's state itself.
 */
public final class ShulkerPanelHeaderDecorations {
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private ShulkerPanelHeaderDecorations() {}

    public static void register(Provider provider) {
        PROVIDERS.add(Objects.requireNonNull(provider, "provider"));
    }

    public static Optional<Decoration> find(ItemStack stack) {
        return resolve(stack).map(ResolvedDecoration::decoration);
    }

    /** Resolves the first visible decoration together with that same provider's optional action. */
    public static Optional<ResolvedDecoration> resolve(ItemStack stack) {
        for (Provider provider : PROVIDERS) {
            Optional<Decoration> decoration = provider.decoration(stack);
            if (decoration.isPresent()) {
                return Optional.of(new ResolvedDecoration(decoration.orElseThrow(), provider.interaction(stack)));
            }
        }
        return Optional.empty();
    }

    @FunctionalInterface
    public interface Provider {
        Optional<Decoration> decoration(ItemStack stack);

        /** Existing decoration-only providers remain source and binary compatible. */
        default Optional<Interaction> interaction(ItemStack stack) {
            return Optional.empty();
        }
    }

    public record ResolvedDecoration(Decoration decoration, Optional<Interaction> interaction) {
        public ResolvedDecoration {
            Objects.requireNonNull(decoration, "decoration");
            interaction = interaction == null ? Optional.empty() : interaction;
        }
    }

    public record Interaction(ClickHandler handler) {
        public Interaction { Objects.requireNonNull(handler, "handler"); }
    }

    public record ClickContext(int menuId, int menuSlot) {}

    @FunctionalInterface
    public interface ClickHandler {
        /** Return true only when this provider consumed the click. */
        boolean click(ClickContext context);
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
