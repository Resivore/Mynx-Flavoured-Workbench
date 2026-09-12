package dev.resivore.inventorysortercsrcompat.core;

import net.kyrptonaught.inventorysorter.client.sort.ClientSortScope;
import net.kyrptonaught.inventorysorter.client.sort.plan.ClientSortClickPlanner;
import net.kyrptonaught.inventorysorter.client.sort.plan.PlannedContainerClick;
import net.kyrptonaught.inventorysorter.client.sort.plan.SlotState;
import net.kyrptonaught.inventorysorter.network.SortPriorityRuleSetting;
import net.kyrptonaught.inventorysorter.sort.SortedInventoryLayout;
import net.kyrptonaught.inventorysorter.sort.SortType;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Builds ordinary upstream click plans while excluding CSR-fixed slots and bundle insertion. */
public final class MaskedClientFallbackSort {
    private MaskedClientFallbackSort() {}

    public static Optional<List<PlannedContainerClick>> planIfNeeded(
            ClientSortScope scope,
            ClientSortClickPlanner clickPlanner,
            SortType sortType,
            String languageCode,
            List<SortPriorityRuleSetting> priorityRules
    ) {
        List<ReservationFillPlan.SlotAccess> fillSlots = new ArrayList<>(scope.slots().size());
        for (ClientSortScope.ScopedSlot scopedSlot : scope.slots()) {
            Slot slot = scopedSlot.slot();
            fillSlots.add(new ReservationFillPlan.SlotAccess() {
                @Override public ItemStack stack() { return slot.getItem(); }
                @Override public boolean reserved() { return FixedSortSlots.isFixed(slot); }
                @Override public boolean mayInsert(ItemStack incoming) {
                    return ContainerSlotReservationsApi.mayInsert(
                            slot.container, slot.getContainerSlot(), incoming) && slot.mayPlace(incoming);
                }
                @Override public int maxStackSize(ItemStack incoming) {
                    return slot.getMaxStackSize(incoming);
                }
            });
        }
        ReservationFillPlan.Result filled = ReservationFillPlan.plan(fillSlots);

        List<ClientSortScope.ScopedSlot> movable = new ArrayList<>(scope.slots().size());
        List<Integer> movableIndices = new ArrayList<>(scope.slots().size());
        boolean hasBundle = false;
        for (int index = 0; index < scope.slots().size(); index++) {
            ClientSortScope.ScopedSlot scopedSlot = scope.slots().get(index);
            hasBundle |= FixedSortSlots.isBundle(filled.stacks().get(index));
            if (!FixedSortSlots.isFixed(scopedSlot.slot())) {
                movable.add(scopedSlot);
                movableIndices.add(index);
            }
        }
        boolean hasFixedSlot = movable.size() != scope.slots().size();
        if (!hasFixedSlot && !hasBundle) return null;

        List<ItemStack> before = new ArrayList<>(movable.size());
        List<SlotState> states = new ArrayList<>(movable.size());
        for (int index = 0; index < movable.size(); index++) {
            ClientSortScope.ScopedSlot scopedSlot = movable.get(index);
            ItemStack stack = filled.stacks().get(movableIndices.get(index));
            before.add(stack);
            states.add(new SlotState(scopedSlot.menuSlotIndex(), stack.copy()));
        }
        // The fallback uses the same ordinary layout as upstream's no-bundle mode.  It never
        // produces bundle-target clicks, but includes every unreserved bundle as a movable stack.
        List<ItemStack> sorted = SortedInventoryLayout.from(
                before, sortType, languageCode, priorityRules).stacks();
        Optional<List<PlannedContainerClick>> sortedClicks = clickPlanner.plan(states, sorted);
        if (sortedClicks.isEmpty()) return Optional.empty();

        List<PlannedContainerClick> clicks = new ArrayList<>();
        for (ReservationFillPlan.Transfer transfer : filled.transfers()) {
            int donorMenuSlot = scope.slots().get(transfer.donorIndex()).menuSlotIndex();
            int targetMenuSlot = scope.slots().get(transfer.targetIndex()).menuSlotIndex();
            clicks.add(new PlannedContainerClick(donorMenuSlot, 0, ContainerInput.PICKUP));
            clicks.add(new PlannedContainerClick(targetMenuSlot, 0, ContainerInput.PICKUP));
            if (transfer.returnsRemainder()) {
                clicks.add(new PlannedContainerClick(donorMenuSlot, 0, ContainerInput.PICKUP));
            }
        }
        clicks.addAll(sortedClicks.orElseThrow());
        return Optional.of(List.copyOf(clicks));
    }
}
