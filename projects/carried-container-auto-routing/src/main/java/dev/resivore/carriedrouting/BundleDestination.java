package dev.resivore.carriedrouting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

final class BundleDestination implements RoutingDestination {
    private final ItemStack carrier;
    BundleDestination(ItemStack carrier) { this.carrier = carrier; }
    @Override public boolean qualifies(ItemStack incoming) {
        BundleContents contents = carrier.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (RoutingLock.isLocked(carrier)
                || !contents.itemCopyStream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, incoming))
                || !BundleContents.canItemBeInBundle(incoming)) return false;
        return new BundleContents.Mutable(contents).tryInsert(incoming.copy()) > 0;
    }
    @Override public int insert(ItemStack incoming) {
        if (incoming.isEmpty() || !qualifies(incoming)) return 0;
        BundleContents.Mutable mutable = new BundleContents.Mutable(carrier.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
        int accepted = mutable.tryInsert(incoming);
        if (accepted > 0) carrier.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
        return accepted;
    }
}
