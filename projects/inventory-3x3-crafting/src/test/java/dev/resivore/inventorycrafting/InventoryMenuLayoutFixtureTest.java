package dev.resivore.inventorycrafting;

import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.APPENDED_CRAFT_START;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.HOTBAR_END_EXCLUSIVE;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.OFFHAND_MENU_ID;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ORDINARY_START;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.PRE_TRINKETS_END_EXCLUSIVE;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.STORAGE_END_EXCLUSIVE;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TRASH_MENU_ID;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CraftingTableMenu;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Owner;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Role;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Side;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.SlotDescriptor;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TargetInventoryMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InventoryMenuLayoutFixtureTest {
    @Test
    void preservesTheAcceptedZeroThroughSeventyThreePrefixExactly() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);

        assertSlot(menu.slot(0), 0, Owner.RESULT, Role.RESULT, 0);

        int[] legacyLogicalCells = {0, 1, 3, 4};
        for (int offset = 0; offset < legacyLogicalCells.length; offset++) {
            assertSlot(menu.slot(offset + 1), offset + 1, Owner.CRAFT, Role.CRAFT_INPUT,
                    legacyLogicalCells[offset]);
        }

        int[] armorBackingSlots = {66, 65, 64, 63};
        for (int offset = 0; offset < armorBackingSlots.length; offset++) {
            assertSlot(menu.slot(offset + 5), offset + 5, Owner.PLAYER, Role.ARMOR,
                    armorBackingSlots[offset]);
        }

        for (int menuId = ORDINARY_START; menuId < STORAGE_END_EXCLUSIVE; menuId++) {
            assertSlot(menu.slot(menuId), menuId, Owner.PLAYER, Role.ORDINARY_STORAGE, menuId);
        }
        for (int menuId = STORAGE_END_EXCLUSIVE; menuId < HOTBAR_END_EXCLUSIVE; menuId++) {
            assertSlot(menu.slot(menuId), menuId, Owner.PLAYER, Role.HOTBAR,
                    menuId - STORAGE_END_EXCLUSIVE);
        }

        assertSlot(menu.slot(OFFHAND_MENU_ID), OFFHAND_MENU_ID, Owner.PLAYER, Role.OFFHAND, 67);
        assertSlot(menu.slot(TRASH_MENU_ID), TRASH_MENU_ID, Owner.PLAYER, Role.TRASH, 68);

        assertEquals(74, menu.slots().subList(0, TRASH_MENU_ID + 1).size(),
                "adding five craft views must not insert into or renumber the accepted prefix");
    }

    @Test
    void exposesOneThreeByThreeContainerInLogicalRowMajorOrder() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        List<SlotDescriptor> inputGrid = menu.inputGridSlots();

        assertAll(
                () -> assertEquals(3, menu.gridWidth()),
                () -> assertEquals(3, menu.gridHeight()),
                () -> assertEquals(9, inputGrid.size()),
                () -> assertEquals(
                        List.of(1, 2, 74, 3, 4, 75, 76, 77, 78),
                        inputGrid.stream().map(SlotDescriptor::index).toList()),
                () -> assertEquals(
                        IntStream.range(0, 9).boxed().toList(),
                        inputGrid.stream().map(SlotDescriptor::containerSlot).toList())
        );

        Object oneContainer = inputGrid.getFirst().container();
        assertTrue(inputGrid.stream().allMatch(slot -> slot.container() == oneContainer),
                "the appended cells may not be backed by a second transient container");
        assertEquals(9, menu.craftingSlotsByMenuOrder().stream()
                .map(SlotDescriptor::containerSlot)
                .distinct()
                .count());
    }

    @Test
    void constructsTrashThenFiveStaticCraftSlotsThenTheDynamicTrinketsSuffix() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);

        assertEquals(Role.TRASH, menu.slot(TRASH_MENU_ID).role());
        assertEquals(
                List.of(2, 5, 6, 7, 8),
                menu.slots().subList(APPENDED_CRAFT_START, PRE_TRINKETS_END_EXCLUSIVE).stream()
                        .map(SlotDescriptor::containerSlot)
                        .toList());
        assertTrue(menu.slots().subList(APPENDED_CRAFT_START, PRE_TRINKETS_END_EXCLUSIVE).stream()
                .allMatch(slot -> slot.owner() == Owner.CRAFT));
        assertEquals(PRE_TRINKETS_END_EXCLUSIVE, menu.trinketSlotStart());
        assertTrue(menu.slots().subList(menu.trinketSlotStart(), menu.slots().size()).stream()
                .allMatch(slot -> slot.owner() == Owner.TRINKET));
    }

    @Test
    void rebuildingTheTrinketsSuffixPreservesStaticIdentityIndexesAndAllSnapshotAlignment() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        List<SlotDescriptor> staticPrefix = new ArrayList<>(
                menu.slots().subList(0, PRE_TRINKETS_END_EXCLUSIVE));
        List<SlotDescriptor> removedSuffix = new ArrayList<>(
                menu.slots().subList(PRE_TRINKETS_END_EXCLUSIVE, menu.slots().size()));

        menu.markSnapshots(TRASH_MENU_ID, "last-trash", "remote-trash");
        menu.markSnapshots(APPENDED_CRAFT_START, "last-craft-2", "remote-craft-2");
        menu.markSnapshots(PRE_TRINKETS_END_EXCLUSIVE, "last-old-trinket", "remote-old-trinket");
        menu.rebuildTrinketSuffix(5);

        assertAll(
                () -> assertEquals(84, menu.slots().size()),
                () -> assertEquals(menu.slots().size(), menu.lastSlots().size()),
                () -> assertEquals(menu.slots().size(), menu.remoteSlots().size()),
                () -> assertEquals("last-trash", menu.lastSlots().get(TRASH_MENU_ID)),
                () -> assertEquals("remote-trash", menu.remoteSlots().get(TRASH_MENU_ID)),
                () -> assertEquals("last-craft-2", menu.lastSlots().get(APPENDED_CRAFT_START)),
                () -> assertEquals("remote-craft-2", menu.remoteSlots().get(APPENDED_CRAFT_START))
        );

        for (int menuId = 0; menuId < PRE_TRINKETS_END_EXCLUSIVE; menuId++) {
            assertSame(staticPrefix.get(menuId), menu.slot(menuId),
                    "a suffix rebuild moved or reconstructed static menu slot " + menuId);
            assertEquals(menuId, menu.slot(menuId).index());
        }
        for (SlotDescriptor oldTrinket : removedSuffix) {
            assertFalse(menu.slots().stream().anyMatch(current -> current == oldTrinket),
                    "the old dynamic suffix should be replaced, not retained");
        }
        for (int menuId = PRE_TRINKETS_END_EXCLUSIVE; menuId < menu.slots().size(); menuId++) {
            int rebuiltMenuId = menuId;
            assertAll(
                    () -> assertEquals(rebuiltMenuId, menu.slot(rebuiltMenuId).index()),
                    () -> assertEquals(Owner.TRINKET, menu.slot(rebuiltMenuId).owner()),
                    () -> assertEquals("EMPTY", menu.lastSlots().get(rebuiltMenuId)),
                    () -> assertEquals("EMPTY", menu.remoteSlots().get(rebuiltMenuId))
            );
        }
    }

    @Test
    void clientAndServerConstructionIsDeterministicAcrossInitialBuildAndSuffixRebuild() {
        TargetInventoryMenu client = TargetInventoryMenu.create(Side.CLIENT, 3);
        TargetInventoryMenu server = TargetInventoryMenu.create(Side.SERVER, 3);

        assertNotSame(client.craftingContainer(), server.craftingContainer(),
                "each side owns a local container instance; only its structure must match");
        assertEquals(server.deterministicFingerprint(), client.deterministicFingerprint());

        client.rebuildTrinketSuffix(5);
        server.rebuildTrinketSuffix(5);
        assertEquals(server.deterministicFingerprint(), client.deterministicFingerprint());
        assertEquals(
                server.inputGridSlots().stream().map(SlotDescriptor::index).toList(),
                client.inputGridSlots().stream().map(SlotDescriptor::index).toList());
    }

    @Test
    void inventorySpecificAppendArchitectureLeavesCraftingMenuUnchanged() {
        CraftingTableMenu craftingTable = new CraftingTableMenu();
        String before = craftingTable.deterministicFingerprint();

        TargetInventoryMenu inventory = TargetInventoryMenu.create(Side.SERVER, 3);
        inventory.rebuildTrinketSuffix(5);

        assertAll(
                () -> assertEquals(before, craftingTable.deterministicFingerprint()),
                () -> assertEquals(3, craftingTable.gridWidth()),
                () -> assertEquals(3, craftingTable.gridHeight()),
                () -> assertEquals(IntStream.rangeClosed(1, 9).boxed().toList(),
                        craftingTable.inputGridSlots().stream().map(SlotDescriptor::index).toList()),
                () -> assertEquals(IntStream.range(0, 9).boxed().toList(),
                        craftingTable.inputGridSlots().stream().map(SlotDescriptor::containerSlot).toList())
        );
        assertTrue(craftingTable.inputGridSlots().stream()
                .noneMatch(slot -> slot.index() >= APPENDED_CRAFT_START));
    }

    private static void assertSlot(
            SlotDescriptor actual,
            int index,
            Owner owner,
            Role role,
            int containerSlot
    ) {
        assertAll(
                () -> assertEquals(index, actual.index()),
                () -> assertEquals(owner, actual.owner()),
                () -> assertEquals(role, actual.role()),
                () -> assertEquals(containerSlot, actual.containerSlot())
        );
    }
}
