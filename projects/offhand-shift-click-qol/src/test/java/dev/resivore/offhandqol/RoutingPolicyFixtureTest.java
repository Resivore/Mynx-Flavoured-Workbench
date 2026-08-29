package dev.resivore.offhandqol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutingPolicyFixtureTest {
    record Stack(String item, String components, int count, int max) {
        Stack withCount(int value) { return new Stack(item, components, value, max); }
        boolean compatible(Stack other) { return item.equals(other.item) && components.equals(other.components); }
    }

    static final class Fixture {
        final List<Stack> storage;
        final List<Stack> hotbar;
        Stack offhand;

        Fixture(int storageSlots, int hotbarSlots, Stack offhand) {
            storage = emptySlots(storageSlots);
            hotbar = emptySlots(hotbarSlots);
            this.offhand = offhand;
        }

        private static List<Stack> emptySlots(int size) {
            List<Stack> result = new ArrayList<>();
            for (int i = 0; i < size; i++) result.add(null);
            return result;
        }

        int route(Stack incoming) {
            int remaining = incoming.count;
            if (offhand != null && offhand.compatible(incoming)) {
                int moved = Math.min(remaining, offhand.max - offhand.count);
                offhand = offhand.withCount(offhand.count + moved);
                remaining -= moved;
            }
            remaining = fill(storage, incoming, remaining);
            remaining = fill(hotbar, incoming, remaining);
            if (remaining > 0 && offhand == null) {
                int moved = Math.min(remaining, incoming.max);
                offhand = incoming.withCount(moved);
                remaining -= moved;
            }
            return remaining;
        }

        private static int fill(List<Stack> slots, Stack incoming, int remaining) {
            for (int i = 0; i < slots.size() && remaining > 0; i++) {
                Stack slot = slots.get(i);
                if (slot == null || !slot.compatible(incoming)) continue;
                int moved = Math.min(remaining, slot.max - slot.count);
                slots.set(i, slot.withCount(slot.count + moved));
                remaining -= moved;
            }
            for (int i = 0; i < slots.size() && remaining > 0; i++) {
                if (slots.get(i) != null) continue;
                int moved = Math.min(remaining, incoming.max);
                slots.set(i, incoming.withCount(moved));
                remaining -= moved;
            }
            return remaining;
        }
    }

    private static Stack cobble(int count) { return new Stack("cobble", "", count, 64); }
    private static Stack namedCobble(int count) { return new Stack("cobble", "name=Distinct", count, 64); }
    private static Stack torch(int count) { return new Stack("torch", "", count, 64); }
    private static Stack full(String id) { return new Stack(id, "", 64, 64); }

    @Test void matchingOffhandMergesFirst() {
        Fixture f = new Fixture(27, 9, cobble(20));
        assertEquals(0, f.route(cobble(32)));
        assertEquals(52, f.offhand.count);
        assertEquals(null, f.storage.get(0));
    }

    @Test void oneRemainingCapacityAndFullMatchingOffhandPreserveExactCounts() {
        Fixture one = new Fixture(27, 9, cobble(63));
        assertEquals(0, one.route(cobble(4)));
        assertEquals(64, one.offhand.count);
        assertEquals(3, one.storage.get(0).count);

        Fixture full = new Fixture(27, 9, cobble(64));
        assertEquals(0, full.route(cobble(4)));
        assertEquals(64, full.offhand.count);
        assertEquals(4, full.storage.get(0).count);
    }

    @Test void componentMismatchDoesNotMerge() {
        Fixture f = new Fixture(27, 9, namedCobble(20));
        assertEquals(0, f.route(cobble(32)));
        assertEquals(20, f.offhand.count);
        assertEquals(32, f.storage.get(0).count);
    }

    @Test void nonmatchingOccupiedOffhandUsesStorageThenHotbar() {
        Fixture f = new Fixture(2, 1, torch(8));
        f.storage.set(0, full("a"));
        assertEquals(0, f.route(cobble(70)));
        assertEquals(64, f.storage.get(1).count);
        assertEquals(6, f.hotbar.get(0).count);
        assertEquals(8, f.offhand.count);
    }

    @Test void emptyOffhandIsOnlyLastResortAndPreservesRemainder() {
        Fixture storageRoom = new Fixture(1, 1, null);
        assertEquals(0, storageRoom.route(cobble(8)));
        assertEquals(null, storageRoom.offhand);

        Fixture hotbarRoom = new Fixture(1, 1, null);
        hotbarRoom.storage.set(0, full("a"));
        assertEquals(0, hotbarRoom.route(cobble(8)));
        assertEquals(8, hotbarRoom.hotbar.get(0).count);
        assertEquals(null, hotbarRoom.offhand);

        Fixture fallback = new Fixture(1, 1, null);
        fallback.storage.set(0, full("a"));
        fallback.hotbar.set(0, full("b"));
        assertEquals(6, fallback.route(cobble(70)));
        assertEquals(64, fallback.offhand.count);
    }

    @Test void expandedStorageIsFullyUsedBeforePhysicalHotbar() {
        Fixture f = new Fixture(54, 9, null);
        for (int i = 0; i < 53; i++) f.storage.set(i, full("f" + i));
        assertEquals(0, f.route(cobble(70)));
        assertEquals(64, f.storage.get(53).count);
        assertEquals(6, f.hotbar.get(0).count);
        assertEquals(null, f.offhand);
    }
}
