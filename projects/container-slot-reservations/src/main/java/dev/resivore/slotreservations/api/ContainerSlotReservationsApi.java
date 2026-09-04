package dev.resivore.slotreservations.api;

import dev.resivore.slotreservations.ReservationData;
import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.NativeInsertionPolicy;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.util.Objects;
import java.util.Optional;

/** Stable, query-only boundary for future transfer-system integrations. */
public final class ContainerSlotReservationsApi {
    private ContainerSlotReservationsApi() {
    }

    public static Optional<Reservation> getReservation(Container container, int slot) {
        return SupportedContainerResolver.resolve(container, slot)
                .flatMap(ReservationStore::get)
                .map(Reservation::new);
    }

    public static boolean isReserved(Container container, int slot) {
        return getReservation(container, slot).isPresent();
    }

    public static boolean reservationMatches(Container container, int slot, ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        return getReservation(container, slot).filter(reservation -> reservation.matches(incoming)).isPresent();
    }

    public static boolean mayInsert(Container container, int slot, ItemStack incoming) {
        return classify(container, slot, incoming).acceptsIncoming();
    }

    public static ReservationSlotClass classify(Container container, int slot, ItemStack incoming) {
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(incoming, "incoming");
        Optional<SupportedContainerResolver.ResolvedSlot> resolved =
                SupportedContainerResolver.resolve(container, slot);
        if (resolved.isEmpty()) {
            return ReservationSlotClass.INELIGIBLE;
        }
        SupportedContainerResolver.ResolvedSlot physical = resolved.orElseThrow();
        boolean nativeWritable = NativeInsertionPolicy.nativeMayInsert(physical, incoming);
        return classify(
                ReservationStore.getData(physical),
                physical.localSlot(),
                physical.physicalStack(),
                incoming,
                nativeWritable,
                physical.owner().getMaxStackSize(incoming)
        );
    }

    public static Optional<Reservation> getReservation(ItemStack shulker, int slot) {
        Objects.requireNonNull(shulker, "shulker");
        if (!SupportedContainerResolver.isSupportedShulkerItem(shulker) || !validSlot(slot)) {
            return Optional.empty();
        }
        return ReservationStore.getData(shulker).get(slot).map(Reservation::new);
    }

    public static boolean isReserved(ItemStack shulker, int slot) {
        return getReservation(shulker, slot).isPresent();
    }

    public static boolean reservationMatches(ItemStack shulker, int slot, ItemStack incoming) {
        Objects.requireNonNull(incoming, "incoming");
        return getReservation(shulker, slot).filter(reservation -> reservation.matches(incoming)).isPresent();
    }

    public static boolean mayInsert(ItemStack shulker, int slot, ItemStack incoming) {
        return classify(shulker, slot, incoming).acceptsIncoming();
    }

    public static ReservationSlotClass classify(ItemStack shulker, int slot, ItemStack incoming) {
        Objects.requireNonNull(shulker, "shulker");
        Objects.requireNonNull(incoming, "incoming");
        if (!SupportedContainerResolver.isSupportedShulkerItem(shulker) || !validSlot(slot)) {
            return ReservationSlotClass.INELIGIBLE;
        }

        ItemContainerContents contents = shulker.getOrDefault(
                DataComponents.CONTAINER,
                ItemContainerContents.EMPTY
        );
        ItemStack physical = contents.allItemsCopyStream()
                .skip(slot)
                .findFirst()
                .orElse(ItemStack.EMPTY);
        return classify(
                ReservationStore.getData(shulker),
                slot,
                physical,
                incoming,
                !incoming.isEmpty() && incoming.getItem().canFitInsideContainerItems(),
                incoming.getMaxStackSize()
        );
    }

    private static ReservationSlotClass classify(
            ReservationData data,
            int slot,
            ItemStack physical,
            ItemStack incoming,
            boolean nativeWritable,
            int maxStackSize
    ) {
        if (!nativeWritable) {
            return ReservationSlotClass.NON_WRITABLE;
        }

        Optional<ItemStack> reservation = data.get(slot);
        if (reservation.isPresent()
                && !ItemStack.isSameItemSameComponents(reservation.orElseThrow(), incoming)) {
            return ReservationSlotClass.RESERVED_OTHER;
        }

        if (!physical.isEmpty()) {
            return ItemStack.isSameItemSameComponents(physical, incoming)
                    && physical.getCount() < Math.min(maxStackSize, incoming.getMaxStackSize())
                    ? ReservationSlotClass.OCCUPIED_COMPATIBLE
                    : ReservationSlotClass.NON_WRITABLE;
        }

        return reservation.isPresent()
                ? ReservationSlotClass.RESERVED_MATCH
                : ReservationSlotClass.UNRESERVED_EMPTY;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < ReservationData.SLOT_COUNT;
    }
}
