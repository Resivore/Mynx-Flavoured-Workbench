package dev.resivore.carriedrouting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Server-authoritative Survival Pick Block fallback for unlocked carried shulkers. */
public final class PickBlockRouting {
    private static final int SHULKER_SIZE = 27;

    private static final class Provider {
        static final ReservationAdmission INSTANCE = ReservationAdmission.load();
    }

    private PickBlockRouting() {}

    /**
     * Called only after vanilla reports no ordinary-inventory match. The repeated
     * ordinary check makes that precedence an invariant even for future callers.
     */
    public static boolean tryPickFromShulkers(Player player, ItemStack requested) {
        if (player.level().isClientSide() || player.isSpectator() || player.hasInfiniteMaterials() || requested.isEmpty()
                || player.getInventory().findSlotMatchingItem(requested) != Inventory.NOT_FOUND_INDEX) {
            return false;
        }
        return plan(player, requested, Provider.INSTANCE).map(Transaction::commit).orElse(false);
    }

    static Optional<Transaction> plan(Player player, ItemStack requested, ReservationAdmission admission) {
        Inventory inventory = player.getInventory();
        if (player.level().isClientSide() || player.isSpectator() || player.hasInfiniteMaterials() || requested.isEmpty()
                || inventory.findSlotMatchingItem(requested) != Inventory.NOT_FOUND_INDEX) {
            return Optional.empty();
        }

        int selectedBefore = inventory.getSelectedSlot();
        List<ItemStack> beforeInventory = copy(inventory.getNonEquipmentItems());
        ItemStack beforeOffhand = player.getOffhandItem().copy();
        SimulatedInventory simulated = new SimulatedInventory(copy(beforeInventory), beforeOffhand.copy());

        try {
            Source source = findSource(simulated, requested);
            if (source == null) return Optional.empty();

            ItemStack sourceCarrier = simulated.get(source.host());
            NonNullList<ItemStack> sourceContents = contents(sourceCarrier);
            ItemStack pickedStack = sourceContents.get(source.physicalSlot()).copy();
            sourceContents.set(source.physicalSlot(), ItemStack.EMPTY);
            writeContents(sourceCarrier, sourceContents);

            // Vanilla Inventory.pickSlot chooses this destination before swapping the full source stack.
            int hotbarDestination = inventory.getSuitableHotbarSlot();
            ItemStack displaced = simulated.inventory().get(hotbarDestination);
            simulated.inventory().set(hotbarDestination, ItemStack.EMPTY);

            List<Merge> merges = new ArrayList<>();
            ItemStack displacedRemainder = displaced.copy();
            mergeIntoCarriedShulkers(simulated, displacedRemainder, admission, merges);
            mergeOrdinaryRange(simulated.inventory(), displacedRemainder, 0,
                    Math.min(Inventory.SELECTION_SIZE, simulated.inventory().size()), hotbarDestination, merges);
            mergeOffhand(simulated, displacedRemainder, merges);
            mergeOrdinaryRange(simulated.inventory(), displacedRemainder, Inventory.SELECTION_SIZE,
                    simulated.inventory().size(), hotbarDestination, merges);

            OptionalInt emptyOrdinarySlot = OptionalInt.empty();
            if (!displacedRemainder.isEmpty()) {
                int empty = firstEmptyOrdinarySlot(simulated.inventory(), hotbarDestination);
                if (empty >= 0) {
                    simulated.inventory().set(empty, displacedRemainder.copy());
                    displacedRemainder = ItemStack.EMPTY;
                    emptyOrdinarySlot = OptionalInt.of(empty);
                }
            }

            int sourceFallbackCount = 0;
            if (!displacedRemainder.isEmpty()) {
                ItemStack currentSourceCarrier = simulated.get(source.host());
                if (!isEligibleShulker(currentSourceCarrier)) return Optional.empty();
                NonNullList<ItemStack> currentSourceContents = contents(currentSourceCarrier);
                ItemStack physical = currentSourceContents.get(source.physicalSlot());
                if (!physical.isEmpty()
                        || !permitsEmptyInsertion(currentSourceCarrier, source.physicalSlot(),
                        displacedRemainder, admission)
                        || displacedRemainder.getCount() > displacedRemainder.getMaxStackSize()) {
                    return Optional.empty();
                }
                sourceFallbackCount = displacedRemainder.getCount();
                currentSourceContents.set(source.physicalSlot(), displacedRemainder.copy());
                writeContents(currentSourceCarrier, currentSourceContents);
                displacedRemainder = ItemStack.EMPTY;
            }

            simulated.inventory().set(hotbarDestination, pickedStack.copy());
            return Optional.of(new Transaction(
                    player,
                    selectedBefore,
                    hotbarDestination,
                    beforeInventory,
                    beforeOffhand,
                    copy(simulated.inventory()),
                    simulated.offhand().copy(),
                    source,
                    pickedStack.copy(),
                    displaced.copy(),
                    List.copyOf(merges),
                    emptyOrdinarySlot,
                    sourceFallbackCount
            ));
        } catch (LinkageError | RuntimeException error) {
            LoggerFactory.getLogger("carried_container_auto_routing").error(
                    "Pick Block planning failed before mutation; carried inventories were left unchanged", error);
            return Optional.empty();
        }
    }

