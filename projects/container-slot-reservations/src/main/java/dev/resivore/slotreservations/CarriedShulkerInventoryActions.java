package dev.resivore.slotreservations;

import dev.resivore.slotreservations.network.CarriedShulkerInventoryActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Server-authoritative inventory-slot to cursor-held-shulker transactions. */
public final class CarriedShulkerInventoryActions {
    private CarriedShulkerInventoryActions() {}

    public static boolean handle(ServerPlayer player, CarriedShulkerInventoryActionPayload action) {
        if (action == null || action.carriedFingerprint() == null || action.sourceFingerprint() == null) {
            return false;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (!usable(player, menu, action.menuId())) return false;

        Optional<Slot> resolved = resolvePlayerInventorySlot(
                player, menu, action.menuSlot(), action.physicalPlayerSlot());
        if (resolved.isEmpty()) return false;
        Slot source = resolved.orElseThrow();

        ItemStack carried = menu.getCarried();
        if (carried.getCount() != 1 || !SupportedContainerResolver.isSupportedShulkerItem(carried)
                || !ShulkerHostFingerprint.of(carried, player.registryAccess())
                .equals(action.carriedFingerprint())) {
            return false;
        }

        ItemStack incoming = source.getItem();
        if (incoming.isEmpty()
                || !ShulkerHostFingerprint.of(incoming, player.registryAccess())
                .equals(action.sourceFingerprint())
                || !ShulkerHostResolver.removableSource(player, source)) {
            return false;
        }

        // The shared helper re-reads and plans against the same live authoritative stacks.
        // No source or cursor mutation occurs until the whole-shulker planner has succeeded.
        return ShulkerContextualTransfers.insertFromSlot(player, menu, source, carried);
    }

    private static Optional<Slot> resolvePlayerInventorySlot(
            ServerPlayer player,
            AbstractContainerMenu menu,
            int menuSlot,
            int physicalPlayerSlot
    ) {
        // Every ordinary screen supplies its exact live menu index. Creative's client-only
        // facade has no server-side menu index, so it deliberately uses -1 and the unique
        // physical player-inventory coordinate instead.
        if (menuSlot >= 0) {
            if (menuSlot >= menu.slots.size()) return Optional.empty();
            Slot source = menu.slots.get(menuSlot);
            return validPlayerSlot(player, source, menuSlot, physicalPlayerSlot)
                    ? Optional.of(source) : Optional.empty();
        }
        if (menuSlot != -1 || menu != player.inventoryMenu) return Optional.empty();

        Slot resolved = null;
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot candidate = menu.slots.get(index);
            if (validPlayerSlot(player, candidate, index, physicalPlayerSlot)) {
                if (resolved != null) return Optional.empty();
                resolved = candidate;
            }
        }
        return Optional.ofNullable(resolved);
    }

    private static boolean validPlayerSlot(
            ServerPlayer player,
            Slot slot,
            int menuSlot,
            int physicalPlayerSlot
    ) {
        return OrdinaryPlayerInventorySlots.isExactLiveSlot(
                player, slot, menuSlot, physicalPlayerSlot);
    }

    private static boolean usable(ServerPlayer player, AbstractContainerMenu menu, int menuId) {
        return player.isAlive() && !player.isSpectator()
                && menu.containerId == menuId && menu.stillValid(player);
    }
}
