package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelMenuQuickMovePayload;
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
        if (action.click() == ShulkerPanelContentActionPayload.Click.QUICK_MOVE) {
            return handleQuickMove(player, host, action);
        }
        ItemStack carried = host.menu().getCarried();
        ItemStack changedHost;
        ItemStack changedCarried;
        int selected = selectedForHost(player, host);
        boolean depositOnly = action.click() == ShulkerPanelContentActionPayload.Click.SECONDARY_DEPOSIT;

        if (carried.isEmpty() && !depositOnly) {
            ShulkerTransferPlanner.Extraction plan = ShulkerTransferPlanner.planExtraction(
                    host.stack(), action.internalSlot(),
                    action.click() == ShulkerPanelContentActionPayload.Click.SECONDARY,
                    Integer.MAX_VALUE);
            if (plan.moved() == 0) return false;
            changedHost = plan.shulker();
            changedCarried = plan.extracted();
            if (selected == action.internalSlot()) selected = ShulkerContents.nextOccupied(plan.contents(), selected);
        } else {
            // A C21 RMB deposit gesture stays a deposit even after its cursor has
            // drained. In that state an empty cursor is a no-op, never an extraction.
            if (carried.isEmpty()) return false;
            ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planExactInsertion(
                    host.stack(), carried, action.internalSlot(),
                    action.click() == ShulkerPanelContentActionPayload.Click.SECONDARY
                            || depositOnly);
            if (plan.moved() == 0) return false;
            changedHost = plan.shulker();
            changedCarried = plan.remainder();
            if (selected < 0) selected = ShulkerContents.firstOccupied(plan.contents());
        }
        commit(player, host, changedHost, changedCarried, selected, action.host());
        return true;
    }

    private static boolean handleQuickMove(ServerPlayer player, ShulkerHostResolver.ResolvedHost host,
                                           ShulkerPanelContentActionPayload action) {
        ShulkerPanelQuickMove.Plan plan = ShulkerPanelQuickMove.plan(player, host, action.internalSlot());
        if (plan.moved() == 0) return false;
        for (ShulkerPanelQuickMove.Move move : plan.moves()) {
            move.target().setByPlayer(move.after(), move.before());
            move.target().setChanged();
            move.target().container.setChanged();
        }
        int selected = selectedForHost(player, host);
        if (selected == action.internalSlot() && plan.contents().get(selected).isEmpty()) {
            selected = ShulkerContents.nextOccupied(plan.contents(), selected);
        }
        commit(player, host, plan.shulker(), host.menu().getCarried(), selected, action.host());
        return true;
    }

    /**
     * Mouse Tweaks sees virtual panel cells as the non-player inventory. Its
     * shift-drag from a player slot therefore arrives here rather than through
     * vanilla's menu routing, which cannot see those transient cells.
     */
    public static boolean handleMenuQuickMove(ServerPlayer player, ShulkerPanelMenuQuickMovePayload action) {
        if (action.sourceMenuSlot() < 0) return false;
        var resolved = ShulkerHostResolver.resolve(player, action.menuId(), action.host(), action.hostFingerprint());
        if (resolved.isEmpty()) return false;
        ShulkerHostResolver.ResolvedHost host = resolved.orElseThrow();
        if (action.sourceMenuSlot() >= host.menu().slots.size()) return false;
        Slot source = host.menu().slots.get(action.sourceMenuSlot());
        if (source == host.slot() || source.index != action.sourceMenuSlot()
                || source.container != player.getInventory()
                || !ShulkerHostResolver.removableSource(player, source)) return false;
        ItemStack before = source.getItem().copy();
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(host.stack(), before);
        if (plan.moved() == 0) return false;
        ItemStack remainder = before.copy();
        remainder.shrink(plan.moved());
        source.setByPlayer(remainder, before);
        source.setChanged();
        source.container.setChanged();
        int selected = selectedForHost(player, host);
        if (selected < 0) selected = ShulkerContents.firstOccupied(plan.contents());
        commit(player, host, plan.shulker(), host.menu().getCarried(), selected, action.host());
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
        if (action.source() == ReservationActionPayload.Source.SLOT_STACK) {
            var contents = ShulkerContents.copy(changed);
            ItemStack changedPhysical = contents.get(action.internalSlot());
            transition.attachIdentity(changedPhysical);
            ShulkerContents.replace(changed, contents);
        } else if (action.source() == ReservationActionPayload.Source.CARRIED_STACK) {
            transition.attachIdentity(carried);
        }
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
        host.slot().set(changedHost);
        host.slot().setChanged();
        host.slot().container.setChanged();
        host.menu().setCarried(changedCarried);
        ShulkerSelectionTracker.rebind(player, host, fingerprint, selected);
        synchronizeCommittedMenu(player, host.menu());
        // The vanilla menu packet(s) carry both the changed host and its real cursor state.
        // Send CSR's fingerprint metadata afterwards so it cannot make a live same-slot host
        // look stale before the authoritative menu state reaches the client.
        if (ServerPlayNetworking.canSend(player, ShulkerPanelSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new ShulkerPanelSyncPayload(
                    host.menu().containerId, locator, fingerprint, selected));
        }
        syncSharedViewers(player, host.slot());
    }

    /**
     * In 26.2, the normal synchronizer carries cursor changes in
     * ClientboundSetCursorItemPacket. ClientPacketListener deliberately ignores that packet
     * while CreativeModeInventoryScreen is open. The native full-content packet is the smallest
     * server-authoritative menu sync which Creative applies to its real carried stack, so use it
     * only for that inventory-menu case. Every ordinary menu keeps the incremental path.
     */
    private static void synchronizeCommittedMenu(ServerPlayer player, AbstractContainerMenu menu) {
        if (player.hasInfiniteMaterials() && menu == player.inventoryMenu) {
            menu.broadcastFullState();
        } else {
            menu.broadcastChanges();
        }
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