    private static Source findSource(SimulatedInventory simulated, ItemStack requested) {
        for (CarriedContainerOrder.Host host : CarriedContainerOrder.hosts(
                simulated.inventory().size(), -1, false)) {
            ItemStack carrier = simulated.get(host);
            if (!isEligibleShulker(carrier)) continue;
            NonNullList<ItemStack> contents = contents(carrier);
            for (int slot = 0; slot < SHULKER_SIZE; slot++) {
                ItemStack physical = contents.get(slot);
                if (!physical.isEmpty() && ItemStack.isSameItemSameComponents(requested, physical)) {
                    return new Source(host, slot);
                }
            }
        }
        return null;
    }

    private static void mergeIntoCarriedShulkers(
            SimulatedInventory simulated,
            ItemStack remainder,
            ReservationAdmission admission,
            List<Merge> merges
    ) {
        if (remainder.isEmpty() || !remainder.getItem().canFitInsideContainerItems()) return;
        for (CarriedContainerOrder.Host host : CarriedContainerOrder.hosts(
                simulated.inventory().size(), -1, false)) {
            if (remainder.isEmpty()) return;
            ItemStack carrier = simulated.get(host);
            if (!isEligibleShulker(carrier)) continue;
            NonNullList<ItemStack> items = contents(carrier);
            boolean changed = false;
            for (int slot = 0; slot < SHULKER_SIZE && !remainder.isEmpty(); slot++) {
                ItemStack physical = items.get(slot);
                if (physical.isEmpty() || !ItemStack.isSameItemSameComponents(physical, remainder)) continue;
                if (admission.classify(carrier, slot, remainder, physical)
                        != ReservationAdmission.Slot.OCCUPIED_COMPATIBLE
                        || !admission.permitsAffinity(carrier, slot, remainder)) {
                    continue;
                }
                int capacity = Math.min(physical.getMaxStackSize(), remainder.getMaxStackSize())
                        - physical.getCount();
                int moved = Math.min(remainder.getCount(), Math.max(0, capacity));
                if (moved <= 0) continue;
                physical.grow(moved);
                remainder.shrink(moved);
                merges.add(Merge.shulker(host, slot, moved));
                changed = true;
            }
            if (changed) writeContents(carrier, items);
        }
    }

    private static void mergeOrdinaryRange(
            List<ItemStack> inventory,
            ItemStack remainder,
            int start,
            int end,
            int excluded,
            List<Merge> merges
    ) {
        for (int slot = Math.max(0, start); slot < Math.min(end, inventory.size()) && !remainder.isEmpty(); slot++) {
            if (slot == excluded) continue;
            ItemStack physical = inventory.get(slot);
            int moved = merge(physical, remainder);
            if (moved > 0) merges.add(Merge.inventory(slot, moved));
        }
    }

    private static void mergeOffhand(SimulatedInventory simulated, ItemStack remainder, List<Merge> merges) {
        int moved = merge(simulated.offhand(), remainder);
        if (moved > 0) merges.add(Merge.offhand(moved));
    }

    private static int merge(ItemStack physical, ItemStack remainder) {
        if (physical.isEmpty() || remainder.isEmpty()
                || !ItemStack.isSameItemSameComponents(physical, remainder)) return 0;
        int capacity = Math.min(physical.getMaxStackSize(), remainder.getMaxStackSize()) - physical.getCount();
        int moved = Math.min(remainder.getCount(), Math.max(0, capacity));
        if (moved > 0) {
            physical.grow(moved);
            remainder.shrink(moved);
        }
        return moved;
    }

