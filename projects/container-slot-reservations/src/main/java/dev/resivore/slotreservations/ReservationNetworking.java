package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.ReservationActionPayload;
import dev.resivore.slotreservations.network.ReservationSnapshotPayload;
import dev.resivore.slotreservations.network.ReservationSnapshotRequestPayload;
import dev.resivore.slotreservations.network.ShulkerPanelContentActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelReservationActionPayload;
import dev.resivore.slotreservations.network.ShulkerPanelSyncPayload;
import dev.resivore.slotreservations.network.ShulkerSelectionPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationNetworking {
    private ReservationNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ShulkerPanelReservationActionPayload.TYPE,
                ShulkerPanelReservationActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShulkerPanelContentActionPayload.TYPE,
                ShulkerPanelContentActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShulkerSelectionPayload.TYPE,
                ShulkerSelectionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShulkerPanelSyncPayload.TYPE,
                ShulkerPanelSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ReservationActionPayload.TYPE, ReservationActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                ReservationSnapshotRequestPayload.TYPE, ReservationSnapshotRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ReservationSnapshotPayload.TYPE, ReservationSnapshotPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ReservationActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleAction(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ReservationSnapshotRequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleSnapshotRequest(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ShulkerPanelReservationActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> ShulkerPanelActions.handleReservation(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ShulkerPanelContentActionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> ShulkerPanelActions.handleContent(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ShulkerSelectionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleSelection(context.player(), payload)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ShulkerSelectionTracker.clear(handler.getPlayer()));
    }

    static boolean handleSelection(ServerPlayer player, ShulkerSelectionPayload payload) {
        var host = ShulkerHostResolver.resolve(player, payload.menuId(), payload.host(), payload.hostFingerprint());
        if (host.isEmpty()) return false;
        if (payload.internalSlot() == -1) {
            ShulkerSelectionTracker.clear(player);
            return true;
        }
        return ShulkerSelectionTracker.select(player, host.orElseThrow(), payload.host(),
                payload.internalSlot(), payload.hostFingerprint());
    }

    static boolean handleAction(ServerPlayer player, ReservationActionPayload payload) {
        if (payload.source() == null) return false;
        Optional<ValidatedTarget> validated = validateTarget(
                player, payload.menuId(), payload.menuSlotIndex());
        if (validated.isEmpty()) return false;

        ValidatedTarget target = validated.orElseThrow();
        ItemStack physical = target.resolvedSlot().physicalStack();
        ItemStack carried = target.menu().getCarried();
        ReservationData current = ReservationStore.getData(target.resolvedSlot());
        ReservationTransition.Result transition = null;
        ItemStack displayedTemplate = ItemStack.EMPTY;

        switch (payload.source()) {
            case SLOT_STACK -> {
                if (physical.isEmpty()) return false;
                transition = ReservationTransition.fromOccupied(
                        current, target.resolvedSlot().localSlot(), physical);
                displayedTemplate = physical;
            }
            case CARRIED_STACK -> {
                if (!physical.isEmpty() || carried.isEmpty()) return false;
                if (SupportedContainerResolver.isShulkerOwner(target.resolvedSlot())
                        && !carried.getItem().canFitInsideContainerItems()) {
                    return false;
                }
                transition = ReservationTransition.fromCursor(
                        current, target.resolvedSlot().localSlot(), carried);
                displayedTemplate = carried;
            }
            case CLEAR_EMPTY -> {
                if (!physical.isEmpty() || !carried.isEmpty()) return false;
                transition = ReservationTransition.clear(current, target.resolvedSlot().localSlot());
                if (!transition.changed()) return false;
            }
        }

        if (transition == null || transition.outcome() == ReservationTransition.Outcome.REJECTED) return false;
        if (!transition.changed()) return true;
        if (payload.source() == ReservationActionPayload.Source.SLOT_STACK) {
            transition.attachIdentity(physical);
            target.resolvedSlot().owner().setChanged();
        } else if (payload.source() == ReservationActionPayload.Source.CARRIED_STACK) {
            transition.attachIdentity(carried);
            target.menu().setCarried(carried);
        }
        ReservationStore.setOwnerData(target.resolvedSlot().owner(), transition.data());
        if (transition.outcome() == ReservationTransition.Outcome.CLEARED) {
            player.sendOverlayMessage(Component.translatable(
                    "text.container_slot_reservations.cleared"));
        } else {
            player.sendOverlayMessage(Component.translatable(
                    "text.container_slot_reservations.reserved", displayedTemplate.getHoverName()));
        }

        target.menu().broadcastChanges();
        syncAllOpenSupportedMenus(player.level().getServer());
        return true;
    }

    static boolean handleSnapshotRequest(
            ServerPlayer player,
            ReservationSnapshotRequestPayload payload
    ) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!isUsableMenu(player, menu, payload.menuId()) || !isSupportedMenu(menu)) return false;
        sendSnapshot(player, menu);
        return true;
    }

    static Optional<ValidatedTarget> validateTarget(
            ServerPlayer player,
            int menuId,
            int menuSlotIndex
    ) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!isUsableMenu(player, menu, menuId)
                || !isSupportedMenu(menu)
                || menuSlotIndex < 0
                || menuSlotIndex >= menu.slots.size()) {
            return Optional.empty();
        }

        Slot slot = menu.slots.get(menuSlotIndex);
        if (slot.index != menuSlotIndex || !slot.isActive() || slot.isFake()) return Optional.empty();
        return SupportedContainerResolver.resolve(slot.container, slot.getContainerSlot())
                .map(resolved -> new ValidatedTarget(menu, slot, resolved));
    }

    private static boolean isUsableMenu(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int menuId
    ) {
        return player.isAlive()
                && !player.isSpectator()
                && menu.containerId == menuId
                && menu.stillValid(player);
    }

    private static void syncAllOpenSupportedMenus(MinecraftServer server) {
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            sendSnapshot(viewer, viewer.containerMenu);
        }
    }

    private static void sendSnapshot(ServerPlayer player, AbstractContainerMenu menu) {
        if (!isSupportedMenu(menu)
                || !ServerPlayNetworking.canSend(player, ReservationSnapshotPayload.TYPE)) {
            return;
        }
        List<ReservationSnapshotPayload.Entry> entries = snapshotEntries(menu);
        if (entries.isEmpty()) return;
        ServerPlayNetworking.send(player, new ReservationSnapshotPayload(menu.containerId, entries));
    }

    static List<ReservationSnapshotPayload.Entry> snapshotEntries(AbstractContainerMenu menu) {
        if (!isSupportedMenu(menu)) return List.of();
        List<ReservationSnapshotPayload.Entry> entries = new ArrayList<>();
        for (int menuIndex = 0; menuIndex < menu.slots.size(); menuIndex++) {
            Slot slot = menu.slots.get(menuIndex);
            if (slot.index != menuIndex || !slot.isActive() || slot.isFake()) continue;
            Optional<SupportedContainerResolver.ResolvedSlot> resolved =
                    SupportedContainerResolver.resolve(slot.container, slot.getContainerSlot());
            if (resolved.isEmpty()) continue;

            Optional<ItemStackTemplate> template = ReservationStore.get(resolved.orElseThrow())
                    .map(ItemStackTemplate::fromNonEmptyStack)
                    .map(value -> value.withCount(1));
            entries.add(new ReservationSnapshotPayload.Entry(menuIndex, template));
        }
        if (entries.size() > ReservationSnapshotPayload.MAX_ENTRIES) {
            throw new IllegalStateException("Supported menu exposed more than 54 reservation slots");
        }
        return List.copyOf(entries);
    }

    private static boolean isSupportedMenu(AbstractContainerMenu menu) {
        Class<?> type = menu.getClass();
        return type == ChestMenu.class
                || type == ShulkerBoxMenu.class
                || type == DispenserMenu.class
                || type == HopperMenu.class
                || type == FurnaceMenu.class
                || type == BlastFurnaceMenu.class
                || type == SmokerMenu.class
                || type == BrewingStandMenu.class
                || type == CrafterMenu.class;
    }

    record ValidatedTarget(
            AbstractContainerMenu menu,
            Slot slot,
            SupportedContainerResolver.ResolvedSlot resolvedSlot
    ) {
    }
}
