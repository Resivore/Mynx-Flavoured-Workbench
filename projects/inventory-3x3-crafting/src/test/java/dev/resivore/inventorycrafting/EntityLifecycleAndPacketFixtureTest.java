package dev.resivore.inventorycrafting;

import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CRAFTING_ENTITY_SLOT_END_EXCLUSIVE;
import static dev.resivore.inventorycrafting.InventoryCraftingContractFixture.CRAFTING_ENTITY_SLOT_START;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.AuthoritativeMenuState;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ClearReport;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ClickPacket;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ClickResult;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.ClickStatus;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.EntityCraftingSlot;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.EntitySlotResolver;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.EvacuatedStack;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.Side;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TargetInventoryMenu;
import dev.resivore.inventorycrafting.InventoryCraftingContractFixture.TransientGrid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class EntityLifecycleAndPacketFixtureTest {
    @Test
    void entitySlotIdsFiveHundredThroughFiveHundredEightResolveLogicalCellsNotMenuOrdinals() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        List<EntityCraftingSlot> resolved = IntStream
                .range(CRAFTING_ENTITY_SLOT_START, CRAFTING_ENTITY_SLOT_END_EXCLUSIVE)
                .mapToObj(entitySlotId -> EntitySlotResolver.resolve(menu, entitySlotId).orElseThrow())
                .toList();

        assertAll(
                () -> assertEquals(9, EntitySlotResolver.registeredCraftingCount()),
                () -> assertEquals(IntStream.range(0, 9).boxed().toList(),
                        resolved.stream().map(EntityCraftingSlot::logicalCell).toList()),
                () -> assertEquals(List.of(1, 2, 74, 3, 4, 75, 76, 77, 78),
                        resolved.stream().map(EntityCraftingSlot::menuId).toList()),
                () -> assertTrue(EntitySlotResolver.resolve(menu, CRAFTING_ENTITY_SLOT_START - 1).isEmpty()),
                () -> assertTrue(EntitySlotResolver.resolve(menu, CRAFTING_ENTITY_SLOT_END_EXCLUSIVE).isEmpty())
        );
        assertEquals(74, EntitySlotResolver.resolve(menu, 502).orElseThrow().menuId(),
                "entity crafting slot 502 is logical cell 2, not public menu slot 3");
    }

    @Test
    void removalVisitsAndEvacuatesAllNineLogicalCellsExactlyOnce() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        TransientGrid grid = menu.craftingGrid();
        for (int logicalCell = 0; logicalCell < 9; logicalCell++) {
            grid.put(logicalCell, "marked-stack-" + logicalCell);
        }

        ClearReport first = grid.clearForRemoval();
        ClearReport duplicateRemovalCallback = grid.clearForRemoval();

        assertAll(
                () -> assertEquals(IntStream.range(0, 9).boxed().toList(),
                        first.evacuated().stream().map(EvacuatedStack::logicalCell).toList()),
                () -> assertEquals(IntStream.range(0, 9).mapToObj(i -> "marked-stack-" + i).toList(),
                        first.evacuated().stream().map(EvacuatedStack::stackIdentity).toList()),
                () -> assertEquals(9,
                        first.evacuated().stream().map(EvacuatedStack::stackIdentity).distinct().count()),
                () -> assertArrayEquals(new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1}, first.visits()),
                () -> assertTrue(grid.isEmpty()),
                () -> assertTrue(duplicateRemovalCallback.evacuated().isEmpty()),
                () -> assertArrayEquals(first.visits(), duplicateRemovalCallback.visits())
        );
        for (int logicalCell = 0; logicalCell < 9; logicalCell++) {
            assertNull(grid.get(logicalCell));
        }
    }

    @Test
    void staleStateIdStillUsesTheAuthoritativeOrdinalThenRequestsFullCorrection() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        AuthoritativeMenuState state = new AuthoritativeMenuState(menu, 7, 40);
        state.put(73, "trash-marker");
        state.put(74, "logical-cell-2");
        state.put(75, "logical-cell-5");
        state.put(76, "logical-cell-6");
        Map<Integer, String> before = state.snapshot();

        ClickResult result = state.click(new ClickPacket(7, 39, 74));

        Map<Integer, String> expectedAfter = new LinkedHashMap<>(before);
        expectedAfter.remove(74);
        assertAll(
                () -> assertEquals(ClickStatus.APPLIED_WITH_FULL_RESYNC, result.status()),
                () -> assertEquals("logical-cell-2", result.removedStack()),
                () -> assertEquals(41, result.newStateId()),
                () -> assertEquals(expectedAfter, state.snapshot()),
                () -> assertEquals("trash-marker", state.stackAt(73)),
                () -> assertNull(state.stackAt(74)),
                () -> assertEquals("logical-cell-5", state.stackAt(75)),
                () -> assertEquals("logical-cell-6", state.stackAt(76))
        );
    }

    @Test
    void outOfRangeAndWrongContainerPacketsRejectWithoutMutatingAnySlot() {
        TargetInventoryMenu menu = TargetInventoryMenu.create(Side.SERVER, 3);
        AuthoritativeMenuState state = new AuthoritativeMenuState(menu, 7, 40);
        List<Integer> markedMenuIds = new ArrayList<>(
                menu.inputGridSlots().stream().map(slot -> slot.index()).toList());
        markedMenuIds.add(73);
        for (int menuId : markedMenuIds) {
            state.put(menuId, "marker-" + menuId);
        }
        Map<Integer, String> before = state.snapshot();

        List<ClickResult> results = List.of(
                state.click(new ClickPacket(7, 40, menu.slots().size())),
                state.click(new ClickPacket(7, 40, -1)),
                state.click(new ClickPacket(8, 40, 74))
        );

        assertTrue(results.stream().allMatch(result -> result.status() == ClickStatus.REJECTED));
        assertTrue(results.stream().allMatch(result -> result.removedStack() == null));
        assertTrue(results.stream().allMatch(result -> result.newStateId() == 40));
        assertEquals(before, state.snapshot());
        assertEquals(40, state.stateId());
    }
}
