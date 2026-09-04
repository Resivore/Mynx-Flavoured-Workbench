package dev.resivore.carriedrouting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

final class ShulkerDestination implements RoutingDestination {
    private static final int SIZE = 27;
    private static final class Provider {
        static final ReservationAdmission INSTANCE = ReservationAdmission.load();
    }
    private final ItemStack carrier;
    private final ReservationAdmission admission;
    ShulkerDestination(ItemStack carrier) { this(carrier, Provider.INSTANCE); }
    ShulkerDestination(ItemStack carrier, ReservationAdmission admission) {
        this.carrier = carrier;
        this.admission = admission;
    }

    private NonNullList<ItemStack> contents() {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    private record Move(int slot, int count) {}
    private record Plan(List<Move> moves, int count) {
        static final Plan EMPTY = new Plan(List.of(), 0);
    }

    private Plan plan(ItemStack incoming, NonNullList<ItemStack> items) {
        if (incoming.isEmpty() || RoutingLock.isLocked(carrier)
                || !incoming.getItem().canFitInsideContainerItems()) return Plan.EMPTY;
        try {
            var classes = new ReservationAdmission.Slot[SIZE];
            boolean affinity = false;
            for (int i = 0; i < SIZE; i++) {
                ItemStack physical = items.get(i);
                classes[i] = admission.classify(carrier, i, incoming, physical);
                if (physical.isEmpty()) {
                    affinity |= classes[i] == ReservationAdmission.Slot.RESERVED_MATCH;
                } else if (ItemStack.isSameItemSameComponents(physical, incoming)
                        && classes[i] != ReservationAdmission.Slot.INELIGIBLE
                        && admission.permitsAffinity(carrier, i, incoming)) {
                    affinity = true;
                }
            }
            if (!affinity) return Plan.EMPTY;
            int remaining = incoming.getCount();
            List<Move> moves = new ArrayList<>();
            for (var tier : List.of(ReservationAdmission.Slot.OCCUPIED_COMPATIBLE,
                    ReservationAdmission.Slot.RESERVED_MATCH, ReservationAdmission.Slot.UNRESERVED_EMPTY)) {
                for (int i = 0; i < SIZE && remaining > 0; i++) {
                    if (classes[i] != tier) continue;
                    ItemStack physical = items.get(i);
                    int capacity = physical.isEmpty() ? incoming.getMaxStackSize()
                            : Math.min(physical.getMaxStackSize(), incoming.getMaxStackSize()) - physical.getCount();
                    int count = Math.min(remaining, Math.max(0, capacity));
                    if (count > 0) { moves.add(new Move(i, count)); remaining -= count; }
                }
            }
            return new Plan(List.copyOf(moves), incoming.getCount() - remaining);
        } catch (LinkageError | IllegalArgumentException error) {
            LoggerFactory.getLogger("carried_container_auto_routing").error(
                    "CSR public API is incompatible; carried-shulker routing denied before mutation", error);
            return Plan.EMPTY;
        }
    }

    @Override public boolean qualifies(ItemStack incoming) { return plan(incoming, contents()).count() > 0; }

    @Override public int insert(ItemStack incoming) {
        NonNullList<ItemStack> items = contents();
        Plan plan = plan(incoming, items);
        if (plan.count() == 0) return 0;
        for (Move move : plan.moves()) {
            ItemStack physical = items.get(move.slot());
            if (physical.isEmpty()) items.set(move.slot(), incoming.copyWithCount(move.count()));
            else physical.grow(move.count());
        }
        carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        incoming.shrink(plan.count());
        return plan.count();
    }
}
