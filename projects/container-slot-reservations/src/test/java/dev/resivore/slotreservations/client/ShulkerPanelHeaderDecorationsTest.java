package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.api.client.ShulkerPanelHeaderDecorations;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShulkerPanelHeaderDecorationsTest {
    @Test void existingDecorationOnlyProviderStillResolvesWithoutAnInteraction() {
        ShulkerPanelHeaderDecorations.Provider legacy = stack -> Optional.of(
                new ShulkerPanelHeaderDecorations.Decoration(1, 1, (graphics, x, y) -> {})
        );
        assertTrue(legacy.interaction(ItemStack.EMPTY).isEmpty());
        ShulkerPanelHeaderDecorations.register(legacy);
        var resolved = ShulkerPanelHeaderDecorations.resolve(ItemStack.EMPTY).orElseThrow();
        assertEquals(1, resolved.decoration().width());
        assertTrue(resolved.interaction().isEmpty());
    }

    @Test void providerMayOptIntoItsOwnClickWithoutChangingDecorationConstruction() {
        ShulkerPanelHeaderDecorations.Provider interactive = new ShulkerPanelHeaderDecorations.Provider() {
            @Override public Optional<ShulkerPanelHeaderDecorations.Decoration> decoration(ItemStack stack) {
                return Optional.empty();
            }

            @Override public Optional<ShulkerPanelHeaderDecorations.Interaction> interaction(ItemStack stack) {
                return Optional.of(new ShulkerPanelHeaderDecorations.Interaction(context ->
                        context.menuId() == 7 && context.menuSlot() == 31));
            }
        };
        assertTrue(interactive.interaction(ItemStack.EMPTY).orElseThrow().handler().click(
                new ShulkerPanelHeaderDecorations.ClickContext(7, 31)));
    }
}
