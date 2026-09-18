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
        CarriedShulkerRmbTrace.server("SERVER_PACKET_RECEIVED", "player=" + player.getName().getString()
                + " menuId=" + (action == null ? "null" : action.menuId()) + " sourceMenuSlot="
                + (action == null ? "null" : action.menuSlot()) + " physicalSlot="
                + (action == null ? "null" : action.physicalPlayerSlot()));
        if (action == null || action.carriedFingerprint() == null || action.sourceFingerprint() == null) {
            CarriedShulkerRmbTrace.server("SERVER_MENU_VALIDATE", "result=false reason=MALFORMED_PAYLOAD");
            return false;
        }
        AbstractContainerMenu menu = player.containerMenu;
        boolean usable = usable(player, menu, action.menuId());
        CarriedShulkerRmbTrace.server("SERVER_MENU_VALIDATE", "alive=" + player.isAlive()
                + " spectator=" + player.isSpectator() + " actualMenu=" + menu.getClass().getName()
                + " actualId=" + menu.containerId + " requestedId=" + action.menuId()
                + " stillValid=" + menu.stillValid(player) + " result=" + usable);
        if (!usable) return false;

        Optional<Slot> resolved = resolvePlayerInventorySlot(
                player, menu, action.menuSlot(), action.physicalPlayerSlot());
        if (resolved.isEmpty()) {
            CarriedShulkerRmbTrace.server("SERVER_SOURCE_RESOLVE", "result=false reason=NO_EXACT_LIVE_PLAYER_SLOT"
                    + " requestedMenuSlot=" + action.menuSlot() + " requestedPhysicalSlot=" + action.physicalPlayerSlot()
                    + " menuSize=" + menu.slots.size());
            return false;
        }
        Slot source = resolved.orElseThrow();
        CarriedShulkerRmbTrace.server("SERVER_SOURCE_RESOLVE", "result=true "
                + CarriedShulkerRmbTrace.slot(source, menu.slots.indexOf(source)) + " playerInventoryIdentity="
                + (source.container == player.getInventory()) + " nonEquipmentSize="
                + player.getInventory().getNonEquipmentItems().size());

        ItemStack carried = menu.getCarried();
        boolean cursorValid = carried.getCount() == 1 && SupportedContainerResolver.isSupportedShulkerItem(carried)
                && ShulkerHostFingerprint.of(carried, player.registryAccess())
                .equals(action.carriedFingerprint());
        CarriedShulkerRmbTrace.server("SERVER_CURSOR_VALIDATE", "cursor=" + CarriedShulkerRmbTrace.stack(carried)
                + " supported=" + SupportedContainerResolver.isSupportedShulkerItem(carried) + " liveFingerprint="
                + CarriedShulkerRmbTrace.shortFingerprint(ShulkerHostFingerprint.of(carried, player.registryAccess()))
                + " requestedFingerprint=" + CarriedShulkerRmbTrace.shortFingerprint(action.carriedFingerprint())
                + " exactMatch=" + cursorValid);
        if (!cursorValid) {
            return false;
        }

        ItemStack incoming = source.getItem();
        boolean removable = ShulkerHostResolver.removableSource(player, source);
        boolean sourceFingerprint = !incoming.isEmpty() && ShulkerHostFingerprint.of(incoming,
                player.registryAccess()).equals(action.sourceFingerprint());
        CarriedShulkerRmbTrace.server("SERVER_SOURCE_VALIDATE", "source=" + CarriedShulkerRmbTrace.stack(incoming)
                + " requestedFingerprint=" + CarriedShulkerRmbTrace.shortFingerprint(action.sourceFingerprint())
                + " fingerprintMatch=" + sourceFingerprint + " removable=" + removable
                + " active=" + source.isActive() + " fake=" + source.isFake()
                + " mayPickup=" + source.mayPickup(player) + " allowModification=" + player.mayBuild());
        if (!sourceFingerprint || !removable) {
            return false;
        }

        // The shared helper re-reads and plans against the same live authoritative stacks.
        // No source or cursor mutation occurs until the whole-shulker planner has succeeded.
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(carried, incoming);
        CarriedShulkerRmbTrace.server("SERVER_INSERT_PLAN", "sourceCount=" + incoming.getCount()
                + " moved=" + plan.moved() + " remainder=" + plan.remainder().getCount()
                + " reason=" + (plan.moved() == 0 ? "NO_MOVE" : "ACCEPTED"));
        if (plan.moved() == 0) return false;
        CarriedShulkerRmbTrace.server("SERVER_COMMIT_BEGIN", "sourceBefore=" + incoming.getCount()
                + " cursorBefore=" + CarriedShulkerRmbTrace.shortFingerprint(ShulkerHostFingerprint.of(carried, player.registryAccess())));
        boolean committed = ShulkerContextualTransfers.insertFromSlot(player, menu, source, carried);
        CarriedShulkerRmbTrace.server("SERVER_COMMIT_END", "committed=" + committed + " sourceAfter="
                + source.getItem().getCount() + " cursorAfter=" + CarriedShulkerRmbTrace.shortFingerprint(
                ShulkerHostFingerprint.of(menu.getCarried(), player.registryAccess())));
        CarriedShulkerRmbTrace.server("SERVER_SYNC_SENT", "path="
                + (player.hasInfiniteMaterials() && menu == player.inventoryMenu ? "broadcastFullState" : "broadcastChanges"));
        return committed;
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
