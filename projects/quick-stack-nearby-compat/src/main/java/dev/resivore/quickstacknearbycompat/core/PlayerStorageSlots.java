package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * One authoritative view of the ordinary, non-hotbar player storage slots
 * used by both the QSN server operation and its client-side rule editor.
 */
public final class PlayerStorageSlots {
    public static final int SLOTS_PER_ROW = 9;
    public static final int MAX_SOURCE_RULES = 64;

    private PlayerStorageSlots() {}

    public static Window liveWindow(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        return fromNonEquipmentItemCount(inventory.getNonEquipmentItems().size());
    }

    public static Window fromNonEquipmentItemCount(int nonEquipmentItemCount) {
        int first = Inventory.SELECTION_SIZE;
        return new Window(first, Math.max(first, nonEquipmentItemCount));
    }

    public static <T> List<T> filterRules(
            List<T> rules,
            ToIntFunction<? super T> slotIndex,
            Window window
    ) {
        Objects.requireNonNull(slotIndex, "slotIndex");
        Objects.requireNonNull(window, "window");
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> window.contains(slotIndex.applyAsInt(rule)))
                .limit(MAX_SOURCE_RULES)
                .toList();
    }

    public static <T> Map<Integer, T> filterRuleMap(Map<Integer, T> rules, Window window) {
        Objects.requireNonNull(window, "window");
        if (rules == null || rules.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<Integer, T> filtered = new LinkedHashMap<>();
        for (Map.Entry<Integer, T> entry : rules.entrySet()) {
            Integer slot = entry.getKey();
            T rule = entry.getValue();
            if (slot == null || rule == null || !window.contains(slot)) {
                continue;
            }
            filtered.put(slot, rule);
            if (filtered.size() == MAX_SOURCE_RULES) {
                break;
            }
        }
        return Map.copyOf(filtered);
    }

    public record Window(int firstInclusive, int endExclusive) {
        public Window {
            if (firstInclusive < 0 || endExclusive < firstInclusive) {
                throw new IllegalArgumentException(
                        "Invalid player storage window [" + firstInclusive + ", " + endExclusive + ")"
                );
            }
        }

        public int slotCount() {
            return endExclusive - firstInclusive;
        }

        public int rowCount() {
            return (slotCount() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
        }

        public boolean contains(int inventorySlot) {
            return inventorySlot >= firstInclusive && inventorySlot < endExclusive;
        }

        public int displayIndex(int inventorySlot) {
            if (!contains(inventorySlot)) {
                throw new IndexOutOfBoundsException("Not a main-storage slot: " + inventorySlot);
            }
            return inventorySlot - firstInclusive;
        }

        public int inventorySlotAt(int displayIndex) {
            if (displayIndex < 0 || displayIndex >= slotCount()) {
                throw new IndexOutOfBoundsException("Not a main-storage display index: " + displayIndex);
            }
            return firstInclusive + displayIndex;
        }

        public int rowOf(int inventorySlot) {
            return displayIndex(inventorySlot) / SLOTS_PER_ROW;
        }

        public int columnOf(int inventorySlot) {
            return displayIndex(inventorySlot) % SLOTS_PER_ROW;
        }
    }
}
