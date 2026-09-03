package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Pure slot-index planning for the 9x3 vanilla shulker contents tooltip. */
public final class ShulkerTooltipOverlay {
    public static final int GRID_WIDTH = 9;
    public static final int GRID_HEIGHT = 3;

    private ShulkerTooltipOverlay() {
    }

    public static Optional<SlotOverlay> at(
            ItemStack sourceShulker,
            List<ItemStack> physicalSlots,
            int slot
    ) {
        Objects.requireNonNull(sourceShulker, "sourceShulker");
        Objects.requireNonNull(physicalSlots, "physicalSlots");
        if (!SupportedContainerResolver.isSupportedShulkerItem(sourceShulker)
                || physicalSlots.size() != ReservationData.SLOT_COUNT
                || slot < 0
                || slot >= ReservationData.SLOT_COUNT) {
            return Optional.empty();
        }

        return at(ReservationStore.getData(sourceShulker), physicalSlots, slot);
    }

    public static Optional<SlotOverlay> at(
            ReservationData reservations,
            List<ItemStack> physicalSlots,
            int slot
    ) {
        Objects.requireNonNull(reservations, "reservations");
        Objects.requireNonNull(physicalSlots, "physicalSlots");
        if (physicalSlots.size() != ReservationData.SLOT_COUNT
                || slot < 0
                || slot >= ReservationData.SLOT_COUNT) {
            return Optional.empty();
        }

        Optional<ItemStack> reservation = reservations.get(slot);
        if (reservation.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SlotOverlay(
                slot,
                ReservationVisualRenderer.state(physicalSlots.get(slot), reservation),
                reservation.orElseThrow()
        ));
    }

    public static List<SlotOverlay> plan(ItemStack sourceShulker, List<ItemStack> physicalSlots) {
        Objects.requireNonNull(sourceShulker, "sourceShulker");
        Objects.requireNonNull(physicalSlots, "physicalSlots");
        if (!SupportedContainerResolver.isSupportedShulkerItem(sourceShulker)) {
            return List.of();
        }
        return plan(ReservationStore.getData(sourceShulker), physicalSlots);
    }

    public static List<SlotOverlay> plan(
            ReservationData reservations,
            List<ItemStack> physicalSlots
    ) {
        Objects.requireNonNull(reservations, "reservations");
        Objects.requireNonNull(physicalSlots, "physicalSlots");
        List<SlotOverlay> overlays = new ArrayList<>();
        for (int slot = 0; slot < ReservationData.SLOT_COUNT; slot++) {
            at(reservations, physicalSlots, slot).ifPresent(overlays::add);
        }
        return List.copyOf(overlays);
    }

    public record SlotOverlay(
            int slot,
            ReservationVisualRenderer.SlotVisualState state,
            ItemStack template
    ) {
        public SlotOverlay {
            if (slot < 0 || slot >= ReservationData.SLOT_COUNT) {
                throw new IndexOutOfBoundsException("Tooltip slot must be between 0 and 26: " + slot);
            }
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(template, "template");
            if (template.isEmpty()) {
                throw new IllegalArgumentException("A tooltip reservation template must be non-empty");
            }
            template = template.copyWithCount(1);
        }

        @Override
        public ItemStack template() {
            return template.copy();
        }
    }
}
