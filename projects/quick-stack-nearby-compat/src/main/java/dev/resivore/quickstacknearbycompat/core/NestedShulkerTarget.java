package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import java.util.function.BooleanSupplier;

/** A one-level destination; its copied contents never become world slots. */
public final class NestedShulkerTarget extends SimpleContainer {
    private final Container parent;
    private final int hostSlot;
    private final ItemStack host;
    private ItemStack expected;
    private final BooleanSupplier validParent;
    public NestedShulkerTarget(Container parent, int hostSlot, BooleanSupplier validParent) {
        super(27); this.parent = parent; this.hostSlot = hostSlot; this.validParent = validParent;
        this.host = parent.getItem(hostSlot); this.expected = host.copy();
        NonNullList<ItemStack> contents = contents(host);
        for (int slot = 0; slot < 27; slot++) super.setItem(slot, contents.get(slot));
    }
    public static boolean supported(ItemStack stack) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof BlockItem item)) return false;
        return item.getBlock() == Blocks.SHULKER_BOX || Blocks.DYED_SHULKER_BOX.asList().contains(item.getBlock());
    }
    private static NonNullList<ItemStack> contents(ItemStack host) {
        NonNullList<ItemStack> result = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(result); return result;
    }
    public boolean live() {
        return hostSlot >= 0 && hostSlot < parent.getContainerSize() && validParent.getAsBoolean()
                && parent.getItem(hostSlot) == host && supported(host) && ItemStack.matches(host, expected)
                && parent.canPlaceItem(hostSlot, host);
    }
    public ItemStack hostIdentity() { return host; }
    public boolean accepts(ItemStack incoming) {
        if (incoming.isEmpty() || !incoming.getItem().canFitInsideContainerItems() || !live()) return false;
        var physical = contents(host);
        for (int slot = 0; slot < 27; slot++) {
            ItemStack item = physical.get(slot);
            // Full physical matches still establish affinity; write admission is checked separately.
            if (!item.isEmpty() && ItemStack.isSameItemSameComponents(item, incoming)) return true;
            if (item.isEmpty() && CsrReservationResolver.classify(host, slot, incoming, item)
                    == CsrReservationResolver.NestedClass.RESERVED_MATCH) return true;
        }
        return false;
    }
    /** Plan first, validate again, write CONTAINER once, then debit the exact committed count. */
    public int insert(ItemStack incoming) {
        try { return insertValidated(incoming); }
        catch (RuntimeException failure) {
            // Earlier targets may already have committed to QSN's working source stack.
            // Return normally so upstream performs its exact accumulated source debit.
            org.slf4j.LoggerFactory.getLogger(NestedShulkerTarget.class).error("Nested shulker planning rejected; source remainder preserved", failure);
            return 0;
        }
    }
    private int insertValidated(ItemStack incoming) {
        if (!accepts(incoming)) return 0;
        var physical = contents(host);
        int remainder = incoming.getCount();
        for (int pass = 0; pass < 3 && remainder > 0; pass++) {
            for (int slot = 0; slot < 27 && remainder > 0; slot++) {
                ItemStack existing = physical.get(slot);
                var classification = CsrReservationResolver.classify(host, slot, incoming, existing);
                boolean allowed = pass == 0 ? !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, incoming)
                        && classification != CsrReservationResolver.NestedClass.BLOCKED
                        : existing.isEmpty() && classification == (pass == 1
                        ? CsrReservationResolver.NestedClass.RESERVED_MATCH : CsrReservationResolver.NestedClass.UNRESERVED_EMPTY);
                if (!allowed) continue;
                int maximum = Math.min(incoming.getMaxStackSize(), getMaxStackSize(incoming));
                int moved = Math.min(remainder, Math.max(0, maximum - existing.getCount()));
                if (moved == 0) continue;
                physical.set(slot, incoming.copyWithCount(existing.getCount() + moved)); remainder -= moved;
            }
        }
        int moved = incoming.getCount() - remainder;
        if (moved == 0 || !live()) return 0;
        var before = host.get(DataComponents.CONTAINER);
        var updated = ItemContainerContents.fromItems(physical);
        try {
            host.set(DataComponents.CONTAINER, updated);
            parent.setChanged();
        } catch (RuntimeException failure) {
            if (before == null) host.remove(DataComponents.CONTAINER); else host.set(DataComponents.CONTAINER, before);
            org.slf4j.LoggerFactory.getLogger(NestedShulkerTarget.class).error("Nested shulker writeback rejected; source preserved", failure);
            return 0;
        }
        expected = host.copy();
        for (int slot = 0; slot < 27; slot++) super.setItem(slot, physical.get(slot));
        incoming.shrink(moved);
        return moved;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack incoming) {
        return slot >= 0 && slot < 27 && incoming.getItem().canFitInsideContainerItems() && live()
                && CsrReservationResolver.classify(host, slot, incoming, getItem(slot)) != CsrReservationResolver.NestedClass.BLOCKED;
    }
}
