package dev.resivore.villagerwork;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.function.BiConsumer;

/** Only inserts: matching component-identical stacks first, then permitted empty slots. */
public final class OutputStorage {
    private OutputStorage() {}

    public static boolean fits(Container container, ItemStack sample, int count) {
        int room = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.canPlaceItem(i, sample)) continue;
            ItemStack present = container.getItem(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameComponents(present, sample)) {
                room += Math.max(0, Math.min(container.getMaxStackSize(sample), present.getMaxStackSize()) - present.getCount());
            } else if (present.isEmpty()) {
                room += Math.min(container.getMaxStackSize(sample), sample.getMaxStackSize());
            }
            if (room >= count) return true;
        }
        return false;
    }

    public static int insert(Container container, ItemStack source, int requested) {
        int matched = insertPass(container, source, requested, false);
        return matched + insertPass(container, source, requested - matched, true);
    }

    /** All matching stacks across eligible containers precede any empty slot; input order breaks ties. */
    public static int insertAcross(List<? extends Container> containers, ItemStack source, int requested) {
        return insertAcross(containers, source, requested, (container, moved) -> {});
    }

    /**
     * Inserts with the same matching-stack-before-empty-slot ordering as {@link #insertAcross(List,
     * ItemStack, int)}, reporting each container that actually accepted items.
     */
    public static int insertAcross(List<? extends Container> containers, ItemStack source, int requested,
                                   BiConsumer<Container, Integer> accepted) {
        if (source.isEmpty() || requested <= 0) return 0;
        int remaining = Math.min(requested, source.getCount());
        for (boolean emptyOnly : new boolean[] {false, true}) {
            for (Container container : containers) {
                int moved = insertPass(container, source, remaining, emptyOnly);
                if (moved > 0) accepted.accept(container, moved);
                remaining -= moved;
                if (remaining == 0) return Math.min(requested, source.getCount());
            }
        }
        return Math.min(requested, source.getCount()) - remaining;
    }

    public static int insertMatching(Container container, ItemStack source, int requested) {
        return insertPass(container, source, requested, false);
    }

    public static int insertEmpty(Container container, ItemStack source, int requested) {
        return insertPass(container, source, requested, true);
    }

    private static int insertPass(Container container, ItemStack source, int requested, boolean emptyOnly) {
        if (source.isEmpty() || requested <= 0) return 0;
        int remaining = Math.min(requested, source.getCount());
        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                if (!container.canPlaceItem(i, source)) continue;
                ItemStack present = container.getItem(i);
                if (!emptyOnly && !present.isEmpty() && ItemStack.isSameItemSameComponents(present, source)) {
                    int amount = Math.min(remaining, Math.max(0, Math.min(container.getMaxStackSize(source), present.getMaxStackSize()) - present.getCount()));
                    if (amount > 0) { present.grow(amount); container.setChanged(); remaining -= amount; }
                } else if (emptyOnly && present.isEmpty()) {
                    int amount = Math.min(remaining, Math.min(container.getMaxStackSize(source), source.getMaxStackSize()));
                    ItemStack placed = source.copy();
                    placed.setCount(amount);
                    container.setItem(i, placed);
                    remaining -= amount;
                }
        }
        return Math.min(requested, source.getCount()) - remaining;
    }
}
