package dev.resivore.inventorysortercsrcompat.core;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plans the reservation-only phase which precedes Inventory Sorter's ordinary masked layout.
 * Callers provide slots in physical order. Reserved slots are destinations only; unreserved
 * slots are donors only.
 */
public final class ReservationFillPlan {
    private ReservationFillPlan() {}

    public static Result plan(List<? extends SlotAccess> slots) {
        Objects.requireNonNull(slots, "slots");
        List<ItemStack> simulated = new ArrayList<>(slots.size());
        for (SlotAccess slot : slots) simulated.add(Objects.requireNonNull(slot.stack(), "stack").copy());

        List<Transfer> transfers = new ArrayList<>();
        for (int targetIndex = 0; targetIndex < slots.size(); targetIndex++) {
            SlotAccess target = slots.get(targetIndex);
            if (!target.reserved()) continue;

            for (int donorIndex = 0; donorIndex < slots.size(); donorIndex++) {
                SlotAccess donor = slots.get(donorIndex);
                if (donor.reserved()) continue;

                ItemStack donorStack = simulated.get(donorIndex);
                if (donorStack.isEmpty() || !target.mayInsert(donorStack)) continue;

                ItemStack targetStack = simulated.get(targetIndex);
                if (!targetStack.isEmpty()
                        && !ItemStack.isSameItemSameComponents(targetStack, donorStack)) continue;

                int maxStackSize = Math.min(
                        donorStack.getMaxStackSize(),
                        Math.max(0, target.maxStackSize(donorStack))
                );
                int availableSpace = maxStackSize - targetStack.getCount();
                if (availableSpace <= 0) break;

                int moved = Math.min(availableSpace, donorStack.getCount());
                if (targetStack.isEmpty()) {
                    ItemStack placed = donorStack.copy();
                    placed.setCount(moved);
                    simulated.set(targetIndex, placed);
                } else {
                    targetStack.grow(moved);
                }
                donorStack.shrink(moved);
                if (donorStack.isEmpty()) simulated.set(donorIndex, ItemStack.EMPTY);
                transfers.add(new Transfer(donorIndex, targetIndex, moved, !donorStack.isEmpty()));

                if (simulated.get(targetIndex).getCount() >= maxStackSize) break;
            }
        }
        return new Result(List.copyOf(simulated), List.copyOf(transfers));
    }

    public interface SlotAccess {
        ItemStack stack();

        boolean reserved();

        /** Uses CSR's canonical admission result for this physical destination. */
        boolean mayInsert(ItemStack incoming);

        /** Effective physical/menu capacity for the incoming stack. */
        int maxStackSize(ItemStack incoming);
    }

    public record Transfer(int donorIndex, int targetIndex, int count, boolean returnsRemainder) {
        public Transfer {
            if (donorIndex < 0 || targetIndex < 0 || count <= 0) {
                throw new IllegalArgumentException("Invalid reservation-fill transfer");
            }
        }
    }

    public record Result(List<ItemStack> stacks, List<Transfer> transfers) {
        public Result {
            stacks = List.copyOf(stacks);
            transfers = List.copyOf(transfers);
        }

        public boolean changed() {
            return !transfers.isEmpty();
        }
    }
}
