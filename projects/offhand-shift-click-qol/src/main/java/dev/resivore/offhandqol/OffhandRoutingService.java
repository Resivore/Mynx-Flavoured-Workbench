package dev.resivore.offhandqol;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class OffhandRoutingService {
    private OffhandRoutingService() {}

    public static boolean routeIncoming(
            Player player,
            ItemStack incoming,
            int excludedInventorySlot,
            boolean excludeOffhand
    ) {
        if (incoming.isEmpty()) return false;
        int before = incoming.getCount();
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();

        merge(inventory, incoming, selected, excludedInventorySlot);
        mergeRange(inventory, incoming, 0,
                Math.min(Inventory.SELECTION_SIZE, inventory.getNonEquipmentItems().size()),
                selected, excludedInventorySlot);
        if (!excludeOffhand) mergeMatchingOccupiedOffhand(player, incoming);
        placeRange(inventory, incoming, 0,
                Math.min(Inventory.SELECTION_SIZE, inventory.getNonEquipmentItems().size()),
                -1, excludedInventorySlot);
        routeTier(inventory, incoming, Inventory.SELECTION_SIZE,
                inventory.getNonEquipmentItems().size(), -1, excludedInventorySlot);

        if (before != incoming.getCount()) inventory.setChanged();
        return before != incoming.getCount();
    }

    public static boolean routePlayerOriginToMatchingOffhand(
            Player player,
            ItemStack incoming,
            boolean excludeOffhand
    ) {
        if (excludeOffhand || incoming.isEmpty()) return false;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || !ItemStack.isSameItemSameComponents(offhand, incoming)) return false;
        int moved = Math.min(incoming.getCount(), offhand.getMaxStackSize() - offhand.getCount());
        if (moved <= 0) return false;
        offhand.grow(moved);
        incoming.shrink(moved);
        player.getInventory().setChanged();
        return true;
    }

    private static void routeTier(Inventory inventory, ItemStack incoming, int start, int end,
                                  int skippedTierSlot, int excludedInventorySlot) {
        mergeRange(inventory, incoming, start, end, skippedTierSlot, excludedInventorySlot);
        placeRange(inventory, incoming, start, end, skippedTierSlot, excludedInventorySlot);
    }

    private static void mergeRange(Inventory inventory, ItemStack incoming, int start, int end,
                                   int skippedTierSlot, int excludedInventorySlot) {
        for (int i = start; i < end && !incoming.isEmpty(); i++) {
            if (i != skippedTierSlot && i != excludedInventorySlot) merge(inventory, incoming, i);
        }
    }

    private static void placeRange(Inventory inventory, ItemStack incoming, int start, int end,
                                   int skippedTierSlot, int excludedInventorySlot) {
        for (int i = start; i < end && !incoming.isEmpty(); i++) {
            if (i != skippedTierSlot && i != excludedInventorySlot) place(inventory, incoming, i);
        }
    }

    private static boolean merge(Inventory inventory, ItemStack incoming, int slot, int excludedInventorySlot) {
        if (slot < 0 || slot >= inventory.getNonEquipmentItems().size() || slot == excludedInventorySlot) return false;
        return merge(inventory, incoming, slot);
    }

    private static boolean merge(Inventory inventory, ItemStack incoming, int slot) {
        ItemStack target = inventory.getItem(slot);
        if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, incoming)) return false;
        int moved = Math.min(incoming.getCount(), target.getMaxStackSize() - target.getCount());
        if (moved <= 0) return false;
        target.grow(moved);
        incoming.shrink(moved);
        return true;
    }

    private static boolean place(Inventory inventory, ItemStack incoming, int slot) {
        if (incoming.isEmpty() || !inventory.getItem(slot).isEmpty()) return false;
        int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
        inventory.setItem(slot, incoming.split(moved));
        return moved > 0;
    }

    private static boolean mergeMatchingOccupiedOffhand(Player player, ItemStack incoming) {
        if (incoming.isEmpty()) return false;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || !ItemStack.isSameItemSameComponents(offhand, incoming)) return false;
        int moved = Math.min(incoming.getCount(), offhand.getMaxStackSize() - offhand.getCount());
        if (moved > 0) {
            offhand.grow(moved);
            incoming.shrink(moved);
        }
        return moved > 0;
    }
}
