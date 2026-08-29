package dev.resivore.radialslotcycler.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnLayoutTest {
    @Test
    void everyHotbarColumnMapsToExactlyThreeVanillaStorageRows() {
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            assertEquals(
                    List.of(9 + hotbar, 18 + hotbar, 27 + hotbar),
                    ColumnLayout.storageCandidates(hotbar, 36));
            assertEquals(4, ColumnLayout.radialSlots(hotbar, 36).size());
        }
    }

    @Test
    void everyHotbarColumnMapsToExactlySixInventoryExtendedRows() {
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            assertEquals(
                    List.of(
                            9 + hotbar,
                            18 + hotbar,
                            27 + hotbar,
                            36 + hotbar,
                            45 + hotbar,
                            54 + hotbar),
                    ColumnLayout.storageCandidates(hotbar, 63));
            assertEquals(7, ColumnLayout.radialSlots(hotbar, 63).size());
        }
    }

    @Test
    void representativeFirstMiddleAndLastColumnsKeepBackingIdentity() {
        assertEquals(List.of(9, 18, 27, 36, 45, 54),
                ColumnLayout.storageCandidates(0, 63));
        assertEquals(List.of(13, 22, 31, 40, 49, 58),
                ColumnLayout.storageCandidates(4, 63));
        assertEquals(List.of(17, 26, 35, 44, 53, 62),
                ColumnLayout.storageCandidates(8, 63));
    }

    @Test
    void hotbarEquipmentOffhandAndForeignIndicesNeverBecomeCandidates() {
        for (int hotbar = 0; hotbar < 9; hotbar++) {
            for (int index = 0; index < 9; index++) {
                assertFalse(ColumnLayout.isStorageCandidate(hotbar, index, 63));
            }
            for (int index = 63; index < 80; index++) {
                assertFalse(ColumnLayout.isStorageCandidate(hotbar, index, 63));
            }
        }
        assertTrue(ColumnLayout.isStorageCandidate(8, 62, 63));
    }

    @Test
    void malformedOrPartialRowsFailClosedWithoutThreeOrSixRowAssumptions() {
        for (int invalid : new int[] {-1, 0, 8, 10, 35, 37, 62, 64}) {
            assertFalse(ColumnLayout.isCompleteOrdinaryLayout(invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> ColumnLayout.storageCandidates(4, invalid));
        }
        assertTrue(ColumnLayout.isCompleteOrdinaryLayout(45));
        assertEquals(List.of(13, 22, 31, 40),
                ColumnLayout.storageCandidates(4, 45));
    }
}
