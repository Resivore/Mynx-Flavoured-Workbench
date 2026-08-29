package dev.resivore.quickstacknearbycompat.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStorageSlotsTest {
    @Test
    void vanillaInventoryRetainsExactlyThreeStorageRows() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(36);

        assertEquals(9, window.firstInclusive());
        assertEquals(36, window.endExclusive());
        assertEquals(27, window.slotCount());
        assertEquals(3, window.rowCount());
        assertTrue(window.contains(9));
        assertTrue(window.contains(35));
        assertFalse(window.contains(36));
    }

    @Test
    void inventoryExtendedExposesAllSixStorageRows() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);

        assertEquals(9, window.firstInclusive());
        assertEquals(63, window.endExclusive());
        assertEquals(54, window.slotCount());
        assertEquals(6, window.rowCount());
    }

    @Test
    void firstAndLastInventoryExtendedSlotsKeepTheirRealIdentities() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);

        assertTrue(window.contains(36));
        assertTrue(window.contains(62));
        assertEquals(27, window.displayIndex(36));
        assertEquals(53, window.displayIndex(62));
        assertEquals(36, window.inventorySlotAt(27));
        assertEquals(62, window.inventorySlotAt(53));
        assertEquals(3, window.rowOf(36));
        assertEquals(0, window.columnOf(36));
        assertEquals(5, window.rowOf(62));
        assertEquals(8, window.columnOf(62));
    }

    @Test
    void hotbarEquipmentAndOffhandStayOutsideTheStorageWindow() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);

        for (int slot = 0; slot < 9; slot++) {
            assertFalse(window.contains(slot), "hotbar slot " + slot);
        }
        for (int slot = 63; slot <= 69; slot++) {
            assertFalse(window.contains(slot), "equipment/offhand slot " + slot);
        }
        assertFalse(window.contains(-1));
    }

    @Test
    void ruleFilteringCannotEscapeTheLiveWindowOrProtocolLimit() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);
        List<Rule> rules = new ArrayList<>();
        for (int slot = -4; slot < 80; slot++) {
            rules.add(new Rule(slot));
        }

        List<Rule> filtered = PlayerStorageSlots.filterRules(rules, Rule::slotIndex, window);

        assertEquals(54, filtered.size());
        assertEquals(9, filtered.getFirst().slotIndex());
        assertEquals(62, filtered.getLast().slotIndex());
        assertTrue(filtered.size() <= PlayerStorageSlots.MAX_SOURCE_RULES);
        assertTrue(filtered.stream().allMatch(rule -> window.contains(rule.slotIndex())));
    }

    @Test
    void invalidDisplayIndicesCannotAliasAnotherInventoryRegion() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);

        assertThrows(IndexOutOfBoundsException.class, () -> window.displayIndex(8));
        assertThrows(IndexOutOfBoundsException.class, () -> window.displayIndex(63));
        assertThrows(IndexOutOfBoundsException.class, () -> window.inventorySlotAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> window.inventorySlotAt(54));
    }

    @Test
    void serverRuleMapDropsClientAuthoredIndicesOutsideLiveStorage() {
        PlayerStorageSlots.Window window = PlayerStorageSlots.fromNonEquipmentItemCount(63);
        Map<Integer, String> incoming = new LinkedHashMap<>();
        incoming.put(8, "hotbar");
        incoming.put(9, "first");
        incoming.put(36, "first-ie");
        incoming.put(62, "last-ie");
        incoming.put(63, "equipment");
        incoming.put(67, "offhand");

        Map<Integer, String> filtered = PlayerStorageSlots.filterRuleMap(incoming, window);

        assertEquals(Map.of(9, "first", 36, "first-ie", 62, "last-ie"), filtered);
    }

    private record Rule(int slotIndex) {}
}
