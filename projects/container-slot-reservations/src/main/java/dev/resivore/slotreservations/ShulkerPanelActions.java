package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ShulkerPanelActions {
    private ShulkerPanelActions() {}

    public static boolean handleContent(ServerPlayer player, ShulkerPanelContentActionPayload action) {
        if (action.click() == null || action.internalSlot() < 0
                || action.internalSlot() >= ReservationData.SLOT_COUNT) return false;
        var resolved = ShulkerHostResolver.resolve(player, action.menuId(), action.host(), action.hostFingerprint());
        if (resolved.isEmpty()) return false;
        ShulkerHostResolver.ResolvedHost host = resolved.orElseThrow();
        ItemStack carried = host.menu().getCarried();
        ItemStack changedHost;
        ItemStack changedCarried;
        int selected = selectedForHost(player, host);

        if (carried.isEmpty()) {
            ShulkerTransferPlanner.Extraction plan = ShulkerTransferPlanner.planExtraction(
                    host.stack(), action.internalSlot(),
                    action.click() == ShulkerPanelContentActionPayload.Click.SECONDARY,
                    Integer.MAX_VALUE);
            if (plan.moved() == 0) return false;
            changedHost = plan.shulker();
            changedCarried = plan.extracted();
            if (selected == action.internalSlot()) selected = ShulkerContents.nextOccupied(plan.contents(), selected);
        } else {
            ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planExactInsertion(
                    host.stack(), carried, action.internalSlot(),
                    action.click() == ShulkerPanelContentActionPayload.Click.SECONDARY);
            if (plan.moved() == 0) return false;
            changedHost = plan.shulker();
            changedCarried = plan.remainder();
            if (selected < 0) selected = ShulkerContents.firstOccupied(plan.contents());
        }
        commit(player, host, changedHost, changedCarried, selected, action.host());
        return true;
    }

    public static boolean handleReservation(ServerPlayer player, ShulkerPanelReservationActionPayload action) {
        if (action.source() == null || action.internalSlot() < 0
                || action.internalSlot() >= ReservationData.SLOT_COUNT) return false;
        var resolved = ShulkerHostResolver.resolve(player, action.menuId(), action.host(), action.hostFingerprint());
        if (resolved.isEmpty()) return false;
        ShulkerHostResolver.ResolvedHost host = resolved.orElseThrow();
        ItemStack physical = ShulkerContents.copy(host.stack()).get(action.internalSlot());
        ItemStack carried = host.menu().getCarried();
        ReservationData current = ReservationStore.getData(host.stack());
        ReservationTransition.Result transition;
        ItemStack displayed = ItemStack.EMPTY;
        switch (action.source()) {
            case SLOT_STACK -> {
                if (physical.isEmpty()) return false;
                transition = ReservationTransition.fromOccupied(current, action.internalSlot(), physical);
                displayed = physical;
            }
            case CARRIED_STACK -> {
                if (!physical.isEmpty() || carried.isEmpty() || !carried.getItem().canFitInsideContainerItems()) return false;
                transition = ReservationTransition.fromCursor(current, action.internalSlot(), carried);
                displayed = carried;
            }
            case CLEAR_EMPTY -> {
                if (!physical.isEmpty() || !carried.isEmpty()) return false;
                transition = ReservationTransition.clear(current, action.internalSlot());
                if (!transition.changed()) return false;
            }
            default -> { return false; }
        }
        if (transition.outcome() == ReservationTransition.Outcome.REJECTED) return false;
        if (!transition.changed()) return true;
        ItemStack changed = host.stack().copy();
        ReservationStore.setData(changed, transition.data());
        int selected = selectedForHost(player, host);
        commit(player, host, changed, carried, selected, action.host());
        player.sendOverlayMessage(transition.outcome() == ReservationTransition.Outcome.CLEARED
                ? Component.translatable("text.container_slot_reservations.cleared")
                : Component.translatable("text.container_slot_reservations.reserved", displayed.getHoverName()));
        return true;
    }

    private static int selectedForHost(ServerPlayer player, ShulkerHostResolver.ResolvedHost host) {
        ShulkerSelectionTracker.Selection selection = ShulkerSelectionTracker.validate(player);
        return selection != null && selection.kind() == ShulkerSelectionTracker.HostKind.MENU_SLOT
                && selection.slot() == host.slot() ? selection.internalSlot() : -1;
    }

    private static void commit(ServerPlayer player, ShulkerHostResolver.ResolvedHost host,
                               ItemStack changedHost, ItemStack changedCarried, int selected,
                               dev.resivore.slotreservations.network.ShulkerHostLocator locator) {
        String fingerprint = ShulkerHostFingerprint.of(changedHost, player.registryAccess());
        if (ServerPlayNetworking.canSend(player, ShulkerPanelSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ShulkerPanelSyncPayload(
                    host.menu().containerId, locator, fingerprint, selected));
        }
        host.slot().set(changedHost);
        host.slot().setChanged();
        host.slot().container.setChanged();
        host.menu().setCarried(changedCarried);
        ShulkerSelectionTracker.rebind(player, host, fingerprint, selected);
        host.menu().broadcastChanges();
        syncSharedViewers(player, host.slot());
    }

    static void syncSharedViewers(ServerPlayer actor, Slot changedSlot) {
        var changed = SupportedContainerResolver.resolve(
                changedSlot.container, changedSlot.getContainerSlot()).orElse(null);
        for (ServerPlayer viewer : actor.level().getServer().getPlayerList().getPlayers()) {
            if (viewer != actor && viewer.containerMenu.stillValid(viewer)
                    && viewer.containerMenu.slots.stream().anyMatch(slot -> {
                        if (slot.container == changedSlot.container) return true;
                        if (changed == null) return false;
                        return SupportedContainerResolver.resolve(slot.container, slot.getContainerSlot())
                                .filter(candidate -> candidate.owner() == changed.owner()).isPresent();
                    })) {
                viewer.containerMenu.broadcastChanges();
            }
        }
    }
}
