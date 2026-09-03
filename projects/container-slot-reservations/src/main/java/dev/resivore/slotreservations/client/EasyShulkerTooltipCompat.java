package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/** Optional, linkage-safe gateway for the Easy Shulker Boxes / Item Interactions tooltip. */
public final class EasyShulkerTooltipCompat {
    private static final boolean ENABLED =
            FabricLoader.getInstance().isModLoaded("easyshulkerboxes")
                    && FabricLoader.getInstance().isModLoaded("iteminteractions");

    private EasyShulkerTooltipCompat() {
    }

    public static void captureSource(
            ItemStack sourceStack,
            Optional<TooltipComponent> tooltipComponent
    ) {
        captureSource(ENABLED, sourceStack, tooltipComponent);
    }

    static void captureSource(
            boolean enabled,
            ItemStack sourceStack,
            Optional<TooltipComponent> tooltipComponent
    ) {
        Objects.requireNonNull(sourceStack, "sourceStack");
        Objects.requireNonNull(tooltipComponent, "tooltipComponent");
        if (!enabled || !SupportedContainerResolver.isSupportedShulkerItem(sourceStack)) {
            return;
        }

        captureSource(sourceStack, ReservationStore.getData(sourceStack), tooltipComponent);
    }

    static void captureSource(
            ItemStack sourceStack,
            ReservationData reservations,
            Optional<TooltipComponent> tooltipComponent
    ) {
        Objects.requireNonNull(sourceStack, "sourceStack");
        Objects.requireNonNull(reservations, "reservations");
        Objects.requireNonNull(tooltipComponent, "tooltipComponent");
        if (reservations.isEmpty()) {
            return;
        }

        tooltipComponent.ifPresent(component -> {
            if (component instanceof TooltipSourceAccess access) {
                access.containerSlotReservations$setSourceStack(sourceStack.copy());
            }
        });
    }
}
