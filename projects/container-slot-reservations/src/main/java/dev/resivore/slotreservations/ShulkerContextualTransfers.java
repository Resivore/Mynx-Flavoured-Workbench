package dev.resivore.slotreservations;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Owns only recognized secondary-click shulker operations. */
public final class ShulkerContextualTransfers {
    private ShulkerContextualTransfers() {}

    public static boolean handle(Player player, AbstractContainerMenu menu, ClickAction action,
                                 Slot target, ItemStack slotStack, ItemStack carried) {
        if (action != ClickAction.SECONDARY || target == null) return false;
        boolean carriedShulker = carried.getCount() == 1
                && SupportedContainerResolver.isSupportedShulkerItem(carried);
        boolean slotShulker = slotStack.getCount() == 1
                && SupportedContainerResolver.isSupportedShulkerItem(slotStack);

        if (carriedShulker) {
            if (slotStack.isEmpty()) {
                extractSelected(player, menu, target, carried);
            } else {
                insertFromSlot(player, menu, target, carried);
            }
            return true;
        }
        if (slotShulker && !carried.isEmpty()) {
            insertFromCursor(player, menu, target, carried);
            return true;
        }
        return false;
    }

    private static void insertFromSlot(Player player, AbstractContainerMenu menu, Slot source, ItemStack shulker) {
        if (!ShulkerHostResolver.removableSource(player, source)) return;
        ItemStack incoming = source.getItem();
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(shulker, incoming);
        if (plan.moved() == 0) return;
        menu.setCarried(plan.shulker());
        source.setByPlayer(plan.remainder(), incoming);
        source.onTake(player, incoming.copyWithCount(plan.moved()));
        source.setChanged();
        source.container.setChanged();
        String fingerprint = ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess());
        ShulkerSelectionTracker.rebindCarried(player, menu, fingerprint,
                selectedAfterMutation(player, plan.contents()));
        menu.broadcastChanges();
    }

    private static void insertFromCursor(Player player, AbstractContainerMenu menu, Slot hostSlot, ItemStack carried) {
        var resolved = ShulkerHostResolver.resolveMenuSlot(player, menu, hostSlot);
        if (resolved.isEmpty()) return;
        ShulkerHostResolver.ResolvedHost host = resolved.orElseThrow();
        ShulkerTransferPlanner.Insertion plan = ShulkerTransferPlanner.planInsertion(host.stack(), carried);
        if (plan.moved() == 0) return;
        host.slot().set(plan.shulker());
        host.slot().setChanged();
        host.slot().container.setChanged();
        menu.setCarried(plan.remainder());
        String fingerprint = ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess());
        ShulkerSelectionTracker.rebind(player, host, fingerprint, selectedAfterMutation(player, plan.contents()));
        menu.broadcastChanges();
    }

    private static void extractSelected(Player player, AbstractContainerMenu menu, Slot target, ItemStack shulker) {
        ShulkerSelectionTracker.Selection selection = ShulkerSelectionTracker.validate(player);
        if (selection == null || selection.kind() != ShulkerSelectionTracker.HostKind.CARRIED_CURSOR
                || selection.menu() != menu) return;
        ItemStack physical = ShulkerContents.copy(shulker).get(selection.internalSlot());
        if (physical.isEmpty() || !target.getItem().isEmpty()
                || !ShulkerHostResolver.writableTarget(player, target, physical)) return;
        if (SupportedContainerResolver.resolve(target.container, target.getContainerSlot())
                .filter(resolved -> !ReservationStore.reservationAllows(resolved, physical)).isPresent()) return;
        int capacity = Math.min(target.getMaxStackSize(physical), physical.getMaxStackSize());
        ShulkerTransferPlanner.Extraction plan = ShulkerTransferPlanner.planExtraction(
                shulker, selection.internalSlot(), false, capacity);
        if (plan.moved() == 0) return;
        target.setByPlayer(plan.extracted(), ItemStack.EMPTY);
        target.setChanged();
        target.container.setChanged();
        menu.setCarried(plan.shulker());
        int selected = ShulkerContents.nextOccupied(plan.contents(), selection.internalSlot());
        String fingerprint = ShulkerHostFingerprint.of(plan.shulker(), player.registryAccess());
        ShulkerSelectionTracker.rebindCarried(player, menu, fingerprint, selected);
        menu.broadcastChanges();
    }

    private static int selectedAfterMutation(Player player, java.util.List<ItemStack> contents) {
        ShulkerSelectionTracker.Selection selection = ShulkerSelectionTracker.get(player).orElse(null);
        if (selection == null) return -1;
        return contents.get(selection.internalSlot()).isEmpty()
                ? ShulkerContents.nextOccupied(contents, selection.internalSlot()) : selection.internalSlot();
    }
}
