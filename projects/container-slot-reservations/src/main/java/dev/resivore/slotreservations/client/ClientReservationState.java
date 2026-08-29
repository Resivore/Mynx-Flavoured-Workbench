package dev.resivore.slotreservations.client;

import dev.resivore.slotreservations.network.ReservationSnapshotPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientReservationState {
    private static AbstractContainerMenu boundMenu;
    private static int boundMenuId = -1;
    private static Map<Integer, Optional<ItemStackTemplate>> eligibleSlots = Map.of();

    private ClientReservationState() {
    }

    public static void accept(AbstractContainerMenu menu, ReservationSnapshotPayload snapshot) {
        if (snapshot.menuId() != menu.containerId) return;
        Map<Integer, Optional<ItemStackTemplate>> accepted = new HashMap<>();
        for (ReservationSnapshotPayload.Entry entry : snapshot.entries()) {
            if (entry.menuSlotIndex() < 0 || entry.menuSlotIndex() >= menu.slots.size()) continue;
            accepted.put(entry.menuSlotIndex(), entry.template().map(template -> template.withCount(1)));
        }
        boundMenu = menu;
        boundMenuId = menu.containerId;
        eligibleSlots = Map.copyOf(accepted);
    }

    public static void clearUnlessBoundTo(AbstractContainerMenu menu) {
        if (boundMenu != menu || boundMenuId != menu.containerId) clear();
    }

    public static void clear() {
        boundMenu = null;
        boundMenuId = -1;
        eligibleSlots = Map.of();
    }

    public static boolean isEligible(AbstractContainerMenu menu, Slot slot) {
        int menuIndex = menu.slots.indexOf(slot);
        return isBound(menu) && menuIndex >= 0 && eligibleSlots.containsKey(menuIndex);
    }

    public static Optional<ItemStack> template(AbstractContainerMenu menu, Slot slot) {
        int menuIndex = menu.slots.indexOf(slot);
        if (!isBound(menu) || menuIndex < 0) return Optional.empty();
        return eligibleSlots.getOrDefault(menuIndex, Optional.empty()).map(ItemStackTemplate::create);
    }

    private static boolean isBound(AbstractContainerMenu menu) {
        return boundMenu == menu && boundMenuId == menu.containerId;
    }
}