    static int firstEmptyOrdinarySlot(List<ItemStack> inventory, int excluded) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (slot != excluded && inventory.get(slot).isEmpty()) return slot;
        }
        return Inventory.NOT_FOUND_INDEX;
    }

    private static boolean permitsEmptyInsertion(
            ItemStack carrier,
            int slot,
            ItemStack incoming,
            ReservationAdmission admission
    ) {
        if (incoming.isEmpty() || !incoming.getItem().canFitInsideContainerItems()) return false;
        ReservationAdmission.Slot classification = admission.classify(carrier, slot, incoming, ItemStack.EMPTY);
        return classification == ReservationAdmission.Slot.RESERVED_MATCH
                || classification == ReservationAdmission.Slot.UNRESERVED_EMPTY;
    }

    private static boolean isEligibleShulker(ItemStack stack) {
        return !stack.isEmpty()
                && !RoutingLock.isLocked(stack)
                && RoutingService.isSupportedShulker(stack);
    }

    private static NonNullList<ItemStack> contents(ItemStack carrier) {
        NonNullList<ItemStack> result = NonNullList.withSize(SHULKER_SIZE, ItemStack.EMPTY);
        carrier.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(result);
        return result;
    }

    private static void writeContents(ItemStack carrier, NonNullList<ItemStack> contents) {
        carrier.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static boolean exact(ItemStack first, ItemStack second) {
        return ItemStack.matches(first, second);
    }

    record Source(CarriedContainerOrder.Host host, int physicalSlot) {}

    record Merge(String destination, int inventorySlot, int shulkerSlot, int count) {
        static Merge inventory(int slot, int count) { return new Merge("inventory", slot, -1, count); }
        static Merge offhand(int count) { return new Merge("offhand", -1, -1, count); }
        static Merge shulker(CarriedContainerOrder.Host host, int slot, int count) {
            return new Merge(host.offhand() ? "offhand_shulker" : "inventory_shulker",
                    host.inventorySlot(), slot, count);
        }
    }

    static final class Transaction {
        private final Player player;
        private final int selectedBefore;
        private final int selectedAfter;
        private final List<ItemStack> beforeInventory;
        private final ItemStack beforeOffhand;
        private final List<ItemStack> afterInventory;
        private final ItemStack afterOffhand;
        private final Source source;
        private final ItemStack pickedStack;
        private final ItemStack displacedStack;
        private final List<Merge> merges;
        private final OptionalInt emptyOrdinarySlot;
        private final int sourceFallbackCount;
        private boolean committed;

        private Transaction(
                Player player,
                int selectedBefore,
                int selectedAfter,
                List<ItemStack> beforeInventory,
                ItemStack beforeOffhand,
                List<ItemStack> afterInventory,
                ItemStack afterOffhand,
                Source source,
                ItemStack pickedStack,
                ItemStack displacedStack,
                List<Merge> merges,
                OptionalInt emptyOrdinarySlot,
                int sourceFallbackCount
        ) {
            this.player = player;
            this.selectedBefore = selectedBefore;
            this.selectedAfter = selectedAfter;
            this.beforeInventory = copy(beforeInventory);
            this.beforeOffhand = beforeOffhand.copy();
            this.afterInventory = copy(afterInventory);
            this.afterOffhand = afterOffhand.copy();
            this.source = source;
            this.pickedStack = pickedStack.copy();
            this.displacedStack = displacedStack.copy();
            this.merges = merges;
            this.emptyOrdinarySlot = emptyOrdinarySlot;
            this.sourceFallbackCount = sourceFallbackCount;
        }

        boolean commit() {
            if (committed) return false;
            Inventory inventory = player.getInventory();
            if (inventory.getSelectedSlot() != selectedBefore
                    || inventory.getNonEquipmentItems().size() != beforeInventory.size()
                    || !exact(player.getOffhandItem(), beforeOffhand)) {
                return false;
            }
            for (int slot = 0; slot < beforeInventory.size(); slot++) {
                if (!exact(inventory.getItem(slot), beforeInventory.get(slot))) return false;
            }

            // Validation is complete before the first live write.
            for (int slot = 0; slot < afterInventory.size(); slot++) {
                if (!exact(beforeInventory.get(slot), afterInventory.get(slot))) {
                    inventory.setItem(slot, afterInventory.get(slot).copy());
                }
            }
            if (!exact(beforeOffhand, afterOffhand)) {
                player.setItemSlot(EquipmentSlot.OFFHAND, afterOffhand.copy());
            }
            inventory.setSelectedSlot(selectedAfter);
            inventory.setChanged();
            committed = true;
            return true;
        }

        Source source() { return source; }
        ItemStack pickedStack() { return pickedStack.copy(); }
        ItemStack displacedStack() { return displacedStack.copy(); }
        List<Merge> merges() { return merges; }
        OptionalInt emptyOrdinarySlot() { return emptyOrdinarySlot; }
        int sourceFallbackCount() { return sourceFallbackCount; }
        int selectedAfter() { return selectedAfter; }
    }

    private static final class SimulatedInventory {
        private final List<ItemStack> inventory;
        private ItemStack offhand;

        private SimulatedInventory(List<ItemStack> inventory, ItemStack offhand) {
            this.inventory = inventory;
            this.offhand = offhand;
        }

        List<ItemStack> inventory() { return inventory; }
        ItemStack offhand() { return offhand; }
        ItemStack get(CarriedContainerOrder.Host host) {
            return host.offhand() ? offhand : inventory.get(host.inventorySlot());
        }
    }
}
