package dev.resivore.carriedrouting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class RoutingService {
    private RoutingService() {}
    public static int routeIncomingStack(Player player, ItemStack incoming, RoutingContext context) {
        return routeIncomingStack(player, incoming, context, -1, false);
    }

    public static int routeIncomingStack(Player player, ItemStack incoming, RoutingContext context,
                                         int excludedInventorySlot, boolean excludeOffhand) {
        return routeIncomingStackWithResult(player, incoming, context, excludedInventorySlot, excludeOffhand)
                .totalItemsMoved();
    }

    /**
     * Keeps the established total-moved result while separately reporting the portion that
     * actually entered carried containers. WORLD_PICKUP audio uses only the latter.
     */
    public static RoutingResult routeIncomingStackWithResult(Player player, ItemStack incoming, RoutingContext context,
                                                              int excludedInventorySlot, boolean excludeOffhand) {
        if ((context == RoutingContext.WORLD_PICKUP && player.level().isClientSide()) || incoming.isEmpty()) {
            return RoutingResult.NONE;
        }
        int before = incoming.getCount();
        Inventory inventory = player.getInventory();

        int selected = inventory.getSelectedSlot();
        mergeInventorySlot(inventory, incoming, selected, excludedInventorySlot);

        // Existing matching destinations always precede ordinary empty slots.
        int beforeCarriedContainers = incoming.getCount();
        routeCarriedContainers(player, inventory, incoming, excludedInventorySlot, excludeOffhand);
        int carriedContainerItemsMoved = beforeCarriedContainers - incoming.getCount();

        mergeInventoryRange(inventory, incoming, 0,
                Math.min(Inventory.SELECTION_SIZE, inventory.getNonEquipmentItems().size()),
                selected, excludedInventorySlot);

        if (!excludeOffhand) mergeMatchingOccupiedOffhand(player, incoming);

        routeOrdinaryInventoryFallbacks(inventory, incoming, context, excludedInventorySlot);
        if (before != incoming.getCount()) inventory.setChanged();
        return new RoutingResult(before - incoming.getCount(), carriedContainerItemsMoved);
    }

    static void routeOrdinaryInventoryFallbacks(
            Inventory inventory,
            ItemStack incoming,
            RoutingContext context,
            int excludedInventorySlot
    ) {
        boolean prioritizeOccupiedStorage = context == RoutingContext.WORLD_PICKUP
                || context == RoutingContext.QUICK_MOVE;
        if (prioritizeOccupiedStorage) {
            // New world pickups and external-container QUICK_MOVE must exhaust
            // the complete live ordinary-storage merge domain before any new
            // ordinary player stack is opened.
            mergeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,
                    inventory.getNonEquipmentItems().size(), -1, excludedInventorySlot);
        }

        // Once all context-specific occupied destinations have had their turn,
        // preserve the accepted physical-hotbar empty-slot fallback.
        placeInventoryRange(inventory, incoming, 0,
                Math.min(Inventory.SELECTION_SIZE, inventory.getNonEquipmentItems().size()),
                -1, excludedInventorySlot);

        if (prioritizeOccupiedStorage) {
            placeInventoryRange(inventory, incoming, Inventory.SELECTION_SIZE,
                    inventory.getNonEquipmentItems().size(), -1, excludedInventorySlot);
        } else {
            // Compatibility-only transfers retain their established
            // empty-hotbar-before-storage behavior.
            routeInventoryTier(inventory, incoming, Inventory.SELECTION_SIZE,
                    inventory.getNonEquipmentItems().size(), -1, excludedInventorySlot);
        }
    }

    public static boolean routePlayerOriginSpecialDestinations(
            Player player,
            ItemStack incoming,
            int excludedInventorySlot,
            boolean excludeOffhand
    ) {
        if (incoming.isEmpty()) return false;
        int before = incoming.getCount();
        if (!excludeOffhand) mergeMatchingOccupiedOffhand(player, incoming);
        routeCarriedContainers(
                player,
                player.getInventory(),
                incoming,
                excludedInventorySlot,
                excludeOffhand
        );
        if (before != incoming.getCount()) player.getInventory().setChanged();
        return before != incoming.getCount();
    }

    private static void routeCarriedContainers(
            Player player,
            Inventory inventory,
            ItemStack incoming,
            int excludedInventorySlot,
            boolean excludeOffhand
    ) {
        // Preserve the already-runtime-passing stable physical inventory order.
        for (int i = 0; i < inventory.getNonEquipmentItems().size() && !incoming.isEmpty(); i++) {
            if (i == excludedInventorySlot) continue;
            RoutingDestination destination = destination(inventory.getItem(i));
            if (destination != null && destination.qualifies(incoming)) destination.insert(incoming);
        }
        if (!excludeOffhand && !incoming.isEmpty()) {
            RoutingDestination destination = destination(player.getOffhandItem());
            if (destination != null && destination.qualifies(incoming)) destination.insert(incoming);
        }
    }

    private static void routeInventoryTier(Inventory inventory, ItemStack incoming, int start, int end,
                                           int skippedTierSlot, int excludedInventorySlot) {
        mergeInventoryRange(inventory, incoming, start, end, skippedTierSlot, excludedInventorySlot);
        placeInventoryRange(inventory, incoming, start, end, skippedTierSlot, excludedInventorySlot);
    }

    static void mergeInventoryRange(Inventory inventory, ItemStack incoming, int start, int end,
                                    int skippedTierSlot, int excludedInventorySlot) {
        for (int i = start; i < end && !incoming.isEmpty(); i++) {
            if (i == skippedTierSlot || i == excludedInventorySlot) continue;
            mergeInventorySlot(inventory, incoming, i);
        }
    }

    private static void placeInventoryRange(Inventory inventory, ItemStack incoming, int start, int end,
                                            int skippedTierSlot, int excludedInventorySlot) {
        for (int i = start; i < end && !incoming.isEmpty(); i++) {
            if (i == skippedTierSlot || i == excludedInventorySlot) continue;
            placeInventorySlot(inventory, incoming, i);
        }
    }

    private static boolean mergeInventorySlot(Inventory inventory, ItemStack incoming, int slot,
                                              int excludedInventorySlot) {
        if (slot < 0 || slot >= inventory.getNonEquipmentItems().size() || slot == excludedInventorySlot) return false;
        return mergeInventorySlot(inventory, incoming, slot);
    }

    private static boolean mergeInventorySlot(Inventory inventory, ItemStack incoming, int slot) {
        ItemStack target = inventory.getItem(slot);
        if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, incoming)) return false;
        int moved = Math.min(incoming.getCount(), target.getMaxStackSize() - target.getCount());
        if (moved <= 0) return false;
        target.grow(moved);
        incoming.shrink(moved);
        return true;
    }

    private static boolean placeInventorySlot(Inventory inventory, ItemStack incoming, int slot) {
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

    public static boolean isSupported(ItemStack stack) { return destination(stack) != null; }

    public record RoutingResult(int totalItemsMoved, int carriedContainerItemsMoved) {
        public static final RoutingResult NONE = new RoutingResult(0, 0);

        public boolean routedToCarriedContainer() { return carriedContainerItemsMoved > 0; }
    }

    private static RoutingDestination destination(ItemStack stack) {
        if (stack.getItem() instanceof BundleItem) return new BundleDestination(stack);
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) return new ShulkerDestination(stack);
        return null;
    }
}
