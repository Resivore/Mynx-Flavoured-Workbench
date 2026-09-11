package dev.resivore.inventorysortercsrcompat.core;

import net.kyrptonaught.inventorysorter.client.sort.ClientSortScope;
import net.kyrptonaught.inventorysorter.client.sort.plan.ClientSortClickPlanner;
import net.kyrptonaught.inventorysorter.client.sort.plan.PlannedContainerClick;
import net.kyrptonaught.inventorysorter.client.sort.plan.SlotState;
import net.kyrptonaught.inventorysorter.network.SortPriorityRuleSetting;
import net.kyrptonaught.inventorysorter.sort.SortedInventoryLayout;
import net.kyrptonaught.inventorysorter.sort.SortType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Builds upstream click plans from a compact list that excludes every fixed menu slot. */
public final class MaskedClientFallbackSort {
    private MaskedClientFallbackSort() {}

    public static Optional<List<PlannedContainerClick>> planIfNeeded(
            ClientSortScope scope,
            ClientSortClickPlanner clickPlanner,
            SortType sortType,
            String languageCode,
            List<SortPriorityRuleSetting> priorityRules
    ) {
        List<ClientSortScope.ScopedSlot> movable = new ArrayList<>(scope.slots().size());
        for (ClientSortScope.ScopedSlot scopedSlot : scope.slots()) {
            if (!FixedSortSlots.isFixed(scopedSlot.slot())) movable.add(scopedSlot);
        }
        if (movable.size() == scope.slots().size()) return null;

        List<ItemStack> before = new ArrayList<>(movable.size());
        List<SlotState> states = new ArrayList<>(movable.size());
        for (ClientSortScope.ScopedSlot scopedSlot : movable) {
            ItemStack stack = scopedSlot.slot().getItem();
            before.add(stack);
            states.add(new SlotState(scopedSlot.menuSlotIndex(), stack.copy()));
        }
        // Do not invoke upstream bundle-target insertion for a masked sort: every bundle is a
        // fixed obstacle, including potential external target bundles in this screen scope.
        List<ItemStack> sorted = SortedInventoryLayout.from(
                before, sortType, languageCode, priorityRules).stacks();
        return clickPlanner.plan(states, sorted);
    }
}
