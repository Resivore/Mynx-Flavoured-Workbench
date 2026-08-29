package dev.resivore.carriedrouting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

final class ShulkerDestination implements RoutingDestination {
    private static final int SIZE = 27;
    private final ItemStack carrier;
    ShulkerDestination(ItemStack carrier) { this.carrier = carrier; }

    private NonNullList<ItemStack> contents() {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }
    @Override public boolean qualifies(ItemStack incoming) {
        if (RoutingLock.isLocked(carrier)) return false;
        NonNullList<ItemStack> items = contents();
        boolean match = items.stream().anyMatch(s -> !s.isEmpty() && ItemStack.isSameItemSameComponents(s, incoming));
        if (!match) return false;
        return items.stream().anyMatch(s -> s.isEmpty() || (ItemStack.isSameItemSameComponents(s, incoming) && s.getCount() < s.getMaxStackSize()));
    }
    @Override public int insert(ItemStack incoming) {
        if (incoming.isEmpty() || !qualifies(incoming)) return 0;
        NonNullList<ItemStack> items = contents();
        int before = incoming.getCount();
        for (ItemStack target : items) {
            if (incoming.isEmpty()) break;
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, incoming)) continue;
            int moved = Math.min(incoming.getCount(), target.getMaxStackSize() - target.getCount());
            if (moved > 0) { target.grow(moved); incoming.shrink(moved); }
        }
        for (int i = 0; i < items.size() && !incoming.isEmpty(); i++) {
            if (!items.get(i).isEmpty()) continue;
            int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
            items.set(i, incoming.split(moved));
        }
        int accepted = before - incoming.getCount();
        if (accepted > 0) carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return accepted;
    }
}
