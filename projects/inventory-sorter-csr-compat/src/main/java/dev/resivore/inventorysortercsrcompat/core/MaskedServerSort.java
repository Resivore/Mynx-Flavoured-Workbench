package dev.resivore.inventorysortercsrcompat.core;

import net.kyrptonaught.inventorysorter.inventory.container.ContainerStacks;
import net.kyrptonaught.inventorysorter.network.SortPriorityRuleSetting;
import net.kyrptonaught.inventorysorter.sort.SortedInventoryLayout;
import net.kyrptonaught.inventorysorter.sort.SortType;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Applies Inventory Sorter's own layout to only the physical slots that may move. */
public final class MaskedServerSort {
    private MaskedServerSort() {}

    /** @return true only when this compat layer replaced the upstream whole-range writeback. */
    public static boolean sortIfNeeded(
            Container container,
            int firstSlot,
            int slotCount,
            SortType sortType,
            String languageCode,
            List<SortPriorityRuleSetting> priorityRules,
            boolean sortIntoBundles
    ) {
        List<ItemStack> original = ContainerStacks.get(container, firstSlot, slotCount);
        List<ReservationFillPlan.SlotAccess> fillSlots = new ArrayList<>(original.size());
        for (int relative = 0; relative < original.size(); relative++) {
            int localSlot = firstSlot + relative;
            fillSlots.add(new ReservationFillPlan.SlotAccess() {
                @Override public ItemStack stack() { return container.getItem(localSlot); }
                @Override public boolean reserved() { return FixedSortSlots.isFixed(container, localSlot); }
                @Override public boolean mayInsert(ItemStack incoming) {
                    return ContainerSlotReservationsApi.mayInsert(container, localSlot, incoming);
                }
                @Override public int maxStackSize(ItemStack incoming) {
                    return container.getMaxStackSize(incoming);
                }
            });
        }
        ReservationFillPlan.Result filled = ReservationFillPlan.plan(fillSlots);
        for (int relative = 0; relative < filled.stacks().size(); relative++) {
            int localSlot = firstSlot + relative;
            ItemStack afterFill = filled.stacks().get(relative);
            if (!ItemStack.matches(container.getItem(localSlot), afterFill)) {
                container.setItem(localSlot, afterFill);
            }
        }

        List<Integer> movableIndices = new ArrayList<>(slotCount);
        List<ItemStack> movableStacks = new ArrayList<>(slotCount);
        boolean hasBundle = false;
        for (int relative = 0; relative < filled.stacks().size(); relative++) {
            int localSlot = firstSlot + relative;
            ItemStack physical = filled.stacks().get(relative);
            hasBundle |= FixedSortSlots.isBundle(physical);
            if (!FixedSortSlots.isFixed(container, localSlot)) {
                movableIndices.add(localSlot);
                movableStacks.add(physical);
            }
        }
        boolean suppressBundleInsertion = sortIntoBundles && hasBundle;
        if (movableIndices.size() == original.size() && !suppressBundleInsertion) return false;

        // Keep Inventory Sorter's normal layout and merging rules.  Only its optional
        // bundle-content insertion pass is disabled; bundles themselves stay in the movable pool.
        List<ItemStack> sorted = SortedInventoryLayout.from(
                movableStacks, sortType, languageCode, priorityRules,
                suppressBundleInsertion ? false : sortIntoBundles).stacks();
        boolean changed = filled.changed();
        for (int i = 0; i < movableIndices.size(); i++) {
            ItemStack before = container.getItem(movableIndices.get(i));
            ItemStack after = sorted.get(i);
            if (!ItemStack.matches(before, after)) {
                container.setItem(movableIndices.get(i), after);
                changed = true;
            }
        }
        if (changed) container.setChanged();
        return true;
    }
}
