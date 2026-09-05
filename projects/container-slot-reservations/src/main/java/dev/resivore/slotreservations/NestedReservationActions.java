package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.NestedReservationActionPayload;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Mutates only the actual server-owned stack in the exact active menu slot. */
public final class NestedReservationActions {
    private NestedReservationActions() {}

    public static boolean handle(ServerPlayer player, NestedReservationActionPayload action) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!player.isAlive() || player.isSpectator() || menu.containerId != action.menuId()
                || menu.getStateId() != action.stateId() || !menu.stillValid(player)
                || action.hostSlot() < 0 || action.hostSlot() >= menu.slots.size()
                || action.nestedSlot() < 0 || action.nestedSlot() >= 27 || action.source() == null) return false;
        Slot slot = menu.slots.get(action.hostSlot());
        if (slot.index != action.hostSlot() || !slot.isActive() || slot.isFake()
                || !slot.mayPickup(player) || !slot.container.stillValid(player)) return false;
        ItemStack host = slot.getItem();
        if (host.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(host)
                || !ShulkerHostFingerprint.of(host, player.registryAccess()).equals(action.hostFingerprint())) return false;
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        ItemStack physical = contents.get(action.nestedSlot());
        ItemStack carried = menu.getCarried();
        ReservationData current = ReservationStore.getData(host);
        ReservationTransition.Result result;
        switch (action.source()) {
            case SLOT_STACK -> {
                if (physical.isEmpty()) return false;
                result = ReservationTransition.fromOccupied(current, action.nestedSlot(), physical);
            }
            case CARRIED_STACK -> {
                if (!physical.isEmpty() || carried.isEmpty() || !carried.getItem().canFitInsideContainerItems()) return false;
                result = ReservationTransition.fromCursor(current, action.nestedSlot(), carried);
            }
            case CLEAR_EMPTY -> {
                if (!physical.isEmpty() || !carried.isEmpty()) return false;
                result = ReservationTransition.clear(current, action.nestedSlot());
                if (!result.changed()) return false;
            }
            default -> { return false; }
        }
        if (result.outcome() == ReservationTransition.Outcome.REJECTED) return false;
        if (!result.changed()) return true;
        ReservationStore.setData(host, result.data());
        slot.setChanged();
        slot.container.setChanged();
        menu.broadcastChanges();
        // Shared containers expose the same server-owned host to each legitimate viewer.
        for (ServerPlayer viewer : player.level().getServer().getPlayerList().getPlayers()) {
            if (viewer != player && viewer.containerMenu.stillValid(viewer)
                    && viewer.containerMenu.slots.stream().anyMatch(other -> other.container == slot.container
                            || other.getItem() == host)) viewer.containerMenu.broadcastChanges();
        }
        return true;
    }
}
