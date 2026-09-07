package tempeststudios.quickstacknearby;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import java.util.function.BooleanSupplier;

/** One-level transactional destination used by C10's QSN routing path. */
final class NestedShulkerTarget extends SimpleContainer {
    private final Container parent;
    private final int hostSlot;
    private final ItemStack host;
    private ItemStack expected;
    private final BooleanSupplier validParent;

    NestedShulkerTarget(Container parent, int hostSlot, BooleanSupplier validParent) {
        super(27);
        this.parent = parent;
        this.hostSlot = hostSlot;
        this.host = parent.getItem(hostSlot);
        this.expected = host.copy();
        this.validParent = validParent;
        NonNullList<ItemStack> contents = contents(host);
        for (int slot = 0; slot < 27; slot++) super.setItem(slot, contents.get(slot));
    }

    static boolean supported(ItemStack stack) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof BlockItem item)) return false;
        return item.getBlock() == Blocks.SHULKER_BOX || Blocks.DYED_SHULKER_BOX.asList().contains(item.getBlock());
    }

    boolean accepts(ItemStack incoming) {
        if (incoming.isEmpty() || !incoming.getItem().canFitInsideContainerItems() || !live()) return false;
        NonNullList<ItemStack> contained = contents(host);
        for (int slot = 0; slot < contained.size(); slot++) {
            ItemStack stack = contained.get(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, incoming)) return true;
            if (stack.isEmpty() && CsrRoutingCompat.available() && nestedEmptyMatches(slot, incoming, true)) return true;
        }
        return false;
    }

    int insert(ItemStack incoming) {
        if (!accepts(incoming)) return 0;
        NonNullList<ItemStack> planned = contents(host);
        int remaining = incoming.getCount();
        // Existing physical stacks first.  C9's CSR priority is then applied to empty nested slots.
        for (int pass = 0; pass < 3 && remaining > 0; pass++) {
            for (int slot = 0; slot < 27 && remaining > 0; slot++) {
                ItemStack current = planned.get(slot);
                boolean allowed = pass == 0 ? !current.isEmpty() && ItemStack.isSameItemSameComponents(current, incoming)
                        : current.isEmpty() && nestedEmptyMatches(slot, incoming, pass == 1);
                if (!allowed) continue;
                int capacity = Math.min(incoming.getMaxStackSize(), getMaxStackSize(incoming));
                int amount = Math.min(remaining, Math.max(0, capacity - current.getCount()));
                if (amount == 0) continue;
                planned.set(slot, incoming.copyWithCount(current.getCount() + amount));
                remaining -= amount;
            }
        }
        int moved = incoming.getCount() - remaining;
        if (moved == 0 || !live()) return 0;
        Object before = host.get(DataComponents.CONTAINER);
        try {
            host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(planned));
            parent.setChanged();
            expected = host.copy();
            for (int slot = 0; slot < 27; slot++) super.setItem(slot, planned.get(slot));
            incoming.shrink(moved);
            return moved;
        } catch (RuntimeException failure) {
            if (before == null) host.remove(DataComponents.CONTAINER);
            else host.set(DataComponents.CONTAINER, (ItemContainerContents) before);
            QuickStackNearby.LOGGER.error("Nested shulker writeback rejected; source remainder preserved", failure);
            return 0;
        }
    }

    private boolean nestedEmptyMatches(int slot, ItemStack incoming, boolean reservedPass) {
        // CSR's ItemStack API deliberately remains optional.  Without it, physical matches above
        // retain their C9 behavior and ordinary legal empties remain available afterwards.
        if (!CsrRoutingCompat.available()) return !reservedPass;
        try {
            Class<?> api = Class.forName("dev.resivore.slotreservations.api.ContainerSlotReservationsApi");
            Object classification = api.getMethod("classify", ItemStack.class, int.class, ItemStack.class)
                    .invoke(null, host, slot, incoming);
            return reservedPass ? "RESERVED_MATCH".equals(String.valueOf(classification))
                    : "UNRESERVED_EMPTY".equals(String.valueOf(classification));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Loaded CSR lacks its audited nested reservation API", e);
        }
    }

    private boolean live() {
        return validParent.getAsBoolean() && hostSlot >= 0 && hostSlot < parent.getContainerSize()
                && parent.getItem(hostSlot) == host && supported(host) && ItemStack.matches(host, expected)
                && parent.canPlaceItem(hostSlot, host);
    }

    private static NonNullList<ItemStack> contents(ItemStack stack) {
        NonNullList<ItemStack> result = NonNullList.withSize(27, ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(result);
        return result;
    }
}
